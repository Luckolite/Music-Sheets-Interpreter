// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic accidental drawings; no score scans or model masks. */
public class AccidentalFontRegressionTest {
    private Object construct(String name,Object...args)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        return constructor.newInstance(args);
    }
    private Object call(String name,Object...args)throws Exception {
        for(var method:OmrScoreInterpreter.class.getDeclaredMethods())
            if(method.getName().equals(name)&&method.getParameterCount()==args.length) {
                method.setAccessible(true);return method.invoke(null,args);
            }
        throw new IllegalArgumentException(name);
    }
    private static class Drawing {
        final int w=96,h=96;
        final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Drawing(){Arrays.fill(gray,(byte)255);}
        void rect(int x,int y,int width,int height,byte label) {
            for(int yy=y;yy<y+height;yy++)for(int xx=x;xx<x+width;xx++) {
                labels[yy*w+xx]=label;gray[yy*w+xx]=0;
            }
        }
    }
    private Object component(Drawing d,int left,int top,int right,int bottom)throws Exception {
        int area=0;long sx=0,sy=0;
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)if(d.labels[y*d.w+x]!=0){area++;sx+=x;sy+=y;}
        return construct("Component",area,left,right,top,bottom,sx/(float)area,sy/(float)area);
    }
    private boolean sharp(Drawing d,int left,int top,int right,int bottom,float gap)throws Exception {
        return (boolean)call("isSharpGlyph",d.labels,d.w,d.h,
                construct("AccidentalCandidate",component(d,left,top,right,bottom),(byte)3),gap);
    }
    @Test public void narrowSharpWithLongSpinesRetainsBothCrossbars()throws Exception {
        Drawing d=new Drawing();
        d.rect(33,20,2,49,(byte)3);d.rect(40,20,2,49,(byte)3);
        d.rect(30,33,14,5,(byte)3);d.rect(30,50,14,5,(byte)3);
        assertTrue(sharp(d,30,20,43,68,17.5f));
    }
    @Test public void neighboringInkCannotMakeSharpCrossbarsTooNarrow()throws Exception {
        Drawing d=new Drawing();
        d.rect(37,20,2,43,(byte)3);d.rect(45,20,2,43,(byte)3);
        d.rect(35,33,14,4,(byte)3);d.rect(35,46,14,4,(byte)3);
        d.rect(30,48,23,1,(byte)3);
        assertTrue(sharp(d,30,20,52,62,14));
    }
    private boolean rawNatural(boolean natural)throws Exception {
        return rawNatural(natural,false);
    }
    private boolean rawNatural(boolean natural,boolean crossingRule)throws Exception {
        Drawing d=new Drawing();
        d.rect(25,18,2,natural?29:41,(byte)1);
        d.rect(35,natural?30:18,2,natural?29:41,(byte)1);
        d.rect(25,30,12,4,(byte)3);d.rect(25,44,12,4,(byte)5);
        if(crossingRule)d.rect(0,58,96,3,(byte)4);
        Object seed=construct("Component",96,25,36,30,47,30.5f,38.5f);
        Object head=construct("Component",100,48,60,33,43,54f,38f);
        return (boolean)call("rawNaturalAtSeed",d.gray,d.w,d.h,seed,head,14f);
    }
    @Test public void printedNaturalSpinesSurviveStemLabelSplits()throws Exception {
        assertTrue(rawNatural(true));
    }
    @Test public void parallelSharpSpinesCannotBecomeNatural()throws Exception {
        assertFalse(rawNatural(false));
    }
    @Test public void thickStaffRuleCannotExtendBothNaturalSpines()throws Exception {
        assertTrue(rawNatural(true,true));
    }

    @Test public void sharpUsesItsCrossbarCentreInsteadOfAnAdjacentChordHead()throws Exception {
        Drawing d=new Drawing();
        d.rect(37,20,2,43,(byte)3);d.rect(45,20,2,43,(byte)3);
        d.rect(35,33,14,4,(byte)3);d.rect(35,46,14,4,(byte)3);
        d.rect(30,28,23,1,(byte)3);
        Object glyph=construct("AccidentalCandidate",component(d,30,20,52,62),(byte)3);
        Object target=construct("Component",100,61,75,37,45,68f,41f);
        Object upper=construct("Component",100,61,75,23,31,68f,27f);
        assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,call("detectWrittenAccidental",
                d.labels,d.w,d.h,java.util.List.of(glyph),target,14f));
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,call("detectWrittenAccidental",
                d.labels,d.w,d.h,java.util.List.of(glyph),upper,14f));
    }
}
