# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
import unittest
from test_tremolo import attacks, note


def solo(start, duration, tied=True):
    return dict(note(start, duration, tied=tied), staffCount=1)


class HiddenStaffTieMidiTests(unittest.TestCase):
    def test_verified_tie_keeps_one_attack_and_full_chain(self):
        events = attacks([note(0, 1), solo(1, 1), solo(2, 1)])
        self.assertEqual([0], [t for t, s, p, v in events if s & 0xF0 == 0x90])
        self.assertEqual(1440, events[-1][0])

    def test_returning_lower_staff_keeps_tie(self):
        events = attacks([solo(0, 1, False), note(1, 1, tied=True)])
        self.assertEqual(1, sum(s & 0xF0 == 0x90 for t, s, p, v in events))

    def test_unmarked_notes_keep_separate_attacks(self):
        events = attacks([note(0, 1), solo(1, 1, False)])
        self.assertEqual(2, sum(s & 0xF0 == 0x90 for t, s, p, v in events))

    def test_silent_gap_cannot_be_filled_by_tie(self):
        events = attacks([note(0, .5), solo(1, 1)])
        self.assertEqual(2, sum(s & 0xF0 == 0x90 for t, s, p, v in events))

    def test_lower_staff_cannot_become_top_staff(self):
        events = attacks([note(0, 1, staff=1), solo(1, 1)])
        self.assertEqual(2, sum(s & 0xF0 == 0x90 for t, s, p, v in events))
