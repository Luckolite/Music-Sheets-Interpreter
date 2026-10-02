// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic attacks and part/rest/chord boundaries, without score imagery. */
public class SlidePitchSourceTest {
    private ScoreNoteEvent note(int bar, float x, int step) {
        return new ScoreNoteEvent(
                        bar, x, step, 0, 1, .5f, false, 0, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY, 1)
                .withClef(ScoreNoteEvent.CLEF_TREBLE);
    }

    private ScoreNoteEvent slide(int bar, float x, int step) {
        return note(bar, x, step)
                .withArticulations(NoteOrnament.SLIDE | NoteOrnament.FROM_PREVIOUS);
    }

    @Test
    public void bindsPreviousBarTerminalAttack() {
        var a = note(2, .8f, 7);
        var b = slide(3, .02f, 3);
        assertSame(a, SlidePitchSource.previous(b, List.of(b, a)));
    }

    @Test
    public void nearestSameBarAttackTakesPrecedence() {
        var a = note(2, .8f, 7);
        var b = note(3, .1f, 4);
        var c = slide(3, .4f, 3);
        assertSame(b, SlidePitchSource.previous(c, List.of(c, a, b)));
    }

    @Test
    public void sourceOrderDoesNotChooseAnEarlierAttack() {
        var a = note(2, .2f, 5);
        var b = note(2, .8f, 7);
        var c = slide(3, 0, 3);
        assertSame(b, SlidePitchSource.previous(c, List.of(b, c, a)));
    }

    @Test
    public void ambiguousPreviousChordIsRejected() {
        var a = note(2, .8f, 7);
        var b = note(2, .8f, 5);
        var c = slide(3, 0, 3);
        assertNull(SlidePitchSource.previous(c, List.of(a, b, c)));
    }

    @Test
    public void anotherPartIsRejected() {
        var a = new ScoreNoteEvent(2, .8f, 7, 1, 2, .5f, false, 0, 0, 2, 1).withClef(30);
        assertNull(SlidePitchSource.previous(slide(3, 0, 3), List.of(a)));
    }

    @Test
    public void anotherClefIsRejected() {
        assertNull(
                SlidePitchSource.previous(slide(3, 0, 3), List.of(note(2, .8f, 7).withClef(18))));
    }

    @Test
    public void cannotSkipAnEmptyBar() {
        assertNull(SlidePitchSource.previous(slide(3, 0, 3), List.of(note(1, .8f, 7))));
    }

    @Test
    public void cannotBindFutureAttack() {
        assertNull(
                SlidePitchSource.previous(
                        slide(3, .2f, 3), List.of(note(3, .3f, 7), note(4, 0, 4))));
    }

    @Test
    public void leadingRestPreventsBarCrossing() {
        assertNull(
                SlidePitchSource.previous(
                        slide(3, .2f, 3).withLeadingRest(1), List.of(note(2, .8f, 7))));
    }

    @Test
    public void followingRestPreventsBarCrossing() {
        var a = new ScoreNoteEvent(2, .8f, 7, 0, 1, .5f, false, 0, 0, 2, 1, 1, 1).withClef(30);
        assertNull(SlidePitchSource.previous(slide(3, 0, 3), List.of(a)));
    }

    @Test
    public void isolatedApproachDoesNotBindPreviousAttack() {
        assertNull(
                SlidePitchSource.previous(
                        note(3, 0, 3).withArticulations(NoteOrnament.SLIDE),
                        List.of(note(2, .8f, 7))));
    }

    @Test
    public void malformedAndMissingInputsAreRejected() {
        assertNull(SlidePitchSource.previous(null, List.of()));
        assertNull(SlidePitchSource.previous(slide(3, 0, 3), null));
        assertNull(SlidePitchSource.previous(slide(3, Float.NaN, 3), List.of(note(2, .8f, 7))));
    }
}
