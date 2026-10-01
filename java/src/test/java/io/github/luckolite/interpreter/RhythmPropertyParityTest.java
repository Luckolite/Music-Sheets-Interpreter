// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Original generated chords, grace notes and incomplete rhythm metadata. */
public class RhythmPropertyParityTest {
    private static List<ScoreNoteEvent> score(int seed) {
        Random random = new Random(seed);
        List<ScoreNoteEvent> notes = new ArrayList<>();
        float[] durations = {0, 1, 2, 4, Float.NaN};
        for (int measure = 0; measure < 2; measure++) {
            for (int staff = 0; staff < 2; staff++) {
                for (int attack = 0; attack < 6; attack++) {
                    float position = .05f + attack * .15f;
                    for (int chord = 0; chord < 1 + seed % 3; chord++) {
                        ScoreNoteEvent note =
                                new ScoreNoteEvent(
                                        measure,
                                        position,
                                        chord * 3 + random.nextInt(2),
                                        staff,
                                        2,
                                        .2f + staff * .25f,
                                        random.nextBoolean(),
                                        random.nextInt(3),
                                        random.nextInt(4),
                                        ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                        durations[random.nextInt(durations.length)],
                                        new int[] {1, 3, 5}[random.nextInt(3)]);
                        if (attack == 0) note = note.withLeadingRest(.125f * (staff + 1));
                        if (seed % 4 == 0 && chord == 0) note = note.withCrossStaffBeam();
                        if (seed % 5 == 0 && attack == 2 && chord == 0)
                            note = note.withArticulations(NoteOrnament.GRACE);
                        notes.add(note);
                    }
                }
            }
        }
        Collections.shuffle(notes, random);
        return List.copyOf(notes);
    }

    private static String fingerprint(boolean session) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer bytes = ByteBuffer.allocate(24);
        float[] meters = {4, 3, 6, .125f, 128, Float.NaN, Float.POSITIVE_INFINITY};
        for (int seed = 0; seed < 24; seed++) {
            List<ScoreNoteEvent> notes = score(seed);
            try (ScoreNoteTiming.TimingSession reuse =
                    session ? ScoreNoteTiming.beginTimingSession() : null) {
                for (float meter : meters) {
                    for (ScoreNoteEvent note : notes) {
                        bytes.clear();
                        bytes.putLong(
                                Double.doubleToLongBits(
                                        ScoreNoteTiming.beatInMeasure(note, notes, meter)));
                        bytes.putLong(
                                Double.doubleToLongBits(
                                        ScoreNoteTiming.resolvedWrittenDurationBeats(
                                                note, notes, meter)));
                        bytes.putLong(ScoreNoteTiming.hasIndependentDuration(note, notes) ? 1 : 0);
                        digest.update(bytes.array());
                    }
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @Test
    public void cachedPropertiesPreserveBaselineTimingForMixedChords() throws Exception {
        // Captured from the pre-cache implementation using this original fixture.
        String expected = "ec4d993ae21c1cec8572c53c5f8f5b3a15d8f7c0bdad5af1d4c5f055ddb59b9b";
        assertEquals(expected, fingerprint(false));
        assertEquals(expected, fingerprint(true));
    }
}
