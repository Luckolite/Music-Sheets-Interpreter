// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original lettering and notation geometry; no score image fixtures. */
public class PrintedTextHeadTest {
    private static final class Page {
        final int w=500,h=300;
        final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(){Arrays.fill(gray,(byte)255);for(int y=100;y<=164;y+=16)rect(20,y,460,1,4);}
        void rect(int x,int y,int w1,int h1,int label){for(int yy=y;yy<y+h1;yy++)for(int xx=x;xx<x+w1;xx++){gray[yy*w+xx]=0;labels[yy*w+xx]=(byte)label;}}
        void white(int x,int y,int w1,int h1){for(int yy=y;yy<y+h1;yy++)for(int xx=x;xx<x+w1;xx++)gray[yy*w+xx]=(byte)255;}
        void erase(int x,int y,int w1,int h1){white(x,y,w1,h1);for(int yy=y;yy<y+h1;yy++)for(int xx=x;xx<x+w1;xx++)labels[yy*w+xx]=0;}
        void head(int x,int y,int rx,int ry,boolean hollow){for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++){double d=Math.pow((xx-x)/(double)rx,2)+Math.pow((yy-y)/(double)ry,2);if(d<=1){labels[yy*w+xx]=2;gray[yy*w+xx]=hollow&&d<.45?(byte)255:0;}}}
        byte[] clean(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,w,h,OmrMeasurePostProcessor.process(labels,gray,w,h));}
        byte[] geometry(){return OmrScoreInterpreter.normalizeTextGeometry(labels,gray,w,h,OmrMeasurePostProcessor.process(labels,gray,w,h));}
        void eight(){rect(210,190,18,30,5);white(216,192,6,9);white(216,209,6,9);for(int y=190;y<204;y++)for(int x=210;x<228;x++)labels[y*w+x]=2;}
        void detached(){head(90,87,5,4,false);rect(95,65,1,23,1);rect(104,195,20,3,5);rect(104,203,20,3,5);digit(136);digit(154);}
        void digit(int x){rect(x,184,3,25,5);rect(x,184,12,3,5);rect(x,195,12,3,5);rect(x+9,195,3,14,5);rect(x,206,12,3,5);}
        void lettering(){rect(170,150,3,14,5);rect(180,155,3,9,5);rect(180,150,3,3,5);for(int x:new int[]{190,202}){rect(x,155,3,9,5);rect(x,155,8,3,5);rect(x+5,155,3,9,5);}for(int y=155;y<165;y++)for(int x=190;x<199;x++)labels[y*w+x]=2;}
    }
    @Test public void uprightTwoCounterTextIsRemoved(){var p=new Page();p.eight();assertEquals(0,p.clean()[196*p.w+211]);}
    @Test public void textRemovalAlsoReleasesGeometryHeadVeto(){var p=new Page();p.eight();assertEquals(0,p.geometry()[196*p.w+211]);}
    @Test public void textCleanupPreservesBothInputs(){var p=new Page();p.eight();var labels=p.labels.clone();var gray=p.gray.clone();p.clean();p.geometry();assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);}
    @Test public void aSingleHollowNoteRemains(){var p=new Page();p.head(220,196,11,7,true);assertArrayEquals(p.labels,p.clean());}
    @Test public void twoHollowChordHeadsRemain(){var p=new Page();p.head(220,192,11,7,true);p.head(220,208,11,7,true);assertArrayEquals(p.labels,p.clean());}
    @Test public void stemmedHollowChordHeadsRemain(){var p=new Page();p.head(220,192,11,7,true);p.head(220,208,11,7,true);p.rect(231,149,1,60,1);assertArrayEquals(p.labels,p.clean());}
    @Test public void aStaffRuleThroughAWholeNoteRemains(){var p=new Page();p.head(220,132,12,8,true);p.rect(20,132,460,1,4);assertArrayEquals(p.labels,p.clean());}
    @Test public void anOpenCounterDoesNotProveText(){var p=new Page();p.eight();p.white(210,195,7,2);assertArrayEquals(p.labels,p.clean());}
    @Test public void disconnectedTempoEquationIsRecognized(){var p=new Page();p.detached();assertEquals(0,p.clean()[87*p.w+90]);}
    @Test public void detachedBeatWithoutEqualsRemains(){var p=new Page();p.detached();p.erase(104,195,20,11);assertArrayEquals(p.labels,p.clean());}
    @Test public void detachedBeatWithOneEqualsStrokeRemains(){var p=new Page();p.detached();p.erase(104,203,20,3);assertArrayEquals(p.labels,p.clean());}
    @Test public void detachedBeatWithOnlyOneTextGlyphRemains(){var p=new Page();p.detached();p.erase(154,184,12,25);assertArrayEquals(p.labels,p.clean());}
    @Test public void detachedBeatWithMisalignedDigitsRemains(){var p=new Page();p.detached();p.erase(154,184,12,25);p.digit(190);assertArrayEquals(p.labels,p.clean());}
    @Test public void detachedBeatWithoutStemRemains(){var p=new Page();p.detached();p.erase(95,65,1,18);assertArrayEquals(p.labels,p.clean());}
    @Test public void normalSizedHighNoteWithDistantTextRemains(){var p=new Page();p.detached();p.head(90,87,10,7,false);assertArrayEquals(p.labels,p.clean());}
    @Test public void baselineLetteringWithDotAndAscenderIsRemoved(){var p=new Page();p.lettering();assertEquals(0,p.clean()[158*p.w+193]);}
    @Test public void letteringWithoutDotIsNotEnough(){var p=new Page();p.lettering();p.erase(180,150,3,3);assertArrayEquals(p.labels,p.clean());}
    @Test public void letteringWithoutAscenderIsNotEnough(){var p=new Page();p.lettering();p.erase(170,150,3,5);assertArrayEquals(p.labels,p.clean());}
    @Test public void aStemmedNoteAmongLettersRemains(){var p=new Page();p.lettering();p.rect(198,112,1,52,1);assertArrayEquals(p.labels,p.clean());}
    @Test public void dottedGraceNotesRemain(){var p=new Page();for(int x:new int[]{170,190,210}){p.head(x,158,5,4,false);p.rect(x+5,138,1,21,1);}p.rect(179,151,2,2,5);assertArrayEquals(p.labels,p.clean());}
}
