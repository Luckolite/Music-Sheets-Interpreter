# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original synthetic metric dictionaries; no images, checkpoints or GPU calls."""
from copy import deepcopy
from importlib.util import find_spec
import unittest

if find_spec('torch') is None:
    gate = None
else:
    from train_hard_scan import gate


HARD = ('warp', 'faint', 'shadow', 'thin', 'combined')
CLASSES = ('background', 'stems_rests_barlines', 'noteheads',
           'clefs_keys_accidentals', 'staff', 'other_symbols')


def heads(tp, fp, fn):
    return dict(tp=tp, fp=fp, fn=fn, precision=tp / max(1, tp + fp),
                recall=tp / max(1, tp + fn))


def stratum(tp=900, fp=100, fn=100):
    return dict(
        heads=heads(tp, fp, fn),
        crop_heads=[dict(tp=tp, fp=fp, fn=fn)],
        pixel=dict(per_class={name: dict(pixels=1000, recall=.9, precision=.9)
                              for name in CLASSES}),
        renderer_category_pixel_recall={
            str(category): dict(pixels=1000, correct=900, recall=.9)
            for category in range(3, 17)},
    )


def examples():
    baseline = {name: stratum() for name in HARD}
    baseline['clean'] = stratum(198, 2, 2)
    baseline['clean']['crop_heads'] = [dict(tp=99, fp=1, fn=1) for _ in range(2)]
    baseline['blank'] = dict(heads=heads(0, 2, 0))
    report = deepcopy(baseline)
    for name in HARD:
        report[name]['heads'] = heads(950, 50, 50)
        report[name]['crop_heads'] = [dict(tp=950, fp=50, fn=50)]
    return report, baseline


@unittest.skipIf(gate is None, 'PyTorch is an optional training dependency')
class HardScanGateTest(unittest.TestCase):
    def setUp(self):
        self.report, self.baseline = examples()

    def assert_gain_cannot_hide(self, failure):
        result = gate(self.report, self.baseline)
        self.assertGreater(result['mean_hard_head_f1_gain'], .003)
        self.assertFalse(result['passed'])
        self.assertIn(failure, result['failures'])
        return result

    def test_gain_with_all_retention_checks_passes_development_only(self):
        result = gate(self.report, self.baseline)
        self.assertTrue(result['passed'])
        self.assertEqual(result['failures'], [])
        self.assertAlmostEqual(result['mean_hard_head_f1_gain'], .05)
        for required in ('Development', 'source', 'sealed', 'export', 'native'):
            self.assertIn(required, result['scope'])

    def test_unchanged_model_is_not_a_gain(self):
        result = gate(deepcopy(self.baseline), self.baseline)
        self.assertFalse(result['passed'])
        self.assertEqual(result['mean_hard_head_f1_gain'], 0)
        self.assertEqual(result['failures'], ['hard-scan head F1 gain below 0.003'])

    def test_gain_below_threshold_is_not_enough(self):
        for name in HARD:
            self.report[name]['heads'] = heads(902, 98, 98)
        result = gate(self.report, self.baseline)
        self.assertAlmostEqual(result['mean_hard_head_f1_gain'], .002)
        self.assertFalse(result['passed'])
        self.assertIn('hard-scan head F1 gain below 0.003', result['failures'])

    def test_blank_false_positive_increase_blocks_large_hard_gain(self):
        self.report['blank']['heads'] = heads(0, 3, 0)
        self.assert_gain_cannot_hide('blank false heads increased')

    def test_fewer_blank_false_positives_are_allowed(self):
        self.report['blank']['heads'] = heads(0, 1, 0)
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_clean_false_positive_swap_fails_despite_unchanged_aggregate(self):
        self.report['clean']['crop_heads'][0]['fp'] += 1
        self.report['clean']['crop_heads'][1]['fp'] -= 1
        self.assertEqual(self.report['clean']['heads'], self.baseline['clean']['heads'])
        self.assert_gain_cannot_hide('clean crop head retention: 0')

    def test_clean_missed_note_swap_fails_despite_unchanged_aggregate(self):
        self.report['clean']['crop_heads'] = [dict(tp=98, fp=1, fn=2),
                                              dict(tp=100, fp=1, fn=0)]
        self.assertEqual(self.report['clean']['heads'], self.baseline['clean']['heads'])
        self.assert_gain_cannot_hide('clean crop head retention: 0')

    def test_clean_retention_checks_later_crops_too(self):
        self.report['clean']['crop_heads'] = [dict(tp=100, fp=1, fn=0),
                                              dict(tp=98, fp=1, fn=2)]
        self.assert_gain_cannot_hide('clean crop head retention: 1')

    def test_clean_true_positive_loss_is_explicitly_guarded(self):
        self.report['clean']['crop_heads'][0]['tp'] -= 1
        self.assert_gain_cannot_hide('clean crop head retention: 0')

    def test_clean_false_negative_increase_is_explicitly_guarded(self):
        self.report['clean']['crop_heads'][0]['fn'] += 1
        self.assert_gain_cannot_hide('clean crop head retention: 0')

    def test_hard_precision_loss_is_not_hidden_by_other_strata(self):
        self.report['warp']['heads'] = heads(950, 150, 50)
        self.assert_gain_cannot_hide('warp head precision')

    def test_hard_recall_loss_is_not_hidden_by_other_strata(self):
        self.report['thin']['heads'] = heads(875, 25, 125)
        self.assert_gain_cannot_hide('thin head recall')

    def test_each_semantic_class_in_each_stratum_is_guarded(self):
        for name in ('clean',) + HARD:
            for semantic in CLASSES:
                with self.subTest(stratum=name, semantic=semantic):
                    self.report, self.baseline = examples()
                    self.report[name]['pixel']['per_class'][semantic]['recall'] = .88
                    self.assert_gain_cannot_hide(name + ' semantic recall: ' + semantic)

    def test_semantic_recall_within_declared_tolerance_is_allowed(self):
        self.report['shadow']['pixel']['per_class']['noteheads']['recall'] = .89
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_each_semantic_precision_in_each_stratum_is_guarded(self):
        for name in ('clean',) + HARD:
            for semantic in CLASSES:
                with self.subTest(stratum=name, semantic=semantic):
                    self.report, self.baseline = examples()
                    self.report[name]['pixel']['per_class'][semantic]['precision'] = .88
                    self.assert_gain_cannot_hide(name + ' semantic precision: ' + semantic)

    def test_semantic_precision_within_declared_tolerance_is_allowed(self):
        self.report['shadow']['pixel']['per_class']['noteheads']['precision'] = .89
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_unrepresented_semantic_class_does_not_create_false_failure(self):
        self.baseline['faint']['pixel']['per_class']['other_symbols']['pixels'] = 0
        self.report['faint']['pixel']['per_class']['other_symbols']['recall'] = 0
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_each_renderer_category_in_each_stratum_is_guarded(self):
        for name in ('clean',) + HARD:
            for category in range(3, 17):
                with self.subTest(stratum=name, category=category):
                    self.report, self.baseline = examples()
                    value = self.report[name]['renderer_category_pixel_recall'][str(category)]
                    value.update(recall=.87, correct=870)
                    self.assert_gain_cannot_hide(
                        name + ' renderer-category pixel recall: ' + str(category))

    def test_renderer_category_minimum_support_is_inclusive(self):
        before = self.baseline['combined']['renderer_category_pixel_recall']['16']
        after = self.report['combined']['renderer_category_pixel_recall']['16']
        before.update(pixels=100, correct=90)
        after.update(pixels=100, correct=80, recall=.8)
        self.assert_gain_cannot_hide('combined renderer-category pixel recall: 16')

    def test_low_support_category_follows_declared_screening_floor(self):
        self.baseline['combined']['renderer_category_pixel_recall']['16']['pixels'] = 99
        self.report['combined']['renderer_category_pixel_recall']['16']['recall'] = 0
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_renderer_category_within_declared_tolerance_is_allowed(self):
        self.report['warp']['renderer_category_pixel_recall']['3']['recall'] = .885
        self.assertTrue(gate(self.report, self.baseline)['passed'])

    def test_independent_failures_are_all_reported(self):
        self.report['blank']['heads'] = heads(0, 3, 0)
        self.report['clean']['crop_heads'][1]['fp'] += 1
        self.report['faint']['pixel']['per_class']['noteheads']['recall'] = .8
        self.report['combined']['renderer_category_pixel_recall']['7']['recall'] = .8
        result = self.assert_gain_cannot_hide('blank false heads increased')
        self.assertIn('clean crop head retention: 1', result['failures'])
        self.assertIn('faint semantic recall: noteheads', result['failures'])
        self.assertIn('combined renderer-category pixel recall: 7', result['failures'])
        self.assertEqual(len(result['failures']), 4)

    def test_gate_does_not_mutate_either_report(self):
        report, baseline = deepcopy(self.report), deepcopy(self.baseline)
        gate(self.report, self.baseline)
        self.assertEqual(self.report, report)
        self.assertEqual(self.baseline, baseline)


if __name__ == '__main__':
    unittest.main()
