// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original short paired-beam geometry with independently stemmed controls. */
public class ShortPairedMergedBeamHeadTest {
    static final int W=300,H=250,G=16;
    final byte[] gray=new byte[W*H];
    public ShortPairedMergedBeamHeadTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=110;y<=174;y+=16)for(int x=15;x<285;x++)gray[y*W+x]=0;
        for(int x=125;x<=175;x++) {
            int top=Math.round(99+(x-125)*.14f);
            for(int y=top;y<=top+22;y++)gray[y*W+x]=0;
            if(x>=145)for(int y=top+11;y<=top+12;y++)gray[y*W+x]=(byte)255;
        }
        note(117,159);note(167,159);
        for(int y=100;y<=159;y++){gray[y*W+125]=0;gray[(y+7)*W+175]=0;}
    }
    void note(int x,int y){for(int yy=y-9;yy<=y+9;yy++)for(int xx=x-11;xx<=x+11;xx++)
        if(Math.pow((xx-x)/11d,2)+Math.pow((yy-y)/9d,2)<=1)gray[yy*W+xx]=0;}
    int rejected(boolean twoOwners)throws Exception {
        var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=component.getDeclaredConstructors()[0];cc.setAccessible(true);
        var left=cc.newInstance(310,106,128,150,168,117f,159f);
        var right=cc.newInstance(310,156,178,150,168,167f,159f);
        var fragment=cc.newInstance(84,144,156,107,116,150f,111f);
        var staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staff.getDeclaredConstructors()[0];sc.setAccessible(true);
        var line=sc.newInstance(110f,174f,16f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("shortPairedMergedBeamHeads",byte[].class,
                int.class,int.class,List.class,List.class);method.setAccessible(true);
        return ((List<?>)method.invoke(null,gray,W,H,twoOwners?List.of(left,right,fragment):List.of(left,fragment),List.of(line))).size();
    }
    @Test public void twoRealStemsOwnSmallIslandOnShortMergedBeams()throws Exception {assertEquals(1,rejected(true));}
    @Test public void oneRealStemIsInsufficient()throws Exception {assertEquals(0,rejected(false));}
    @Test public void thinSingleBeamIsInsufficient()throws Exception {
        for(int x=126;x<175;x++)for(int y=98;y<=132;y++)if(y>Math.round(99+(x-125)*.14f)+7)
            gray[y*W+x]=(byte)255;
        assertEquals(0,rejected(true));
    }
    @Test public void protrudingOvalProtectsARealNote()throws Exception {
        for(int y=96;y<=130;y++)for(int x=140;x<=160;x++)
            if(Math.pow((x-150)/10d,2)+Math.pow((y-113)/17d,2)<=1)gray[y*W+x]=0;
        assertEquals(0,rejected(true));
    }
}
