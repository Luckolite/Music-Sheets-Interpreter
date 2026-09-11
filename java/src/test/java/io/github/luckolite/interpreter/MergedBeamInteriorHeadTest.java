// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MergedBeamInteriorHeadTest {
    private final int w=320,h=260;
    private final byte[] gray=new byte[w*h];
    private void rect(int x,int y,int ww,int hh) {
        for(int yy=y;yy<y+hh;yy++)for(int xx=x;xx<x+ww;xx++)gray[yy*w+xx]=0;
    }
    private void ellipse(int x,int y,int rx,int ry) {
        for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++)
            if(Math.pow((xx-x)/(double)rx,2)+Math.pow((yy-y)/(double)ry,2)<=1)gray[yy*w+xx]=0;
    }
    private void page() {
        Arrays.fill(gray,(byte)255);ellipse(100,70,11,8);
        rect(89,70,3,59);rect(89,108,130,21);
    }
    private int rejected(boolean owner)throws Exception {
        var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=component.getDeclaredConstructors()[0];cc.setAccessible(true);
        var main=cc.newInstance(270,89,111,62,78,100f,70f);
        var fragment=cc.newInstance(49,88,98,114,120,94f,117f);
        var staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staff.getDeclaredConstructors()[0];sc.setAccessible(true);
        var line=sc.newInstance(80f,136f,14f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("mergedBeamInteriorHeads",byte[].class,
                int.class,int.class,List.class,List.class);method.setAccessible(true);
        return ((List<?>)method.invoke(null,gray,w,h,owner?List.of(main,fragment):List.of(fragment),List.of(line))).size();
    }
    @Test public void interiorIslandOnOwnedMergedBeamsIsRejected()throws Exception {
        page();assertEquals(1,rejected(true));
    }
    @Test public void aStripWithoutAnAttachedLargerHeadIsNotEnough()throws Exception {
        page();assertEquals(0,rejected(false));
    }
    @Test public void aRoundedChordHeadProtrudingFromTheStripIsPreserved()throws Exception {
        page();ellipse(100,117,11,14);assertEquals(0,rejected(true));
    }
    @Test public void aShortDarkMarkIsNotAnExtendedBeam() {
        Arrays.fill(gray,(byte)255);rect(90,108,22,21);
        assertFalse(OmrScoreInterpreter.mergedBeamStrip(gray,w,h,100,117,14));
    }
    @Test public void aThinStaffRuleIsNotAMergedBeam() {
        Arrays.fill(gray,(byte)255);rect(30,116,260,3);
        assertFalse(OmrScoreInterpreter.mergedBeamStrip(gray,w,h,100,117,14));
    }
    @Test public void aDisconnectedStemCannotOwnTheFragment()throws Exception {
        page();for(int y=85;y<100;y++)for(int x=85;x<96;x++)gray[y*w+x]=(byte)255;
        assertEquals(0,rejected(true));
    }
    @Test public void rejectedIslandsStopMaskingBeamCoresWithoutMutatingTheInput()throws Exception {
        var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=component.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var fragment=ctor.newInstance(49,88,98,114,120,94f,117f);
        byte[] labels=new byte[w*h];labels[117*w+94]=2;labels[70*w+100]=2;labels[118*w+94]=4;
        var method=OmrScoreInterpreter.class.getDeclaredMethod("withoutBeamHeadIslands",byte[].class,int.class,List.class);
        method.setAccessible(true);byte[] clean=(byte[])method.invoke(null,labels,w,List.of(fragment));
        assertEquals(2,labels[117*w+94]);assertEquals(0,clean[117*w+94]);
        assertEquals(2,clean[70*w+100]);assertEquals(4,clean[118*w+94]);
    }
}
