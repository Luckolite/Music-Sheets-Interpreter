// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric numeral strokes; no score images or trained masks. */
public class RepeatedMeterFragmentTest {
    private static class Page {
        final int scale,w,h;final byte[] labels,gray;
        Page(int scale,boolean bar,boolean upper) {
            this.scale=scale;w=400*scale;h=240*scale;labels=new byte[w*h];gray=new byte[w*h];Arrays.fill(gray,(byte)255);
            for(int y=80;y<=144;y+=16)rect(20,y,380,y,4);
            if(bar)rect(90,80,91,144,1);
            if(upper)four(80);
            four(112);
            rect(138,132,142,136,2);
        }
        void rect(int x1,int y1,int x2,int y2,int label) {
            for(int y=y1*scale;y<(y2+1)*scale;y++)for(int x=x1*scale;x<(x2+1)*scale;x++){gray[y*w+x]=0;labels[y*w+x]=(byte)label;}
        }
        void four(int top) {
            rect(140,top,143,top+30,5);rect(121,top+20,146,top+23,5);
            for(int dy=0;dy<=20;dy++)rect(139-dy,top+dy,142-dy,top+dy,5);
        }
        Object make(String name,Object... args)throws Exception {
            var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);
        }
        boolean classify()throws Exception {
            var method=Arrays.stream(OmrScoreInterpreter.class.getDeclaredMethods()).filter(m->m.getName().equals("isHeaderMeterDigit")).findFirst().orElseThrow();method.setAccessible(true);
            Object head=make("Component",25*scale*scale,138*scale,143*scale-1,132*scale,137*scale-1,140f*scale,134f*scale);
            Object staff=make("Staff",80f*scale,144f*scale,16f*scale);
            return (boolean)method.invoke(null,labels,gray,w,h,head,List.of(staff),List.of());
        }
    }
    @Test public void aSmallCornerOfRepeatedMeterDigitsIsRejected()throws Exception {assertTrue(new Page(1,true,true).classify());}
    @Test public void recognitionScalesWithStaffSpacing()throws Exception {assertTrue(new Page(2,true,true).classify());}
    @Test public void matchingInkWithoutABarlineIsInsufficient()throws Exception {assertFalse(new Page(1,false,true).classify());}
    @Test public void anIsolatedLowerDigitIsInsufficient()throws Exception {assertFalse(new Page(1,true,false).classify());}
    @Test public void anUpperNotePredictionProtectsAChord()throws Exception {
        Page p=new Page(1,true,true);p.rect(133,96,142,102,2);assertFalse(p.classify());
    }
    @Test public void anAttachedStemOutsideTheStaffProtectsANote()throws Exception {
        Page p=new Page(1,true,true);p.rect(142,60,143,136,1);assertFalse(p.classify());
    }
    @Test public void aShortVerticalStrokeIsNotABarline()throws Exception {
        Page p=new Page(1,false,true);p.rect(90,104,91,144,1);assertFalse(p.classify());
    }
    @Test public void recognitionDoesNotAlterPixelsOrLabels()throws Exception {
        Page p=new Page(1,true,true);byte[] labels=p.labels.clone(),gray=p.gray.clone();assertTrue(p.classify());assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);
    }
}
