// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WholeNoteMeterOcrTest {
    private List<ScoreNoteEvent> bars(int beats) {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int m=0;m<2;m++)for(int b=0;b<beats;b++)notes.add(new ScoreNoteEvent(m,
                .05f+b*.9f/beats,4,0,1,.5f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1));
        return notes;
    }
    private List<ScoreMeterChange> filtered(int n,int d,int beats) {
        return MeterChangeDetector.filterWholeNoteOcrReadings(List.of(new ScoreMeterChange(0,n,d)),bars(beats),2);
    }
    @Test public void repeatedTwoBeatBarsRejectSpuriousTwoOverOne() {assertTrue(filtered(2,1,2).isEmpty());}
    @Test public void repeatedSixBeatBarsRejectSpuriousOneOverOne() {assertTrue(filtered(1,1,6).isEmpty());}
    @Test public void realWholeNoteDenominatorIsPreserved() {assertEquals(1,filtered(2,1,8).size());assertEquals(1,filtered(1,1,4).size());}
    @Test public void uncertainCompoundRhythmCannotEraseItsMeter() {assertEquals(1,filtered(12,8,8).size());}
    @Test public void emptyNotationCannotDisproveAMeter() {
        var input=List.of(new ScoreMeterChange(0,2,1));
        assertEquals(input,MeterChangeDetector.filterWholeNoteOcrReadings(input,List.of(),2));
    }
    @Test public void oneBarIsInsufficientToRejectAReading() {
        var input=List.of(new ScoreMeterChange(0,2,1));
        assertEquals(input,MeterChangeDetector.filterWholeNoteOcrReadings(input,bars(2),1));
    }
}
