// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShortMeterTieTest {
    private Class<?> nested(String name) {
        return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals(name)).findFirst().orElseThrow();
    }
    private Object detected(int bar,float position,int step,int x,int dots,int beams)throws Exception {
        var event=new ScoreNoteEvent(bar,position,step,0,1,.5f,false,dots,beams,2,beams==0?1:0);
        var component=nested("Component").getDeclaredConstructors()[0];component.setAccessible(true);
        var head=component.newInstance(80,x-8,x+8,76,84,(float)x,80f);
        var note=nested("DetectedNote").getDeclaredConstructors()[0];note.setAccessible(true);
        return note.newInstance(event,head,14f);
    }
    private boolean tied(int dots,int beams,boolean arc,int step,boolean intervening)throws Exception {
        int w=500,h=150;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        if(arc)for(int x=109;x<=231;x++) {
            float t=(x-109)/122f;int y=Math.round(87+10*4*t*(1-t));
            for(int yy=y-1;yy<=y+1;yy++){gray[yy*w+x]=0;labels[yy*w+x]=5;}
        }
        var notes=new ArrayList<Object>();notes.add(detected(0,.10f,0,100,dots,beams));
        if(intervening)notes.add(detected(0,.70f,2,180,0,1));
        notes.add(detected(1,.10f,step,240,0,0));
        var mark=OmrScoreInterpreter.class.getDeclaredMethod("markTieContinuations",byte[].class,byte[].class,int.class,int.class,List.class);mark.setAccessible(true);
        var result=(List<?>)mark.invoke(null,labels,gray,w,h,notes);
        var getter=nested("DetectedNote").getDeclaredMethod("event");getter.setAccessible(true);
        return ((ScoreNoteEvent)getter.invoke(result.get(result.size()-1))).tiedFromPrevious();
    }
    @Test public void dottedQuarterAtBarStartCanContinueAcrossBarline()throws Exception {assertTrue(tied(1,0,true,0,false));}
    @Test public void quarterAtBarStartCanContinueAcrossBarline()throws Exception {assertTrue(tied(0,0,true,0,false));}
    @Test public void shortBeamedValueAtBarStartCanContinueAcrossBarline()throws Exception {assertTrue(tied(0,1,true,0,false));}
    @Test public void repeatedPitchWithoutArcStillAttacksAgain()throws Exception {assertFalse(tied(1,0,false,0,false));}
    @Test public void slurToDifferentPitchIsNotTie()throws Exception {assertFalse(tied(1,0,true,1,false));}
    @Test public void interveningShortVoiceAttackPreventsTie()throws Exception {assertFalse(tied(1,0,true,0,true));}
}
