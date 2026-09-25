// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Full-size chords use printed spacing, not a page-width percentage, for tie eligibility. */
public class CompactChordTieOnsetTest {
    private Class<?> nested(String n){return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals(n)).findFirst().orElseThrow();}
    private Object note(int measure,int x,int pitch,float position,int halfWidth)throws Exception {
        return note(measure,x,pitch,position,halfWidth,1);
    }
    private Object note(int measure,int x,int pitch,float position,int halfWidth,int staffCount)throws Exception {
        var event=new ScoreNoteEvent(measure,position,pitch,0,staffCount,.5f,false,0,1,2,0);
        var hc=nested("Component").getDeclaredConstructors()[0];hc.setAccessible(true);
        var head=hc.newInstance(150,x-halfWidth,x+halfWidth,73,87,(float)x,80f);
        var nc=nested("DetectedNote").getDeclaredConstructors()[0];nc.setAccessible(true);
        return nc.newInstance(event,head,20f);
    }
    private int prior(List<Object> notes)throws Exception {
        var m=OmrScoreInterpreter.class.getDeclaredMethod("previousSamePitch",List.class,int.class,int.class);m.setAccessible(true);
        return (int)m.invoke(null,notes,notes.size()-1,1000);
    }
    @Test public void narrowMeasureDoesNotSplitOffsetChordOnset()throws Exception {
        assertEquals(0,prior(List.of(note(0,100,3,.85f,10),note(0,110,0,.883f,10),note(1,200,3,.05f,10))));
    }
    @Test public void separateLaterAttackStillBlocksOlderMovingVoice()throws Exception {
        assertEquals(-1,prior(List.of(note(0,100,3,.75f,10),note(0,132,0,.883f,10),note(1,200,3,.05f,10))));
    }
    @Test public void graceWidthCannotWidenChordOnset()throws Exception {
        assertEquals(-1,prior(List.of(note(0,100,3,.85f,10),note(0,110,0,.883f,3),note(1,200,3,.05f,10))));
    }
    @Test public void overlappingHeadFragmentStillBelongsToFullSizeChord()throws Exception {
        assertEquals(0,prior(List.of(note(0,100,3,.85f,4),note(0,108,0,.883f,10),note(1,200,3,.05f,10))));
    }
    @Test public void samePitchReattackRemainsTheNearestEndpoint()throws Exception {
        assertEquals(1,prior(List.of(note(0,100,3,.85f,10),note(0,112,3,.89f,10),note(1,200,3,.05f,10))));
    }
    @Test public void differentMeasuresNeverBecomeAChordByPixelCloseness()throws Exception {
        assertEquals(-1,prior(List.of(note(0,100,3,.85f,10),note(1,110,0,.883f,10),note(2,200,3,.05f,10))));
    }
    @Test public void distinctStaffTopologyDoesNotWidenTheOnset()throws Exception {
        var a=note(0,100,3,.85f,10,1);var b=note(0,110,0,.883f,10,2);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("samePrintedOnset",a.getClass(),a.getClass());m.setAccessible(true);
        assertFalse((boolean)m.invoke(null,a,b));
    }
}
