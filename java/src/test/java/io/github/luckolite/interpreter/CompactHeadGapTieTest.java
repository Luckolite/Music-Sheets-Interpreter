// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ties at the one-pixel boundary between inclusive and exclusive head gaps. */
public class CompactHeadGapTieTest {
    private Class<?> nested(String name) {
        return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals(name)).findFirst().orElseThrow();
    }
    private Object note(int x, int step, float position, float scale) throws Exception {
        var event = new ScoreNoteEvent(0, position, step, 0, 1, .5f, false, 0, 1, 2, 0);
        var headConstructor = nested("Component").getDeclaredConstructors()[0];
        headConstructor.setAccessible(true);
        var head = headConstructor.newInstance(400, Math.round((x-14)*scale), Math.round((x+14)*scale),
                Math.round(70*scale), Math.round(90*scale), x*scale, 80*scale);
        var constructor = nested("DetectedNote").getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(event, head, 17*scale);
    }
    private boolean detect(int mode, int side, int step, float scale) throws Exception {
        int w=Math.round(500*scale), h=Math.round(170*scale);
        byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int x=0;x<w;x++) for(int y=Math.round((80+side*17)*scale)-1;y<=Math.round((80+side*17)*scale)+1;y++) {
            gray[y*w+x]=0;labels[y*w+x]=4;
        }
        for(int x=Math.round(106*scale);x<=Math.round(144*scale);x++) {
            float t=(x/scale-106)/38f;
            if(mode==0 || mode==2 && t>.5f)continue;
            float curve=mode==3?7*t:7*4*t*(1-t);
            int yy=Math.round((80+side*(20+curve))*scale);
            for(int y=yy-1;y<=yy+1;y++){gray[y*w+x]=0;if(labels[y*w+x]!=4)labels[y*w+x]=5;}
        }
        var mark=OmrScoreInterpreter.class.getDeclaredMethod("markTieContinuations",
                byte[].class,byte[].class,int.class,int.class,List.class);
        mark.setAccessible(true);
        var result=(List<?>)mark.invoke(null,labels,gray,w,h,List.of(note(100,0,.2f,scale),note(151,step,.3f,scale)));
        var getter=nested("DetectedNote").getDeclaredMethod("event");getter.setAccessible(true);
        return ((ScoreNoteEvent)getter.invoke(result.get(1))).tiedFromPrevious();
    }
    @Test public void lowerTieRetainsOverlappingShoulders()throws Exception {assertTrue(detect(1,1,0,1));}
    @Test public void upperTieRetainsOverlappingShoulders()throws Exception {assertTrue(detect(1,-1,0,1));}
    @Test public void scaledTieRetainsOverlappingShoulders()throws Exception {assertTrue(detect(1,1,0,1.5f));}
    @Test public void straightRuleDoesNotJoinRepeatedNotes()throws Exception {assertFalse(detect(0,1,0,1));}
    @Test public void incompleteCurveDoesNotJoinRepeatedNotes()throws Exception {assertFalse(detect(2,1,0,1));}
    @Test public void slopingStrokeDoesNotJoinRepeatedNotes()throws Exception {assertFalse(detect(3,1,0,1));}
    @Test public void slurToAnotherPitchIsNotATie()throws Exception {assertFalse(detect(1,1,1,1));}
}
