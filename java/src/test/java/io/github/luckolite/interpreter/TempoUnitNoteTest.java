// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric note-equals-text marks and ordinary high-note counterexamples. */
public class TempoUnitNoteTest {
    static final class Page {
        final int w=500,h=260;
        final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page() {
            Arrays.fill(gray,(byte)255);
            for(int y=100;y<=164;y+=16)rect(20,y,460,1,(byte)4);
            note(110,89,false);rect(120,45,1,45,(byte)1);
            rect(131,73,19,3,(byte)5);rect(131,80,19,3,(byte)5);
            digit();
        }
        void rect(int x,int y,int width,int height,byte label) {
            for(int yy=y;yy<y+height;yy++)for(int xx=x;xx<x+width;xx++) {
                labels[yy*w+xx]=label;gray[yy*w+xx]=0;
            }
        }
        void erase(int x,int y,int width,int height) {
            for(int yy=y;yy<y+height;yy++)for(int xx=x;xx<x+width;xx++) {
                labels[yy*w+xx]=0;gray[yy*w+xx]=(byte)255;
            }
        }
        void digit() {
            rect(164,62,3,27,(byte)5);rect(164,62,12,3,(byte)5);
            rect(164,74,12,3,(byte)5);rect(164,86,12,3,(byte)5);rect(173,74,3,15,(byte)5);
        }
        void note(int x,int y,boolean hollow) {
            for(int yy=y-7;yy<=y+7;yy++)for(int xx=x-10;xx<=x+10;xx++) {
                double d=Math.pow((xx-x)/10.0,2)+Math.pow((yy-y)/7.0,2);
                if(d<=1){labels[yy*w+xx]=2;gray[yy*w+xx]=hollow&&d<.4?(byte)255:0;}
            }
        }
        void realNote(){note(300,132,false);rect(310,84,1,49,(byte)1);}
        List<MeasureRegion> measures(){return OmrMeasurePostProcessor.process(labels,gray,w,h);}
        byte[] normalized(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,w,h,measures());}
        int heads(byte[] data){int n=0;for(byte value:data)if(value==2)n++;return n;}
    }

    @Test public void quarterBeatUnitIsNotASoundingNote(){
        Page p=new Page();assertTrue(p.heads(p.labels)>0);assertEquals(0,p.heads(p.normalized()));
    }
    @Test public void hollowBeatUnitUsesTheSamePrintedEquationEvidence(){
        Page p=new Page();p.note(110,89,true);assertEquals(0,p.heads(p.normalized()));
    }
    @Test public void preparationPreservesInputsAndTheRealNote(){
        Page p=new Page();p.realNote();byte[] original=p.labels.clone(),ink=p.gray.clone();byte[] clean=p.normalized();
        assertArrayEquals(original,p.labels);assertArrayEquals(ink,p.gray);
        assertEquals(0,clean[89*p.w+110]);assertEquals(2,clean[132*p.w+300]);
        assertArrayEquals(clean,OmrScoreInterpreter.normalizeHeaderSymbols(clean,p.gray,p.w,p.h,p.measures()));
    }
    @Test public void directExtractionAlsoExcludesTheBeatUnit(){
        Page p=new Page();p.realNote();
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,p.w,p.h,p.measures());
        assertEquals(1,notes.size());assertEquals(4,notes.get(0).staffStep());
    }
    @Test public void highNotesWithoutAnEqualsSignArePreserved(){
        Page p=new Page();p.erase(130,72,21,12);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void oneHorizontalStrokeIsInsufficient(){
        Page p=new Page();p.erase(131,80,19,3);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void ledgerSpacedStrokesAreNotAnEqualsSign(){
        Page p=new Page();p.erase(130,72,21,12);p.rect(131,64,19,3,(byte)5);p.rect(131,80,19,3,(byte)5);
        assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void anEqualsSignWithoutFollowingTextIsInsufficient(){
        Page p=new Page();p.erase(162,60,16,31);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void aClippedTextComponentCannotQualify(){
        Page p=new Page();p.rect(164,54,3,10,(byte)5);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void aNoteInsideTheStaffIsPreserved(){
        Page p=new Page();p.erase(99,44,23,53);p.note(110,108,false);p.rect(120,64,1,45,(byte)1);
        assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void aStemlessHighNoteIsPreserved(){
        Page p=new Page();p.erase(120,44,2,44);assertArrayEquals(p.labels,p.normalized());
    }
}
