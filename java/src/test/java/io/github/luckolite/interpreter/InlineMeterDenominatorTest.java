// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-digit numerator strokes and rounded denominator masks. */
public class InlineMeterDenominatorTest {
    private static class Page {
        final int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(boolean barline,boolean numerator) {
            Arrays.fill(gray,(byte)255);
            for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++) {gray[y*w+x]=0;labels[y*w+x]=4;}
            if(barline)for(int y=80;y<=144;y++)for(int x=90;x<=91;x++) {gray[y*w+x]=0;labels[y*w+x]=1;}
            if(numerator) {
                for(int y=81;y<=105;y++)for(int x=109;x<=111;x++)symbol(x,y);
                for(int x=126;x<=141;x++) {symbol(x,81);symbol(x,82);symbol(x,105);symbol(x,106);}
                for(int y=83;y<=104;y++)for(int dx=0;dx<3;dx++)symbol(140-(y-83)*13/21+dx,y);
            }
            for(int cy:new int[]{120,136})for(int y=cy-11;y<=cy+11;y++)for(int x=118;x<=142;x++) {
                if(Math.pow((x-130)/12.0,2)+Math.pow((y-cy)/11.0,2)<=1)labels[y*w+x]=2;
                double outer=Math.pow((x-130)/10.0,2)+Math.pow((y-cy)/8.0,2);
                double inner=Math.pow((x-130)/5.0,2)+Math.pow((y-cy)/5.5,2);
                if(outer<=1)gray[y*w+x]=inner<1?(byte)255:0;
            }
        }
        void symbol(int x,int y) {gray[y*w+x]=0;labels[y*w+x]=5;}
        Object make(String type,Object... args)throws Exception {
            var c=Class.forName("io.github.luckolite.interpreter.OmrScoreInterpreter$"+type).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);
        }
        boolean classify(boolean clef)throws Exception {return classify(clef,80f);}
        boolean classify(boolean clef,float top)throws Exception {
            var method=Arrays.stream(OmrScoreInterpreter.class.getDeclaredMethods()).filter(m->m.getName().equals("isRoundedHeaderMeter")).findFirst().orElseThrow();method.setAccessible(true);
            Object head=make("Component",750,118,142,109,147,130f,128f),staff=make("Staff",top,top+64,16f),glyph=make("Component",900,40,75,50,170,57f,110f);
            return (boolean)method.invoke(null,labels,gray,w,h,head,List.of(staff),clef?List.of(glyph):List.of());
        }
        List<ScoreNoteEvent> notes() {
            return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.15f,.8f)));
        }
    }
    @Test public void inlineDenominatorIsRemovedBeforeItsBowlsBecomeNotes() {
        assertTrue(new Page(true,true).notes().isEmpty());
    }
    @Test public void headerDenominatorCanUseTheClefContext()throws Exception {
        assertTrue(new Page(false,true).classify(true));
    }
    @Test public void withoutClefOrBarlineTheShapeIsPreserved()throws Exception {
        assertFalse(new Page(false,true).classify(false));
    }
    @Test public void anActualNoteAboveProtectsTheLowerChord()throws Exception {
        Page p=new Page(true,true);
        for(int y=90;y<=98;y++)for(int x=123;x<=136;x++)p.labels[y*p.w+x]=2;
        assertFalse(p.classify(false));
    }
    @Test public void aDenominatorNeedsThePrintedUpperNumeral()throws Exception {
        assertFalse(new Page(true,false).classify(false));
    }
    @Test public void aStemBeyondTheStaveProtectsARealChord()throws Exception {
        Page p=new Page(true,true);for(int y=109;y<=190;y++)p.gray[y*p.w+118]=0;
        assertFalse(p.classify(false));
    }
    @Test public void aRealNoteAfterTheSignatureRemains() {
        Page p=new Page(true,true);
        for(int y=112;y<=128;y++)for(int x=290;x<=310;x++)
            if(Math.pow((x-300)/10.0,2)+Math.pow((y-120)/8.0,2)<=1) {p.gray[y*p.w+x]=0;p.labels[y*p.w+x]=2;}
        for(int y=72;y<=120;y++) {p.gray[y*p.w+310]=0;p.labels[y*p.w+310]=1;}
        var notes=p.notes();assertEquals(1,notes.size());assertEquals(3,notes.get(0).staffStep());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,notes.get(0).writtenAccidental());
    }
    @Test public void inclusivePixelExtentCanReachAFractionalStaffBoundary()throws Exception {
        assertTrue(new Page(false,true).classify(true,81.25f));
    }
    @Test public void aHeadClearlyAboveTheDenominatorBandIsPreserved()throws Exception {
        assertFalse(new Page(false,true).classify(true,83f));
    }
    private static void reshapeCounters(Page p,double rx,double ry) {
        for(int cy:new int[]{120,136})for(int y=cy-8;y<=cy+8;y++)for(int x=120;x<=140;x++) {
            double outer=Math.pow((x-130)/10.0,2)+Math.pow((y-cy)/8.0,2);
            if(outer<=1)p.gray[y*p.w+x]=Math.pow((x-130)/rx,2)+Math.pow((y-cy)/ry,2)<1?(byte)255:0;
        }
    }
    @Test public void aWideBowledEightStillNeedsItsNumerator()throws Exception {
        Page p=new Page(false,true);reshapeCounters(p,7,5);assertTrue(p.classify(true));
        Page inline=new Page(true,true);reshapeCounters(inline,7,5);assertTrue(inline.notes().isEmpty());
        Page absent=new Page(false,false);reshapeCounters(absent,7,5);assertFalse(absent.classify(true));
    }
    @Test public void shallowHollowChordCountersRemainProtected()throws Exception {
        Page p=new Page(false,true);reshapeCounters(p,8,3.5);assertFalse(p.classify(true));
    }
}
