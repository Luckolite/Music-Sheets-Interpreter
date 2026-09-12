// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original analytic ovals with isolated semantic contour noise. */
public class StackedHeadContourTest {
    static final int W=360,H=210;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public StackedHeadContourTest(){Arrays.fill(gray,(byte)255);for(int y=80;y<=144;y+=16)for(int x=20;x<340;x++){labels[y*W+x]=4;gray[y*W+x]=0;}}
    void stack(){for(int cy:new int[]{100,116,132})for(int y=cy-8;y<=cy+8;y++)for(int x=190;x<=210;x++)if((x-200)*(x-200)/100d+(y-cy)*(y-cy)/64d<=1){labels[y*W+x]=2;gray[y*W+x]=0;}for(int y=64;y<=132;y++){gray[y*W+210]=0;if(labels[y*W+210]!=2)labels[y*W+210]=1;}}
    void semanticWidth(int y,int width){for(int x=190;x<=210;x++)labels[y*W+x]=0;for(int x=200-width/2;x<200-width/2+width;x++)labels[y*W+x]=2;}
    List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(100f/W,335f/W,50f/H,180f/H)));}
    void assertTriad(){var notes=notes();assertEquals(3,notes.size());var ys=notes.stream().map(n->n.pageY()*H).sorted().toList();for(int i=0;i<3;i++)assertEquals(100+16*i,ys.get(i),2f);}
    @Test public void ordinaryThreeHeadChordRemainsReadable(){stack();assertTriad();}
    @Test public void isolatedLowerContourBandDoesNotDiscardChord(){stack();semanticWidth(138,16);semanticWidth(139,18);assertTriad();}
    @Test public void isolatedUpperContourBandDoesNotDiscardChord(){stack();semanticWidth(93,18);semanticWidth(94,16);assertTriad();}
    @Test public void isolatedBandsAreNotAcceptedAsRegularLobes() throws Exception {
        for(int y=92;y<=140;y++)semanticWidth(y,5);
        for(int y:new int[]{100,116,132})semanticWidth(y,18);
        var owner=OmrScoreInterpreter.class;
        var components=owner.getDeclaredMethod("findComponents",byte[].class,int.class,int.class,byte.class);
        components.setAccessible(true);
        var all=(List<?>)components.invoke(null,labels,W,H,(byte)2);
        assertEquals(1,all.size());
        var staffType=Class.forName(owner.getName()+"$Staff");
        var ctor=staffType.getDeclaredConstructor(float.class,float.class,float.class);ctor.setAccessible(true);
        var split=owner.getDeclaredMethod("splitRegularStack",byte[].class,int.class,all.get(0).getClass(),staffType);split.setAccessible(true);
        assertTrue(((List<?>)split.invoke(null,labels,W,all.get(0),ctor.newInstance(80f,144f,16f))).isEmpty());
    }
    @Test public void contourAnalysisDoesNotModifyInputs(){stack();semanticWidth(139,18);var l=labels.clone();var g=gray.clone();notes();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
