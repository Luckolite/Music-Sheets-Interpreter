# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original small tensors only: no score data, checkpoint, training run or GPU."""
from importlib.util import find_spec
import unittest

if find_spec('torch') is None:
    torch = None
else:
    import torch
    from torch.nn import functional as F
    from hard_scan_losses import (CLASS_WEIGHTS, _centre_focal, _head_negative_band,
                                  _teacher_agreement, precision_losses)


@unittest.skipIf(torch is None, 'PyTorch is an optional training dependency')
class PrecisionLossTest(unittest.TestCase):
    def setUp(self):
        torch.set_num_threads(1)

    def example(self):
        labels = torch.zeros((1, 9, 9), dtype=torch.long)
        labels[0, 4, 2] = labels[0, 4, 6] = 2
        labels[0, 1, 1] = 3
        labels[0, 2, 1] = 1
        labels[0, 7, 1:6] = 4
        labels[0, 6, 7] = 5
        heat = torch.zeros((1, 9, 9))
        heat[0, 4, 2] = heat[0, 4, 6] = 1.
        logits = torch.zeros((1, 6, 9, 9), requires_grad=True)
        centres = torch.zeros((1, 1, 9, 9), requires_grad=True)
        return logits, centres, labels, heat

    def confident_teacher(self, labels):
        logits = torch.zeros((labels.shape[0], 6, *labels.shape[1:]))
        return logits.scatter_(1, labels[:, None], 8.)

    def test_full_loss_is_scalar_with_finite_student_gradients(self):
        logits, centres, labels, heat = self.example()
        loss = precision_losses(logits, centres, labels, heat,
                                self.confident_teacher(labels))
        self.assertEqual(loss.ndim, 0)
        self.assertTrue(torch.isfinite(loss))
        loss.backward()
        self.assertTrue(torch.isfinite(logits.grad).all())
        self.assertTrue(torch.isfinite(centres.grad).all())
        self.assertGreater(logits.grad.abs().sum().item(), 0)
        self.assertGreater(centres.grad.abs().sum().item(), 0)

    def test_blank_target_with_no_centres_is_finite(self):
        logits, centres, labels, heat = self.example()
        labels.zero_()
        heat.zero_()
        loss = precision_losses(logits, centres, labels, heat)
        loss.backward()
        self.assertTrue(torch.isfinite(loss))
        self.assertTrue(torch.isfinite(logits.grad).all())
        self.assertTrue(torch.isfinite(centres.grad).all())

    def test_extreme_finite_logits_do_not_overflow(self):
        logits, centres, labels, heat = self.example()
        with torch.no_grad():
            logits[:, 2] = 1000.
            logits[:, 0] = -1000.
            centres[:] = -1000.
        loss = precision_losses(logits, centres, labels, heat,
                                self.confident_teacher(labels))
        loss.backward()
        self.assertTrue(torch.isfinite(loss))
        self.assertTrue(torch.isfinite(logits.grad).all())
        self.assertTrue(torch.isfinite(centres.grad).all())

    def test_balanced_weights_and_full_objective_coefficients(self):
        logits, centres, labels, heat = self.example()
        self.assertEqual(CLASS_WEIGHTS, (1., 1.5, 2., 2., 1., 1.5))
        ce = F.cross_entropy(logits, labels, weight=torch.tensor(CLASS_WEIGHTS))
        truth = F.one_hot(labels, 6).permute(0, 3, 1, 2).float()
        p = logits.softmax(1)
        dice = 1-(2*(p*truth).sum((0, 2, 3))+1)/((p+truth).sum((0, 2, 3))+1)
        expected = ce + .4*dice[1:].mean() + .25*_centre_focal(centres, heat)
        expected = expected + .3*_head_negative_band(logits, labels)
        torch.testing.assert_close(precision_losses(logits, centres, labels, heat), expected)

    def test_false_head_bridge_is_pushed_down(self):
        logits, _, labels, _ = self.example()
        loss = _head_negative_band(logits, labels)
        loss.backward()
        # This pixel lies between two separately labelled heads, not in either.
        self.assertGreater(logits.grad[0, 2, 4, 4].item(), 0)

    def test_higher_false_bridge_probability_costs_more(self):
        logits, _, labels, _ = self.example()
        stronger = logits.detach().clone()
        stronger[0, 2, 4, 4] = 8.
        self.assertGreater(_head_negative_band(stronger, labels).item(),
                           _head_negative_band(logits, labels).item())

    def test_confident_false_bridge_retains_a_corrective_gradient(self):
        logits, _, labels, _ = self.example()
        with torch.no_grad():
            logits[0, 2, 4, 4] = 1000.
        _head_negative_band(logits, labels).backward()
        self.assertGreater(logits.grad[0, 2, 4, 4].item(), 0)
        self.assertTrue(torch.isfinite(logits.grad).all())

    def test_true_head_pixels_have_no_band_penalty_or_gradient(self):
        logits, _, labels, _ = self.example()
        stronger = logits.detach().clone()
        stronger[0, 2, 4, 2] = stronger[0, 2, 4, 6] = 20.
        torch.testing.assert_close(_head_negative_band(stronger, labels),
                                   _head_negative_band(logits, labels))
        _head_negative_band(logits, labels).backward()
        self.assertEqual(logits.grad[0, :, 4, 2].abs().sum().item(), 0)
        self.assertEqual(logits.grad[0, :, 4, 6].abs().sum().item(), 0)

    def test_band_is_exactly_two_pixels_wide(self):
        logits, _, labels, _ = self.example()
        labels.zero_()
        labels[0, 4, 4] = 2
        _head_negative_band(logits, labels).backward()
        self.assertGreater(logits.grad[0, 2, 2, 4].item(), 0)
        self.assertEqual(logits.grad[0, :, 1, 4].abs().sum().item(), 0)

    def test_other_semantic_ink_near_a_head_is_still_not_head_ink(self):
        logits, _, labels, _ = self.example()
        labels[0, 4, 4] = 1
        _head_negative_band(logits, labels).backward()
        self.assertGreater(logits.grad[0, 2, 4, 4].item(), 0)

    def test_no_heads_mean_differentiable_zero_band_loss(self):
        logits, _, labels, _ = self.example()
        labels.zero_()
        loss = _head_negative_band(logits, labels)
        self.assertEqual(loss.item(), 0)
        loss.backward()
        self.assertEqual(logits.grad.abs().sum().item(), 0)

    def test_teacher_is_detached_and_correct_class_is_encouraged(self):
        logits, _, labels, _ = self.example()
        teacher = self.confident_teacher(labels).requires_grad_()
        loss = _teacher_agreement(logits, labels, teacher)
        self.assertGreater(loss.item(), 0)
        loss.backward()
        self.assertIsNone(teacher.grad)
        self.assertLess(logits.grad[0, 2, 4, 2].item(), 0)

    def test_wrong_confident_teacher_is_completely_ignored(self):
        logits, _, labels, _ = self.example()
        teacher = self.confident_teacher((labels + 1) % 6).requires_grad_()
        loss = _teacher_agreement(logits, labels, teacher)
        self.assertEqual(loss.item(), 0)
        loss.backward()
        self.assertEqual(logits.grad.abs().sum().item(), 0)
        self.assertIsNone(teacher.grad)

    def test_uncertain_teacher_is_completely_ignored(self):
        logits, _, labels, _ = self.example()
        loss = _teacher_agreement(logits, labels, torch.zeros_like(logits))
        self.assertEqual(loss.item(), 0)
        loss.backward()
        self.assertEqual(logits.grad.abs().sum().item(), 0)

    def test_only_correct_pixels_in_mixed_teacher_contribute(self):
        labels = torch.tensor([[[0, 2]]])
        logits = torch.zeros((1, 6, 1, 2), requires_grad=True)
        teacher = self.confident_teacher(torch.tensor([[[0, 3]]]))
        loss = _teacher_agreement(logits, labels, teacher)
        expected = F.kl_div(logits[..., :1].log_softmax(1),
                            teacher[..., :1].softmax(1), reduction='sum')
        torch.testing.assert_close(loss, expected)
        loss.backward()
        self.assertGreater(logits.grad[..., :1].abs().sum().item(), 0)
        self.assertEqual(logits.grad[..., 1:].abs().sum().item(), 0)

    def test_per_class_normalization_prevents_background_domination(self):
        def reading(width):
            labels = torch.zeros((1, 1, width), dtype=torch.long)
            labels[0, 0, -1] = 2
            logits = torch.zeros((1, 6, 1, width))
            logits[0, 2, 0, -1] = 2.
            return _teacher_agreement(logits, labels, self.confident_teacher(labels))
        torch.testing.assert_close(reading(3), reading(101))

    def test_teacher_does_not_freeze_parent_missed_heads(self):
        logits, centres, labels, heat = self.example()
        teacher = self.confident_teacher((labels + 1) % 6)
        with_teacher = precision_losses(logits, centres, labels, heat, teacher)
        without_teacher = precision_losses(logits, centres, labels, heat)
        torch.testing.assert_close(with_teacher, without_teacher)
        with_teacher.backward()
        self.assertLess(logits.grad[0, 2, 4, 2].item(), 0)

    def test_teacher_term_has_the_declared_half_weight(self):
        logits, centres, labels, heat = self.example()
        teacher = self.confident_teacher(labels)
        without = precision_losses(logits, centres, labels, heat)
        with_teacher = precision_losses(logits, centres, labels, heat, teacher)
        torch.testing.assert_close(with_teacher-without,
                                   .5*_teacher_agreement(logits, labels, teacher))

    def test_centre_objective_retains_positive_and_negative_gradients(self):
        _, centres, _, heat = self.example()
        _centre_focal(centres, heat).backward()
        self.assertLess(centres.grad[0, 0, 4, 2].item(), 0)
        self.assertGreater(centres.grad[0, 0, 0, 0].item(), 0)

    def test_teacher_geometry_mismatch_is_rejected(self):
        logits, centres, labels, heat = self.example()
        with self.assertRaisesRegex(ValueError, 'Teacher logits'):
            precision_losses(logits, centres, labels, heat, torch.zeros((1, 6, 8, 9)))

    def test_invalid_student_shapes_are_rejected(self):
        logits, centres, labels, heat = self.example()
        for args in ((logits[:, :5], centres, labels, heat),
                     (logits, centres, labels[:, :8], heat),
                     (logits, centres, labels, heat[:, :8]),
                     (logits, centres[:, :, :8], labels, heat)):
            with self.subTest(shapes=[tuple(a.shape) for a in args]):
                with self.assertRaises(ValueError):
                    precision_losses(*args)


if __name__ == '__main__':
    unittest.main()
