// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic grand staff, not a transcription of a private score. */
public class TimingSessionTest {
    static List<ScoreNoteEvent> page() {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int bar=0;bar<3;bar++)for(int staff=0;staff<2;staff++)for(int i=0;i<8;i++)
            notes.add(new ScoreNoteEvent(bar,.07f+i*.1f+staff*.008f,i%7,staff,2,
                    .2f+staff*.1f,false,0,1,0,0));
        return notes;
    }
    @Test public void batchedAndUnbatchedTimingsMatchAndScopeDoesNotRetainChangedInput() {
        var notes=page();double[] expected=new double[notes.size()];
        for(int i=0;i<notes.size();i++)expected[i]=ScoreNoteTiming.beatInMeasure(notes.get(i),notes,4);
        try(var session=ScoreNoteTiming.beginTimingSession()) {
            for(int i=0;i<notes.size();i++)assertEquals(expected[i],ScoreNoteTiming.beatInMeasure(notes.get(i),notes,4),0);
            try(var nested=ScoreNoteTiming.beginTimingSession()) {
                assertEquals(expected[0],ScoreNoteTiming.beatInMeasure(notes.get(0),notes,4),0);
            }
            assertEquals(expected[1],ScoreNoteTiming.beatInMeasure(notes.get(1),notes,4),0);
        }
        notes.remove(0);
        assertEquals(ScoreNoteTiming.beatInMeasure(notes.get(0),new ArrayList<>(notes),4),
                ScoreNoteTiming.beatInMeasure(notes.get(0),notes,4),0);
    }
    @Test public void repeatedResolutionReusesVoiceWorkWithinTheBatch() {
        var source=page();int[] reads={0};
        List<ScoreNoteEvent> notes=new AbstractList<>() {
            public int size(){return source.size();}
            public ScoreNoteEvent get(int i){reads[0]++;return source.get(i);}
        };
        try(var session=ScoreNoteTiming.beginTimingSession()) {
            for(var n:source)ScoreNoteTiming.beatInMeasure(n,notes,4);
            int first=reads[0];reads[0]=0;
            for(var n:source)ScoreNoteTiming.beatInMeasure(n,notes,4);
            assertTrue("Repeated resolution should avoid rebuilding voice clocks",reads[0]<first);
        }
    }
}
