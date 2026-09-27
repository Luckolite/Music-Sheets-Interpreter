// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Ownership tests use an artificial glyph, independent of any engraving font. */
public class OrnamentNoteOwnershipTest {
    private final byte[] gray = new byte[300 * 150];
    private final PortableOrnamentGlyphs glyphs = new PortableOrnamentGlyphs();
    private final List<PlayingTechniqueDetector.Staff> staffs =
            List.of(new PlayingTechniqueDetector.Staff(80, 120, 10, 0, 1));

    public OrnamentNoteOwnershipTest() {
        Arrays.fill(gray, (byte) 255);
        for (int y = 60; y < 70; y++) for (int x = 100; x < 112; x++) gray[y * 300 + x] = 0;
        glyphs.add(new byte[120], 12, 10, NoteOrnament.MORDENT, false);
    }

    private List<PortableNoteOrnaments.Found> detect(List<PortableNoteOrnaments.Anchor> notes) {
        return PortableNoteOrnaments.detect(glyphs, gray, 300, 150, staffs, notes);
    }

    private PortableNoteOrnaments.Anchor note(float x, float y) {
        return new PortableNoteOrnaments.Anchor(x, y, 10, 0, 0);
    }

    @Test
    public void writtenLedgerHeadCannotBecomeLowerNotesOrnament() {
        assertTrue(detect(List.of(note(106, 65), note(106, 100))).isEmpty());
    }

    @Test
    public void realSymbolAboveNoteStillAttaches() {
        var found = detect(List.of(note(106, 100)));
        assertEquals(1, found.size());
        assertEquals(NoteOrnament.MORDENT, found.get(0).marks());
    }

    @Test
    public void neighboringHeadOutsideSymbolDoesNotSuppress() {
        assertEquals(1, detect(List.of(note(120, 65), note(106, 100))).size());
    }

    @Test
    public void ownershipProtectsHeadOnAnotherStaffToo() {
        assertTrue(
                detect(
                                List.of(
                                        new PortableNoteOrnaments.Anchor(106, 65, 10, -1, 0),
                                        note(106, 100)))
                        .isEmpty());
    }
}
