// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original stacked glyphs on strong, faded and absent printed staff rules. */
public class FaintMeterStaffTest {
    private static final int W=600,H=440,X=240;

    @Test public void fadedStaffStillOffersItsSignatureToOcr() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);digits(gray,80);
        assertTrue(RawStaffLineDetector.detect(gray,W,H).isEmpty());
        var crops=MeterChangeDetector.candidates(labels,gray,W,H);
        assertEquals(1,crops.size());assertTrue(crops.get(0).left()<X&&crops.get(0).right()>X+18);
    }

    @Test public void fadedLaterStaffIsRecoveredBesideAStrongOne() {
        byte[] labels=new byte[W*H],gray=paper();
        staff(labels,gray,60,80);digits(gray,60);staff(labels,gray,270,200);digits(gray,270);
        assertEquals(1,RawStaffLineDetector.detect(gray,W,H).size());
        var crops=MeterChangeDetector.candidates(labels,gray,W,H);
        assertEquals(2,crops.size());assertTrue(crops.get(0).top()<200&&crops.get(1).top()>200);
    }

    @Test public void semanticRulesWithoutPrintedSupportCannotInventASignature() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,255);digits(gray,80);
        assertTrue(MeterChangeDetector.candidates(labels,gray,W,H).isEmpty());
    }

    @Test public void actualNotesRetainTheirSemanticHeadVetoOnFadedStaffs() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);digits(gray,80);
        for(int y=88;y<102;y++)for(int x=X;x<X+18;x++)labels[y*W+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertTrue(MeterChangeDetector.candidates(labels,gray,W,H).isEmpty());
    }

    @Test public void staggeredKeyGlyphsCannotBecomeAMeter() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);
        flat(labels,gray,X+12,76);flat(labels,gray,X,100);
        assertTrue(MeterChangeDetector.candidates(labels,gray,W,H).isEmpty());
    }

    @Test public void stackedDigitsMisclassifiedAsKeyInkStillReachOcr() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);digits(gray,80);
        for(int y=80;y<=144;y++)for(int x=X;x<X+19;x++)
            if((gray[y*W+x]&255)<100)labels[y*W+x]=OmrMeasurePostProcessor.CLEF_OR_KEY;
        assertEquals(1,MeterChangeDetector.candidates(labels,gray,W,H).size());
    }

    @Test public void barlineBesideUnknownDigitsIsNotARhythmicSlash() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);digits(gray,80);
        for(int y=80;y<=144;y++)for(int x=X-4;x<X-1;x++) {
            gray[y*W+x]=30;labels[y*W+x]=OmrMeasurePostProcessor.STEM_OR_REST;
        }
        assertEquals(1,MeterChangeDetector.candidates(labels,gray,W,H).size());
    }

    private static void flat(byte[] labels,byte[] gray,int x,int top) {
        for(int y=0;y<40;y++)for(int dx=0;dx<11;dx++) {
            boolean stem=dx<2;
            boolean bulb=y>=21&&y<39&&dx<=10-(y-21)/2
                    &&(y<24||dx>=7-(y-21)/2||y>=36);
            if(stem||bulb){int at=(top+y)*W+x+dx;gray[at]=30;labels[at]=OmrMeasurePostProcessor.CLEF_OR_KEY;}
        }
    }

    @Test public void rhythmicSlashCannotBecomeOneOverFour() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);
        for(int y=80;y<=144;y++)for(int x=X+7;x<=X+9;x++) {
            labels[y*W+x]=OmrMeasurePostProcessor.STEM_OR_REST;gray[y*W+x]=30;
        }
        for(int x=X;x<X+19;x++)for(int d=-1;d<=1;d++) {
            int y=108-(x-X)/2+d;labels[y*W+x]=OmrMeasurePostProcessor.SYMBOL;gray[y*W+x]=30;
        }
        assertTrue(MeterChangeDetector.candidates(labels,gray,W,H).isEmpty());
    }

    @Test public void partialKeyMisclassificationDoesNotVetoSupportedDigits() {
        byte[] labels=new byte[W*H],gray=paper();staff(labels,gray,80,200);digits(gray,80);
        for(int y=80;y<=144;y++)for(int x=X;x<X+19;x++)if((gray[y*W+x]&255)<100)
            labels[y*W+x]=(x%4==0)?OmrMeasurePostProcessor.CLEF_OR_KEY:OmrMeasurePostProcessor.SYMBOL;
        assertEquals(1,MeterChangeDetector.candidates(labels,gray,W,H).size());
    }

    @Test public void tiltedFadedStaffRetainsTheSourceCropCoordinates() { tilted(.035f); }
    @Test public void oppositeTiltRetainsTheSourceCropCoordinates() { tilted(-.035f); }

    private static void tilted(float slope) {
        byte[] original=new byte[W*H],paper=paper();staff(original,paper,80,200);digits(paper,80);
        byte[] labels=new byte[W*H],gray=paper();
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int yy=y+Math.round(slope*(x-W*.5f));
            if(yy>=0&&yy<H){labels[yy*W+x]=original[y*W+x];gray[yy*W+x]=paper[y*W+x];}
        }
        var crops=MeterChangeDetector.candidates(labels,gray,W,H);
        assertEquals(1,crops.size());var crop=crops.get(0);
        int shift=Math.round(slope*(X+9-W*.5f));
        assertEquals(80+shift,crop.firstLine(),1);
        assertTrue(crop.top()<82+shift&&crop.bottom()>140+shift);
    }

    private static byte[] paper(){byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);return gray;}
    private static void staff(byte[] labels,byte[] gray,int top,int shade) {
        for(int line=0;line<5;line++)for(int x=50;x<550;x++) {
            int at=(top+line*16)*W+x;labels[at]=OmrMeasurePostProcessor.STAFF;gray[at]=(byte)shade;
        }
    }
    private static void digits(byte[] gray,int top) {
        for(int base:new int[]{top+2,top+35})for(int y=0;y<28;y++)for(int x=0;x<19;x++)
            if(x>=15||y<4||y>=12&&y<16||y>=24)gray[(base+y)*W+X+x]=40;
    }
}
