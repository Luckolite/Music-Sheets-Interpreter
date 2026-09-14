// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic notation: compact ties with clear space beside each head. */
public class CompactTieShoulderTest {
    private Class<?> nested(String name) {
        return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals(name)).findFirst().orElseThrow();
    }
    private Object note(int x, int step, float position, float scale) throws Exception {
        var event=new ScoreNoteEvent(0,position,step,0,1,.5f,false,0,1,2,0);
        var hc=nested("Component").getDeclaredConstructors()[0];hc.setAccessible(true);
        var head=hc.newInstance(400,Math.round((x-12)*scale),Math.round((x+12)*scale),
                Math.round(70*scale),Math.round(90*scale),x*scale,80*scale);
        var nc=nested("DetectedNote").getDeclaredConstructors()[0];nc.setAccessible(true);
        return nc.newInstance(event,head,17*scale);
    }
    private boolean detect(int mode,int side,int step,float scale) throws Exception {
        int w=Math.round(400*scale),h=Math.round(180*scale);
        byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int x=0;x<w;x++)for(int y=Math.round(80*scale)-1;y<=Math.round(80*scale)+1;y++) {
            labels[y*w+x]=4;gray[y*w+x]=0;
        }
        for(int x=Math.round(119*scale);x<=Math.round(139*scale);x++) {
            float t=(x/scale-119)/20f;
            if(mode==0 || mode==3&&t>.5f || mode==5&&t>.3f&&t<.7f)continue;
            float bend=mode==4?7*t:7*4*t*(1-t);
            int yy=Math.round((80+side*(8+bend))*scale);
            int ink=mode==6?185:mode==2&&(t<.18f||t>.82f)?180:0;
            for(int y=yy-1;y<=yy+1;y++){gray[y*w+x]=(byte)ink;labels[y*w+x]=5;}
        }
        var mark=OmrScoreInterpreter.class.getDeclaredMethod("markTieContinuations",
                byte[].class,byte[].class,int.class,int.class,List.class);mark.setAccessible(true);
        var result=(List<?>)mark.invoke(null,labels,gray,w,h,List.of(note(100,0,.2f,scale),note(155,step,.3f,scale)));
        var getter=nested("DetectedNote").getDeclaredMethod("event");getter.setAccessible(true);
        return ((ScoreNoteEvent)getter.invoke(result.get(1))).tiedFromPrevious();
    }
    @Test public void upperCompactTie()throws Exception{assertTrue(detect(1,-1,0,1));}
    @Test public void lowerCompactTie()throws Exception{assertTrue(detect(1,1,0,1));}
    @Test public void compactTieWithFaintShoulders()throws Exception{assertTrue(detect(2,1,0,1));}
    @Test public void scaledCompactTie()throws Exception{assertTrue(detect(1,1,0,1.5f));}
    @Test public void staffRuleIsNotATie()throws Exception{assertFalse(detect(0,1,0,1));}
    @Test public void incompleteCurveIsNotATie()throws Exception{assertFalse(detect(3,1,0,1));}
    @Test public void slopingStrokeIsNotATie()throws Exception{assertFalse(detect(4,1,0,1));}
    @Test public void disconnectedFragmentsAreNotATie()throws Exception{assertFalse(detect(5,1,0,1));}
    @Test public void faintCurveWithoutDarkCoreIsNotATie()throws Exception{assertFalse(detect(6,1,0,1));}
    @Test public void differentPitchIsNotATie()throws Exception{assertFalse(detect(1,1,1,1));}
}
