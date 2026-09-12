// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloping staff rules with separately drawn real beam counterexamples. */
public class SlopedStaffBeamTest {
    static final int W=400,H=240,X=200;
    static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        final float slope;
        Page(float slope) {
            this.slope=slope;Arrays.fill(gray,(byte)240);
            for(int x=0;x<W;x++)for(int line=0;line<5;line++)for(int dy=-2;dy<=2;dy++) {
                int y=Math.round(90+line*16+slope*x)+dy;
                gray[y*W+x]=70;labels[y*W+x]=5;
            }
            // Rule masks can be thin even when the antialiased ink is five pixels thick.
            for(int x=0;x<W;x++)labels[Math.round(90+slope*x)*W+x]=4;
        }
        int top(){return Math.round(90+slope*X);}
        void beam(int center,int halfThickness) {
            for(int x=135;x<=265;x++)for(int y=center-halfThickness;y<=center+halfThickness;y++) {
                gray[y*W+x]=20;labels[y*W+x]=5;
            }
        }
        int count(int from,int to,boolean tracked)throws Exception {
            Class<?> staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
            var c=staff.getDeclaredConstructor(float.class,float.class,float.class);c.setAccessible(true);
            Object s=c.newInstance(90f,154f,16f);
            if(tracked) {
                var constructor=StaffPitchTrack.class.getDeclaredConstructor(List.class);constructor.setAccessible(true);
                Object track=constructor.newInstance(List.of(new float[]{0,154,16},new float[]{W,154+slope*W,16}));
                var field=staff.getDeclaredField("pitchTrack");field.setAccessible(true);field.set(s,track);
            }
            var method=OmrScoreInterpreter.class.getDeclaredMethod("thickNonHeadBands",byte[].class,byte[].class,int.class,int.class,int.class,int.class,int.class,staff);method.setAccessible(true);
            return (int)method.invoke(null,gray,labels,W,H,X,from,to,s);
        }
    }
    @Test public void risingRuleDoesNotShortenAQuarter()throws Exception {
        var p=new Page(-.10f);assertEquals(0,p.count(p.top()-6,p.top()+6,true));
    }
    @Test public void fallingRuleDoesNotShortenAQuarter()throws Exception {
        var p=new Page(.10f);assertEquals(0,p.count(p.top()-6,p.top()+6,true));
    }
    @Test public void thickRealBeamCrossingARuleIsRetained()throws Exception {
        var p=new Page(.10f);p.beam(p.top(),5);assertEquals(1,p.count(p.top()-7,p.top()+7,true));
    }
    @Test public void thinRealBeamBetweenRulesIsRetained()throws Exception {
        var p=new Page(-.10f);p.beam(p.top()+8,2);assertEquals(1,p.count(p.top()+5,p.top()+11,true));
    }
    @Test public void twoRealBeamsRemainTwo()throws Exception {
        var p=new Page(.10f);p.beam(p.top()-10,2);p.beam(p.top()+8,2);assertEquals(2,p.count(p.top()-14,p.top()+11,true));
    }
    @Test public void straightRuleKeepsExistingBehavior()throws Exception {
        var p=new Page(0);assertEquals(0,p.count(p.top()-6,p.top()+6,false));
    }
    @Test public void sourceArraysAreNotRewritten()throws Exception {
        var p=new Page(.10f);byte[] g=p.gray.clone(),l=p.labels.clone();p.count(p.top()-6,p.top()+6,true);
        assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);
    }
}
