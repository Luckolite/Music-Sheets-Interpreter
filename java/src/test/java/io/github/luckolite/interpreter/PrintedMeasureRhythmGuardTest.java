// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrintedMeasureRhythmGuardTest {
    private final List<MeasureRegion> raw = List.of(new MeasureRegion(.1f,.3f,.2f,.6f),
            new MeasureRegion(.31f,.5f,.2f,.6f),new MeasureRegion(.51f,.9f,.2f,.6f));
    private final List<MeasureRegion> fitted = List.of(raw.get(0),raw.get(1),
            new MeasureRegion(.51f,.703f,.2f,.6f),new MeasureRegion(.707f,.9f,.2f,.6f));
    private byte[] paper() { byte[] g=new byte[1000*800];Arrays.fill(g,(byte)255);return g; }
    private List<ScoreNoteEvent> notes(int runBeams, boolean bass, int neighborBeams) {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int m=0;m<3;m++) {
            int n=m==2?16:8;
            for(int i=0;i<n;i++)notes.add(new ScoreNoteEvent(m,.03f+i*.94f/n,i%7,0,2,.3f,
                    false,0,m==2?runBeams:neighborBeams,2,0));
            if(bass)for(int i=0;i<2;i++)notes.add(new ScoreNoteEvent(m,.03f+i*.5f,0,1,2,.5f,
                    false,0,0,2,2));
        }
        return notes;
    }
    @Test public void completeDenseMeasureSurvivesIncorrectPrintedCount() {
        assertEquals(raw,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(2,true,1),paper(),1000,800));
    }
    @Test public void genuinelyMergedEightBeatRunStillSplits() {
        assertEquals(fitted,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(1,true,1),paper(),1000,800));
    }
    @Test public void missingAccompanimentCannotOverruleNumbering() {
        assertEquals(fitted,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(2,false,1),paper(),1000,800));
    }
    @Test public void differentRhythmOnNeighboringBarsIsNotEnoughEvidence() {
        assertEquals(fitted,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(2,true,2).stream()
                .filter(n->n.measureIndex()==2||n.staffIndex()==0).toList(),paper(),1000,800));
    }
    @Test public void visibleBarlineAlwaysSurvivesEvenWhenRhythmIsUnderread() {
        byte[] g=paper();for(int y=160;y<=480;y++)g[y*1000+705]=0;
        assertEquals(fitted,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(2,true,1),g,1000,800));
    }
    @Test public void missingPixelEvidenceDoesNotSuppressASeparator() {
        assertEquals(fitted,PrintedMeasureRhythmGuard.preserveCompleteRuns(raw,fitted,notes(2,true,1),null,1000,800));
    }
    @Test public void inferredMidpointMovesToTheVisibleGrandStaffBarline() {
        byte[] g=paper();for(int y=160;y<=480;y++)g[y*1000+630]=0;
        var aligned=PrintedMeasureRhythmGuard.alignPrintedSeparators(raw,fitted,notes(1,true,1),g,1000,800);
        assertEquals(.628f,aligned.get(2).right(),.0001f);
        assertEquals(.632f,aligned.get(3).left(),.0001f);
        assertEquals(raw.get(0),aligned.get(0));
    }
    @Test public void singleStaffStemCannotMoveAnInferredBoundary() {
        byte[] g=paper();for(int y=160;y<=480;y++)g[y*1000+630]=0;
        assertEquals(fitted,PrintedMeasureRhythmGuard.alignPrintedSeparators(raw,fitted,notes(1,false,1),g,1000,800));
    }
}
