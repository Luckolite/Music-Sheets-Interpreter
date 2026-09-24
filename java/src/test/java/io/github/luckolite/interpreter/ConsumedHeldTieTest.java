// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** A later same-pitch attack ends eligibility of an earlier held endpoint. */
public class ConsumedHeldTieTest {
    private Class<?> nested(String name){return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals(name)).findFirst().orElseThrow();}
    private Object note(int x,int pitch,float position,boolean held)throws Exception {
        var event=new ScoreNoteEvent(0,position,pitch,0,1,.5f,false,0,held?0:1,2,held?2:0);
        var hc=nested("Component").getDeclaredConstructors()[0];hc.setAccessible(true);
        var head=hc.newInstance(100,x-7,x+7,73,87,(float)x,80f);
        var nc=nested("DetectedNote").getDeclaredConstructors()[0];nc.setAccessible(true);
        return nc.newInstance(event,head,14f);
    }
    private int candidate(boolean repeated)throws Exception {
        var notes=new ArrayList<>();notes.add(note(100,0,.1f,true));
        if(repeated)notes.add(note(180,0,.4f,false));
        notes.add(note(240,2,.6f,false));notes.add(note(300,0,.8f,false));
        var m=OmrScoreInterpreter.class.getDeclaredMethod("previousSamePitch",List.class,int.class,int.class);m.setAccessible(true);
        return (int)m.invoke(null,notes,notes.size()-1,1000);
    }
    @Test public void consumedHeldNoteCannotBridgeLaterMelody()throws Exception{assertEquals(-1,candidate(true));}
    @Test public void otherVoiceDoesNotConsumeHeldPitch()throws Exception{assertEquals(0,candidate(false));}
}
