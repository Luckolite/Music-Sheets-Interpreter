// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curved flags, with separate stem roots for sixteenth flags. */
public class ReturningFlagCountTest {
    private static final int W=240,H=180;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private boolean down;
    private int y(int value){return down?160-value:value;}
    private void pixel(int x,int yy,byte label){gray[y(yy)*W+x]=0;labels[y(yy)*W+x]=label;}
    private void setup(boolean down) {
        this.down=down;Arrays.fill(gray,(byte)255);Arrays.fill(labels,(byte)0);
        for(int line=5;line<=85;line+=20)for(int yy=line;yy<line+2;yy++)for(int x=10;x<220;x++)pixel(x,yy,(byte)4);
        for(int yy=99;yy<=111;yy++)for(int x=80;x<=100;x++)
            if(Math.pow((x-90)/10.0,2)+Math.pow((yy-105)/6.0,2)<=1)pixel(x,yy,(byte)2);
        for(int yy=50;yy<=105;yy++)for(int x=99;x<=101;x++)pixel(x,yy,(byte)1);
    }
    private void hook(int dy) {
        int[] xs={100,110,117,120,119,112,109,110,115,110,104,100};
        int[] ys={50,59,64,72,80,90,91,77,73,69,65,62};
        for(int yy=49;yy<=92;yy++)for(int x=100;x<=121;x++) {
            boolean inside=false;
            for(int i=0,j=xs.length-1;i<xs.length;j=i++)
                if((ys[i]>yy)!=(ys[j]>yy)&&x<(xs[j]-xs[i])*(yy-ys[i])/(double)(ys[j]-ys[i])+xs[i])inside=!inside;
            if(inside)pixel(x,yy+dy,(byte)5);
        }
    }
    private int beams()throws Exception {
        var headType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var hc=headType.getDeclaredConstructors()[0];hc.setAccessible(true);
        var head=hc.newInstance(180,80,100,down?49:99,down?61:111,90f,(float)y(105));
        var staffType=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=staffType.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);
        var staff=sc.newInstance(down?75f:5f,down?155f:85f,20f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,headType,staffType);m.setAccessible(true);
        return (int)m.invoke(null,labels,gray,W,H,head,staff);
    }
    @Test public void oneReturningUpStemFlagCountsOnce()throws Exception {setup(false);hook(0);assertEquals(1,beams());}
    @Test public void oneReturningDownStemFlagCountsOnce()throws Exception {setup(true);hook(0);assertEquals(1,beams());}
    @Test public void twoUpStemFlagRootsRemainSixteenths()throws Exception {setup(false);hook(0);hook(17);assertEquals(2,beams());}
    @Test public void twoDownStemFlagRootsRemainSixteenths()throws Exception {setup(true);hook(0);hook(17);assertEquals(2,beams());}
    @Test public void anUnflaggedQuarterRemainsUnflagged()throws Exception {setup(false);assertEquals(0,beams());}
    @Test public void twoLongBeamsRemainSixteenths()throws Exception {setup(false);for(int start:new int[]{50,68})for(int yy=start;yy<start+8;yy++)for(int x=100;x<185;x++)pixel(x,yy,(byte)1);assertEquals(2,beams());}
    @Test public void oneLongBeamRemainsAnEighth()throws Exception {setup(false);for(int yy=50;yy<58;yy++)for(int x=100;x<185;x++)pixel(x,yy,(byte)1);assertEquals(1,beams());}
    @Test public void analysisPreservesPixelsAndLabels()throws Exception {setup(false);hook(0);var g=gray.clone();var l=labels.clone();beams();assertArrayEquals(g,gray);assertArrayEquals(l,labels);}
}
