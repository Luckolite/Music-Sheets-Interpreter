// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff rules and dots with variable ink strength. */
public class FadedRuleDotTest {
    static final int W=320,H=220;
    final byte[] gray=new byte[W*H];
    public FadedRuleDotTest(){Arrays.fill(gray,(byte)255);}
    void rules(int count){for(int line=0;line<count;line++)for(int x=20;x<300;x++)gray[(80+16*line)*W+x]=(byte)228;}
    void dot(int x,int y,int radius){for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)if(dx*dx+dy*dy<=radius*radius)gray[(y+dy)*W+x+dx]=0;}
    void fleck(int y){for(int x=127;x<=129;x++)for(int row=y;row<=y+1;row++)gray[row*W+x]=100;}
    int dots(int headY)throws Exception {
        Class<?> type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        Object head=ctor.newInstance(150,92,108,headY-6,headY+6,100f,(float)headY);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("countAugmentationDots",List.class,type,float.class,byte[].class,int.class,int.class,boolean.class);m.setAccessible(true);
        return (Integer)m.invoke(null,List.of(),head,16f,gray,W,H,false);
    }
    @Test public void fadedRuleFleckDoesNotLengthenSpaceNote()throws Exception{rules(5);fleck(112);assertEquals(0,dots(104));}
    @Test public void darkRuleFleckDoesNotLengthenSpaceNote()throws Exception{rules(5);for(int line=0;line<5;line++)for(int x=20;x<300;x++)gray[(80+16*line)*W+x]=(byte)150;fleck(112);assertEquals(0,dots(104));}
    @Test public void smallActualDotInSpaceIsKept()throws Exception{rules(5);fleck(104);assertEquals(1,dots(104));}
    @Test public void fullRoundDotCrossingRuleIsKept()throws Exception{rules(5);dot(128,112,3);assertEquals(1,dots(104));}
    @Test public void isolatedSmallDotWithoutStaffEvidenceIsKept()throws Exception{fleck(112);assertEquals(1,dots(104));}
    @Test public void twoRulesCannotEstablishStaffPhase()throws Exception{rules(2);fleck(96);assertEquals(1,dots(104));}
    @Test public void inputPixelsAreUnchanged()throws Exception{rules(5);fleck(112);byte[] before=gray.clone();dots(104);assertArrayEquals(before,gray);}
}
