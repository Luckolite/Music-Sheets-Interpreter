# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Precision-preserving loss for original renderer-labelled scan pairs.

Teacher logits must use the same pixel geometry as the target labels. The
teacher is a frozen parent, not a source of replacement labels: only confident
predictions that agree with renderer truth contribute to the optional anchor.
These training losses do not replace the unchanged development/release gates.
"""
import torch
from torch.nn import functional as F


CLASS_WEIGHTS = (1., 1.5, 2., 2., 1., 1.5)
DICE_WEIGHT = .4
CENTRE_WEIGHT = .25
HEAD_BAND_WEIGHT = .3
TEACHER_WEIGHT = .5
TEACHER_CONFIDENCE = .9


def _head_negative_band(logits, labels):
    """Penalize class2 spill/bridges within two pixels of true head ink.

Actual head pixels and pixels outside the 5x5 dilation have exactly zero
contribution. Other true semantic classes in this band are still non-heads.
The log-sum-exp form stays finite and useful for confidently false bridges.
"""
    truth = labels == 2
    dilated = F.max_pool2d(truth[:, None].float(), 5, stride=1, padding=2)[:, 0] > 0
    band = dilated & ~truth
    scores = logits.float()
    not_heads = torch.cat((scores[:, :2], scores[:, 3:]), dim=1)
    negative = torch.logsumexp(scores, dim=1) - torch.logsumexp(not_heads, dim=1)
    return (negative * band).sum() / band.sum().clamp_min(1)


def _teacher_agreement(logits, labels, teacher_logits):
    """Class-balanced KL only at confident, renderer-correct teacher pixels."""
    if teacher_logits is None:
        return logits.float().sum() * 0.
    if teacher_logits.shape != logits.shape:
        raise ValueError('Teacher logits must match the student pixel geometry and classes')
    teacher = teacher_logits.detach().float().softmax(dim=1)
    confidence, predicted = teacher.max(dim=1)
    eligible = (predicted == labels) & (confidence >= TEACHER_CONFIDENCE)
    divergence = F.kl_div(logits.float().log_softmax(dim=1), teacher,
                          reduction='none').sum(dim=1)
    total = divergence.sum() * 0.
    represented = divergence.new_zeros(())
    for category in range(logits.shape[1]):
        selected = eligible & (labels == category)
        count = selected.sum()
        total = total + (divergence * selected).sum() / count.clamp_min(1)
        represented = represented + (count > 0).to(divergence.dtype)
    return total / represented.clamp_min(1)


def _centre_focal(centres, heat):
    """Retain the parent's exact centre objective and unit-peak convention."""
    p = centres[:, 0].float().sigmoid().clamp(1e-5, 1-1e-5)
    positive = (heat == 1).float()
    negative = heat < 1
    loss = (-torch.log(p) * (1-p).square() * positive
            - torch.log1p(-p) * p.square() * (1-heat).pow(4) * negative)
    return loss.sum() / positive.sum().clamp_min(1)


def precision_losses(logits, centres, labels, heat, teacher_logits=None):
    """Return scalar CE + Dice + centre + anti-bridge + optional parent KL.

Expected shapes: logits/teacher N,6,H,W; centres N,1,H,W;
labels/heat N,H,W. Teacher tensors are always detached. Incorrect or uncertain
teacher predictions are ignored, so ground-truth corrections remain learnable.
"""
    if logits.ndim != 4 or logits.shape[1] != len(CLASS_WEIGHTS):
        raise ValueError('Six semantic logits are required')
    expected = (logits.shape[0], logits.shape[2], logits.shape[3])
    if tuple(labels.shape) != expected or tuple(heat.shape) != expected:
        raise ValueError('Labels and heat must match the student pixel geometry')
    if tuple(centres.shape) != (expected[0], 1, expected[1], expected[2]):
        raise ValueError('One centre channel must match the student pixel geometry')
    scores = logits.float()
    ce = F.cross_entropy(scores, labels, weight=scores.new_tensor(CLASS_WEIGHTS))
    probabilities = scores.softmax(dim=1)
    truth = F.one_hot(labels, len(CLASS_WEIGHTS)).permute(0, 3, 1, 2).float()
    dice = 1 - (2*(probabilities*truth).sum((0, 2, 3))+1) / (
        (probabilities+truth).sum((0, 2, 3))+1)
    return (ce + DICE_WEIGHT*dice[1:].mean() + CENTRE_WEIGHT*_centre_focal(centres, heat)
            + HEAD_BAND_WEIGHT*_head_negative_band(scores, labels)
            + TEACHER_WEIGHT*_teacher_agreement(scores, labels, teacher_logits))
