# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original wire-only records, without score scans, melodies or personal data."""
import copy
import struct
import unittest
from sheet_interpreter import semantic_wire as wire


def fixture():
    evidence = [dict(sourceId='original-synthetic', pageIndex=0, visualX=.25,
                     staffIndex=0, staffCount=1, printedText='café 🎵')]
    directions = [dict(measureBoundary=0, kind=6, details=dict(
        quarterBeatOffset=1.5, eventId='ending-🎵', targetId='segno-é',
        codaTargetId='coda-β', repeatGroupId='r', totalPlays=3, passes=[1, 3],
        end=dict(measureIndex=3, quarterBeatOffset=0), afterJumpRepeats='PLAY',
        printedText='1., 3.', evidence=evidence))]
    expressions = [dict(eventId='kind-'+kind, kind=kind,
        start=dict(measureIndex=0, quarterBeatOffset=.5),
        end=dict(measureIndex=3, quarterBeatOffset=0), scope='UNRESOLVED',
        staffIndex=0, staffCount=1, targetEventId=None, strength='POCO',
        qualifierText='original wire fixture', evidence=evidence) for kind in wire.KINDS]
    expressions[5]['targetEventId'] = 'kind-RITARDANDO'
    return directions, expressions


class SemanticWireTest(unittest.TestCase):
    def test_all_kinds_unicode_targets_and_cross_page_end_roundtrip(self):
        directions, expressions = fixture()
        before = copy.deepcopy((directions, expressions))
        data = wire.encode(directions, expressions, 1)
        self.assertEqual((directions, expressions), wire.decode(data, 1))
        self.assertEqual(before, (directions, expressions))
        self.assertEqual(data, wire.encode(*wire.decode(data, 1), 1))

    def test_every_truncated_prefix_and_trailing_byte_is_rejected(self):
        data = wire.encode(*fixture(), 1)
        for end in range(len(data)):
            with self.assertRaises(ValueError, msg=str(end)): wire.decode(data[:end], 1)
        with self.assertRaises(ValueError): wire.decode(data+b'\x00', 1)

    def test_unknown_kind_counts_frame_length_and_utf8_are_rejected(self):
        encoded = wire.encode(*fixture(), 1)
        for offset, value in ((12, 14), (4, 2**31-1), (0, -1)):
            data = bytearray(encoded); struct.pack_into('>i', data, offset, value)
            with self.assertRaises(ValueError): wire.decode(data, 1)
        data = bytearray(encoded); data[28] = 0xff
        with self.assertRaises(ValueError): wire.decode(data, 1)

    def test_duplicate_expression_identity_is_not_silently_merged(self):
        directions, expressions = fixture()
        expressions.append(copy.deepcopy(expressions[0]))
        with self.assertRaises(ValueError): wire.encode(directions, expressions, 1)

    def test_anonymous_rich_navigation_and_unpaired_surrogates_are_rejected(self):
        directions, expressions = fixture()
        directions[0]['details']['eventId'] = ''
        with self.assertRaises(ValueError): wire.encode(directions, expressions, 1)
        directions, expressions = fixture()
        expressions[0]['eventId'] = 'bad-\ud800'
        with self.assertRaises(ValueError): wire.encode(directions, expressions, 1)

    def test_invalid_optional_boolean_is_rejected(self):
        # Legacy-default direction frame: four empty identity strings, total count,
        # pass count, then the optional endpoint boolean.
        data = bytearray(wire.encode([dict(measureBoundary=0, kind=0)], [], 1))
        data[8+16+4*4+8] = 2
        with self.assertRaises(ValueError): wire.decode(data, 1)


if __name__ == '__main__':
    unittest.main()
