// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrintedOpeningPreservationTest {
    private final MeasureRegion first=new MeasureRegion(.1f,.16f,.2f,.3f);
    private final MeasureRegion second=new MeasureRegion(.17f,.49f,.2f,.3f);
    private final MeasureRegion rest=new MeasureRegion(.5f,.85f,.2f,.3f);
    private final List<MeasureRegion> raw=List.of(first,second,rest);
    private final List<MeasureRegion> fitted=List.of(new MeasureRegion(.1f,.49f,.2f,.3f),rest,rest);
    private ScoreNoteEvent note(boolean compact) {
        var n=new ScoreNoteEvent(0,.7f,2,0,1,.25f,false,0,0,2,1);
        return compact?n.withCompactOpening():n;
    }
    @Test public void restoresWrittenPickupWithoutUndoingRestExpansion() {
        assertEquals(List.of(first,second,rest,rest),PrintedMeasureRhythmGuard.preserveWrittenOpening(raw,fitted,List.of(note(true))));
    }
    @Test public void anEmptyHeaderHasNoPickupToRestore() {
        assertSame(fitted,PrintedMeasureRhythmGuard.preserveWrittenOpening(raw,fitted,List.of()));
    }
    @Test public void ordinaryNotesDoNotOverrideTheMeasureFit() {
        assertSame(fitted,PrintedMeasureRhythmGuard.preserveWrittenOpening(raw,fitted,List.of(note(false))));
    }
    @Test public void aPartialCutIsNotAMergedOpening() {
        var partial=List.of(new MeasureRegion(.1f,.3f,.2f,.3f),rest);
        assertSame(partial,PrintedMeasureRhythmGuard.preserveWrittenOpening(raw,partial,List.of(note(true))));
    }
}
