// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original opposing shafts and note events; arc proof is still required separately. */
public class IndependentTieVoiceTest {
    static final int W = 1000, H = 200;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public IndependentTieVoiceTest() {
        Arrays.fill(gray, (byte) 255);
    }

    Object make(String name, Object... args) throws Exception {
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$" + name)
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(args);
    }

    Object note(
            int measure,
            int x,
            int pitch,
            float position,
            boolean up,
            boolean print,
            boolean dotted)
            throws Exception {
        int y = 80 - pitch * 7;
        if (print)
            for (int yy = up ? y - 45 : y; yy <= (up ? y : y + 45); yy++)
                for (int xx = x + (up ? 7 : -7); xx <= x + (up ? 8 : -6); xx++)
                    gray[yy * W + xx] = 0;
        Object head = make("Component", 150, x - 7, x + 7, y - 7, y + 7, (float) x, (float) y);
        var event =
                new ScoreNoteEvent(
                        measure,
                        position,
                        pitch,
                        0,
                        1,
                        y / (float) H,
                        false,
                        dotted ? 1 : 0,
                        0,
                        2,
                        1);
        return make("DetectedNote", event, head, 14f);
    }

    int previous(boolean opposite, boolean print, boolean reattack) throws Exception {
        var notes = new ArrayList<Object>();
        notes.add(note(0, 100, 0, .2f, false, true, true));
        if (reattack) notes.add(note(0, 155, 0, .4f, false, true, false));
        notes.add(note(0, 220, 2, .7f, opposite, print, false));
        notes.add(note(1, 300, 0, .1f, false, true, false));
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "previousSamePitch",
                        List.class,
                        int.class,
                        int.class,
                        byte[].class,
                        byte[].class,
                        int.class);
        m.setAccessible(true);
        return (int) m.invoke(null, notes, notes.size() - 1, W, labels, gray, H);
    }

    private Object shortNote(int x, int pitch, boolean up) throws Exception {
        Object n = note(0, x, pitch, x / (float) W, up, true, false);
        var type = n.getClass();
        var head = type.getDeclaredMethod("head");
        head.setAccessible(true);
        var event =
                new ScoreNoteEvent(
                        0,
                        x / (float) W,
                        pitch,
                        0,
                        1,
                        (80 - pitch * 7) / (float) H,
                        false,
                        0,
                        2,
                        2,
                        0);
        return make("DetectedNote", event, head.invoke(n), 14f);
    }

    private int shortPrevious(boolean opposing) throws Exception {
        var notes =
                List.of(
                        shortNote(100, 0, false),
                        shortNote(220, 2, opposing),
                        shortNote(300, 0, false));
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "previousSamePitch",
                        List.class,
                        int.class,
                        int.class,
                        byte[].class,
                        byte[].class,
                        int.class);
        m.setAccessible(true);
        return (int) m.invoke(null, notes, 2, W, labels, gray, H);
    }

    @Test
    public void shortVoiceCanTiePastOpposingAttack() throws Exception {
        assertEquals(0, shortPrevious(true));
    }

    @Test
    public void shortMelodyCannotSkipSameDirectionAttack() throws Exception {
        assertEquals(-1, shortPrevious(false));
    }

    @Test
    public void dottedVoiceCanContinueAcrossBarOverOpposingMovingVoice() throws Exception {
        assertEquals(0, previous(true, true, false));
    }

    @Test
    public void sameDirectionMelodyCannotBorrowOlderEndpoint() throws Exception {
        assertEquals(-1, previous(false, true, false));
    }

    @Test
    public void unprovedMiddleVoiceDoesNotAuthorizeTie() throws Exception {
        assertEquals(-1, previous(true, false, false));
    }

    @Test
    public void interveningSamePitchConsumesOldEndpoint() throws Exception {
        assertNotEquals(0, previous(true, true, true));
    }

    @Test
    public void longerOpposingChordShaftDoesNotHideShorterTiedVoice() throws Exception {
        for (int x : new int[] {107, 108, 307, 308})
            for (int y = 15; y <= 80; y++) gray[y * W + x] = 0;
        assertEquals(0, previous(true, true, false));
    }

    @Test
    public void ambiguousMiddleChordCannotProveIndependentVoice() throws Exception {
        for (int x : new int[] {213, 214}) for (int y = 66; y <= 112; y++) gray[y * W + x] = 0;
        assertEquals(-1, previous(true, true, false));
    }
}
