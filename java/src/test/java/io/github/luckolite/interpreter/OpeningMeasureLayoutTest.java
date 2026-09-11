// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class OpeningMeasureLayoutTest {
    private static final List<MeasureRegion> SHORT=List.of(new MeasureRegion(.1f,.16f,.2f,.3f),
            new MeasureRegion(.17f,.49f,.2f,.3f),new MeasureRegion(.50f,.85f,.2f,.3f));
    private static ScoreNoteEvent eighth(float x) {
        return new ScoreNoteEvent(0,x,2,0,1,.25f,false,0,1,2,0);
    }
    private static double onset(ScoreNoteEvent n,List<ScoreNoteEvent> notes,float beats) {
        return ScoreNoteTiming.beatInMeasure(n,notes,beats);
    }
    @Test public void eighthPickupEndsAtFollowingBarline() {
        var marked=OpeningMeasureLayout.mark(List.of(eighth(.7f)),List.of(),SHORT);
        assertTrue(marked.get(0).compactOpening());
        assertEquals(5.5,onset(marked.get(0),marked,6),.0001);
        assertEquals(.5,ScoreNoteTiming.writtenDurationBeats(marked.get(0)),.0001);
    }
    @Test public void severalPickupNotesKeepTheirWrittenSpacing() {
        var marked=OpeningMeasureLayout.mark(List.of(eighth(.45f),eighth(.65f),eighth(.82f)),List.of(),SHORT);
        for(int i=0;i<3;i++)assertEquals(2.5+i*.5,onset(marked.get(i),marked,4),.0001);
    }
    @Test public void fullWidthBarDoesNotBecomeAPickup() {
        var notes=List.of(eighth(.7f));
        var measures=List.of(new MeasureRegion(.1f,.4f,.2f,.3f),new MeasureRegion(.41f,.65f,.2f,.3f),new MeasureRegion(.66f,.9f,.2f,.3f));
        var marked=OpeningMeasureLayout.mark(notes,List.of(),measures);
        assertFalse(marked.get(0).compactOpening());
        assertEquals(onset(notes.get(0),notes,4),onset(marked.get(0),marked,4),.0001);
    }
    @Test public void fullWrittenMeasureStaysAtBeatZeroEvenIfEngravedNarrowly() {
        var note=new ScoreNoteEvent(0,.65f,2,0,1,.25f,false,0,0,2,4);
        var marked=OpeningMeasureLayout.mark(List.of(note),List.of(),SHORT);
        assertEquals(0,onset(marked.get(0),marked,4),.0001);
    }
    @Test public void detectedRestPreventsAnIncompleteBarFromBeingShifted() {
        var notes=List.of(eighth(.7f));
        var rests=List.of(new ScoreRestEvent(0,.45f,.25f,.02f,0,1,2));
        assertFalse(OpeningMeasureLayout.mark(notes,rests,SHORT).get(0).compactOpening());
        assertFalse(OpeningMeasureLayout.mark(List.of(eighth(.7f).withLeadingRest(2)),List.of(),SHORT).get(0).compactOpening());
    }
    @Test public void tiedPageContinuationDoesNotBecomeAnOpeningPickup() {
        var note=new ScoreNoteEvent(0,.7f,2,0,1,.25f,true,0,1);
        assertFalse(OpeningMeasureLayout.mark(List.of(note),List.of(),SHORT).get(0).compactOpening());
    }
    @Test public void followingRowsCannotEstablishTheOpeningWidth() {
        var measures=List.of(SHORT.get(0),new MeasureRegion(.1f,.4f,.4f,.5f),new MeasureRegion(.41f,.85f,.4f,.5f));
        assertFalse(OpeningMeasureLayout.mark(List.of(eighth(.7f)),List.of(),measures).get(0).compactOpening());
    }
    @Test public void earlyAttackDoesNotLookLikeHeaderPaddedPickup() {
        assertFalse(OpeningMeasureLayout.mark(List.of(eighth(.1f)),List.of(),SHORT).get(0).compactOpening());
    }
    @Test public void unknownWrittenDurationKeepsExistingClock() {
        var note=new ScoreNoteEvent(0,.7f,2,0,1);
        var marked=List.of(note.withCompactOpening());
        assertEquals(onset(note,List.of(note),4),onset(marked.get(0),marked,4),.0001);
    }
    @Test public void noteCopiesRetainLayoutMetadata() {
        var note=eighth(.7f).withCompactOpening();
        assertTrue(note.withArticulations(NoteArticulation.TENUTO).compactOpening());
        assertTrue(note.withClef(ScoreNoteEvent.CLEF_TREBLE).compactOpening());
        assertTrue(note.withCrossStaffBeam().compactOpening());
        assertTrue(note.withLeadingRest(1).compactOpening());
    }
    @Test public void matchingPartsShareOnePickupClock() {
        var upper=new ScoreNoteEvent(0,.7f,2,0,2,.25f,false,0,1,2,0).withCompactOpening();
        var lower=new ScoreNoteEvent(0,.7f,0,1,2,.28f,false,0,1,2,0).withCompactOpening();
        var notes=List.of(upper,lower);
        assertEquals(3.5,onset(upper,notes,4),.0001);
        assertEquals(3.5,onset(lower,notes,4),.0001);
    }
    @Test public void unequalPartsNeedTheirMissingRhythmResolvedFirst() {
        var upper=new ScoreNoteEvent(0,.7f,2,0,2,.25f,false,0,1,2,0);
        var lower=new ScoreNoteEvent(0,.7f,0,1,2,.28f,false,0,0,2,1);
        var original=List.of(upper,lower);var marked=List.of(upper.withCompactOpening(),lower.withCompactOpening());
        assertEquals(onset(upper,original,4),onset(marked.get(0),marked,4),.0001);
        assertEquals(onset(lower,original,4),onset(marked.get(1),marked,4),.0001);
    }
}
