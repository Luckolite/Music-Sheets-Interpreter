// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original oval counters and semantic masks; no commercial score pixels. */
public class RoundedMeterGlyphTest {
    private static class Page {
        final int w=400,h=240;byte[] gray=new byte[w*h],labels=new byte[w*h];
        Page(boolean shallow) {
            Arrays.fill(gray,(byte)255);
            for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++) {gray[y*w+x]=0;labels[y*w+x]=4;}
            for(int y=50;y<=170;y++)for(int x=40;x<=75;x++)labels[y*w+x]=3;
            for(int cy:new int[]{100,120,137})for(int y=cy-11;y<=cy+11;y++)for(int x=118;x<=142;x++)
                if(Math.pow((x-130)/12.0,2)+Math.pow((y-cy)/11.0,2)<=1)labels[y*w+x]=2;
            for(int cy:new int[]{104,120,136})for(int y=cy-9;y<=cy+9;y++)for(int x=120;x<=140;x++) {
                double outer=Math.pow((x-130)/10.0,2)+Math.pow((y-cy)/9.0,2);
                double inner=Math.pow((x-130)/(shallow?7.5:5.0),2)+Math.pow((y-cy)/(shallow?3.0:5.5),2);
                if(outer<=1)gray[y*w+x]=inner<1?(byte)255:0;
            }
            for(int y=81;y<=97;y++)gray[y*w+125]=0;
        }
        Object make(String type,Object... values)throws Exception {
            var c=Class.forName("io.github.luckolite.interpreter.OmrScoreInterpreter$"+type).getDeclaredConstructors()[0];
            c.setAccessible(true);return c.newInstance(values);
        }
        boolean classify(boolean clef)throws Exception {
            Object head=make("Component",1100,119,141,89,145,130f,117f);
            Object staff=make("Staff",80f,144f,16f);
            Object glyph=make("Component",900,40,75,50,170,57f,110f);
            var method=Arrays.stream(OmrScoreInterpreter.class.getDeclaredMethods())
                    .filter(m->m.getName().equals("isRoundedHeaderMeter")).findFirst().orElseThrow();
            method.setAccessible(true);
            return (boolean)method.invoke(null,labels,gray,w,h,head,List.of(staff),clef?List.of(glyph):List.of());
        }
        List<ScoreNoteEvent> notes() {
            return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.15f,.8f)));
        }
    }
    @Test public void uprightNumeralCountersAreRecognizedBeforeChordSplitting()throws Exception {
        assertTrue(new Page(false).classify(true));
    }
    @Test public void shallowHollowChordCountersAreProtected()throws Exception {
        assertFalse(new Page(true).classify(true));
    }
    @Test public void noClefContextMeansTheGlyphIsNotRemoved()throws Exception {
        assertFalse(new Page(false).classify(false));
    }
    @Test public void aRealStemBeyondTheStaveProtectsAnOverlappingChord()throws Exception {
        Page p=new Page(false);for(int y=45;y<=146;y++)p.gray[y*p.w+141]=0;
        assertFalse(p.classify(true));
    }
    @Test public void aMergedSignatureDoesNotProduceThreeNotes() {
        assertTrue(new Page(false).notes().isEmpty());
    }
    @Test public void theRealNoteAfterTheSignatureRemains() {
        Page p=new Page(false);
        for(int y=120;y<=136;y++)for(int x=290;x<=310;x++)
            if(Math.pow((x-300)/10.0,2)+Math.pow((y-128)/8.0,2)<=1) {
                p.gray[y*p.w+x]=0;p.labels[y*p.w+x]=2;
            }
        for(int y=80;y<=128;y++) {p.gray[y*p.w+310]=0;p.labels[y*p.w+310]=1;}
        var notes=p.notes();assertEquals(1,notes.size());assertEquals(2,notes.get(0).staffStep());
    }
}
