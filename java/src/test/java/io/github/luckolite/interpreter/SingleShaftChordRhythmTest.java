// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original four-oval chord; rhythm disagreement is injected independently. */
public class SingleShaftChordRhythmTest {
    static final int W=240,H=220,G=16;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    final List<Object> notes=new ArrayList<>();
    final Class<?> component,detected;
    public SingleShaftChordRhythmTest()throws Exception {
        Arrays.fill(gray,(byte)255);
        component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        detected=Class.forName(OmrScoreInterpreter.class.getName()+"$DetectedNote");
        var hc=component.getDeclaredConstructors()[0];hc.setAccessible(true);
        var nc=detected.getDeclaredConstructors()[0];nc.setAccessible(true);
        for(int i=0;i<4;i++) {
            int cy=90+i*G;
            for(int y=cy-7;y<=cy+7;y++)for(int x=90;x<=110;x++)
                if(Math.pow((x-100)/10d,2)+Math.pow((y-cy)/7d,2)<=1){labels[y*W+x]=2;gray[y*W+x]=0;}
            Object head=hc.newInstance(190,90,110,cy-7,cy+7,100f,(float)cy);
            var e=new ScoreNoteEvent(2,.4f,6-i*2,1,3,cy/(float)H,false,1,i==2?2:1,1,0f);
            notes.add(nc.newInstance(e,head,(float)G));
        }
        shaft(110,35,145);
    }
    void shaft(int x,int top,int bottom){for(int y=top;y<=bottom;y++){labels[y*W+x]=1;gray[y*W+x]=0;}}
    List<ScoreNoteEvent> run()throws Exception{
        var m=OmrScoreInterpreter.class.getDeclaredMethod("reconcileSingleShaftChords",List.class,byte[].class,byte[].class,int.class,int.class);m.setAccessible(true);
        var values=(List<?>)m.invoke(null,notes,labels,gray,W,H);
        var e=detected.getDeclaredMethod("event");e.setAccessible(true);
        var result=new ArrayList<ScoreNoteEvent>();for(Object n:values)result.add((ScoreNoteEvent)e.invoke(n));return result;
    }
    @Test public void oneFilledChordSharesMajorityBeamCount()throws Exception{assertTrue(run().stream().allMatch(n->n.beamCount()==1));}
    @Test public void independentOpposingShaftsStaySeparate()throws Exception{shaft(90,83,198);assertEquals(2,run().get(2).beamCount());}
    @Test public void missingCommonShaftPreventsCorrection()throws Exception{
        for(int y=35;y<=145;y++){labels[y*W+110]=0;gray[y*W+110]=(byte)255;}
        assertEquals(2,run().get(2).beamCount());
    }
    @Test public void aShaftEndsAtTheOuterOvalCentreNotItsRoundedTip()throws Exception {
        notes.remove(3);
        for(int y=123;y<=145;y++)gray[y*W+110]=(byte)255;
        assertEquals(1,run().get(2).beamCount());
    }
    @Test public void correctionPreservesPitchDotsAndAccidental()throws Exception{
        var n=run().get(2);assertEquals(2,n.staffStep());assertEquals(1,n.augmentationDots());assertEquals(1,n.writtenAccidental());assertEquals(2,n.measureIndex());
    }
    @Test public void callerPixelsAreUnchanged()throws Exception{var a=labels.clone();var b=gray.clone();run();assertArrayEquals(a,labels);assertArrayEquals(b,gray);}
}
