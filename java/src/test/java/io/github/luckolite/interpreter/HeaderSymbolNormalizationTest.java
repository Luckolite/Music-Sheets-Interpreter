// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric C signs, noteheads and multimeasure rest bars. */
public class HeaderSymbolNormalizationTest {
    static class Page {
        final int w=480,h=240;final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(boolean cut,boolean clef) {
            Arrays.fill(gray,(byte)255);
            staff();
            if(clef)for(int y=48;y<=172;y++)for(int x=35;x<=58;x++){labels[y*w+x]=3;gray[y*w+x]=0;}
            for(int y=88;y<=136;y++)for(int x=110;x<=142;x++) {
                double outer=Math.pow((x-126)/16.0,2)+Math.pow((y-112)/24.0,2);
                double inner=Math.pow((x-126)/10.0,2)+Math.pow((y-112)/18.0,2);
                if(outer<=1&&inner>=1&&!(x>126&&Math.abs(y-112)<9))gray[y*w+x]=0;
            }
            for(int y=96;y<=102;y++)for(int x=135;x<=141;x++)if((x-138)*(x-138)+(y-99)*(y-99)<=9){gray[y*w+x]=0;labels[y*w+x]=2;}
            if(cut)for(int y=84;y<=140;y++)for(int x=125;x<=127;x++)gray[y*w+x]=0;
        }
        void staff(){for(int y=80;y<=144;y+=16)for(int x=20;x<460;x++){gray[y*w+x]=0;labels[y*w+x]=4;}}
        void eraseSign(){for(int y=75;y<=148;y++)for(int x=105;x<=148;x++){gray[y*w+x]=(byte)255;labels[y*w+x]=0;}staff();}
        void note(int x,int y,int rx,int ry,boolean hollow){for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++){double d=Math.pow((xx-x)/(double)rx,2)+Math.pow((yy-y)/(double)ry,2);if(d<=1){labels[yy*w+xx]=2;gray[yy*w+xx]=hollow&&d<.45?(byte)255:0;}}}
        List<MeasureRegion> measures(){return OmrMeasurePostProcessor.process(labels,gray,w,h);}
        byte[] normalized(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,w,h,measures());}
        int heads(byte[] data){int n=0;for(byte b:data)if(b==2)n++;return n;}
        void restAndNextNote(){
            for(int x=180;x<=315;x++)for(int y=109;y<=115;y++){gray[y*w+x]=0;labels[y*w+x]=1;}
            for(int x:new int[]{180,181,314,315})for(int y=96;y<=128;y++){gray[y*w+x]=0;labels[y*w+x]=1;}
            for(int y=80;y<=144;y++)for(int x=340;x<=341;x++){gray[y*w+x]=0;labels[y*w+x]=1;}
            note(410,112,10,7,false);for(int y=64;y<=112;y++){gray[y*w+420]=0;labels[y*w+420]=1;}
        }
        MeasureNumberReconciler.NumberToken count(){return new MeasureNumberReconciler.NumberToken(8,240f/w,58f/h,255f/w,75f/h);}
    }
    @Test public void commonTimeTerminalIsNotANote(){Page p=new Page(false,true);assertTrue(p.heads(p.labels)>0);assertEquals(0,p.heads(p.normalized()));}
    @Test public void cutTimeKeepsTheSourceMasksAndInkUntouched(){Page p=new Page(true,true);byte[] before=p.labels.clone(),ink=p.gray.clone();assertEquals(0,p.heads(p.normalized()));assertArrayEquals(before,p.labels);assertArrayEquals(ink,p.gray);}
    @Test public void headerEvidenceIsRequired(){Page p=new Page(true,false);assertArrayEquals(p.labels,p.normalized());}
    @Test public void anActualFilledHeadIsPreserved(){Page p=new Page(false,true);p.eraseSign();p.note(130,112,10,7,false);assertArrayEquals(p.labels,p.normalized());}
    @Test public void hollowChordHeadsArePreserved(){Page p=new Page(false,true);p.eraseSign();p.note(130,104,10,7,true);p.note(130,120,10,7,true);assertArrayEquals(p.labels,p.normalized());}
    @Test public void aGraceNoteStemProtectsItsSmallHead(){Page p=new Page(false,true);p.eraseSign();p.note(130,112,5,3,false);for(int y=65;y<=112;y++)p.gray[y*p.w+135]=0;assertArrayEquals(p.labels,p.normalized());}
    @Test public void aClosedOvalIsNotAnOpenTimeSign(){Page p=new Page(false,true);for(int y=101;y<=123;y++)for(int x=137;x<=142;x++)p.gray[y*p.w+x]=0;assertArrayEquals(p.labels,p.normalized());}
    @Test public void normalizationIsIdempotent(){Page p=new Page(true,true);byte[] once=p.normalized();assertArrayEquals(once,OmrScoreInterpreter.normalizeHeaderSymbols(once,p.gray,p.w,p.h,p.measures()));}
    @Test public void clearingNumeralHeadsCannotCreateABarline(){
        Page p=new Page(false,true);p.eraseSign();
        // A numeral's vertical stroke can span the stave near a mislabelled bowl.
        for(int y=80;y<=144;y++)for(int x=250;x<=251;x++){p.gray[y*p.w+x]=0;p.labels[y*p.w+x]=5;}
        p.note(250,120,9,6,false);
        byte[] headerLabels=p.labels.clone();
        for(int i=0;i<headerLabels.length;i++)if(headerLabels[i]==2)headerLabels[i]=0;
        var original=p.measures();
        assertTrue(OmrMeasurePostProcessor.process(headerLabels,p.gray,p.w,p.h).size()>original.size());
        assertEquals(original,OmrMeasurePostProcessor.process(p.labels,p.gray,p.w,p.h,headerLabels));
    }
    @Test public void aFalseHeaderHeadCannotBlockAnEightBarRest(){
        Page p=new Page(true,true);p.restAndNextNote();var regions=p.measures();byte[] clean=p.normalized();
        var rests=MultiMeasureRestDetector.detect(clean,p.gray,p.w,p.h,regions,List.of(p.count()));assertEquals(1,rests.size());
        var expanded=MeasureNumberReconciler.reconcile(regions,List.of(),rests);var notes=OmrScoreInterpreter.extract(clean,p.gray,p.w,p.h,expanded);
        assertEquals(9,expanded.size());assertEquals(1,notes.size());assertEquals(8,notes.get(0).measureIndex());assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,notes.get(0).writtenAccidental());
    }
}
