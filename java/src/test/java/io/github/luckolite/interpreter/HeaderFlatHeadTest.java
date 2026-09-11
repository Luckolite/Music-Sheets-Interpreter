// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original split flat drawings with independent raw and semantic ink. */
public final class HeaderFlatHeadTest {
    static final int W=420,H=260;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(0,1,.1f,.9f));
    static class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];
        Page(boolean clef) {
            Arrays.fill(gray,(byte)255);
            for(int y=100;y<=164;y+=16)for(int x=10;x<410;x++)pixel(x,y,4);
            if(clef)for(int y=68;y<=190;y++)for(int x=25;x<=51;x++)
                if(x<28||x>48||y<71||y>187)pixel(x,y,3);
            note(280,140,false,false);
        }
        void pixel(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void flat(int offset,boolean spineLabel) {
            for(int y=115;y<=153;y++)for(int x=82;x<=84;x++)pixel(x+offset,y,spineLabel?3:5);
            for(int x=83;x<=96;x++) {
                double t=(x-83)/13d;
                int upper=(int)Math.round(134+7*t*t),lower=(int)Math.round(153-12*t);
                for(int y:new int[]{upper-1,upper,upper+1,lower-1,lower,lower+1})
                    pixel(x+offset,y,x>=89&&y>=139?2:5);
            }
        }
        void note(int cx,int cy,boolean down,boolean hollow) {
            for(int y=cy-6;y<=cy+6;y++)for(int x=cx-8;x<=cx+8;x++) {
                double d=Math.pow((x-cx)/8d,2)+Math.pow((y-cy)/6d,2);
                if(d<=1){pixel(x,y,2);if(hollow&&d<.4)gray[y*W+x]=(byte)255;}
            }
            for(int y=down?cy:cy-48;y<=(down?cy+48:cy);y++)pixel(cx+(down?-8:8),y,3);
        }
        int count(byte[] mask){int count=0;for(int x=80;x<=102;x++)for(int y=130;y<=156;y++)if(mask[y*W+x]==2)count++;return count;}
        byte[] normalized(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,W,H,M);}
        boolean headAt(int x){return OmrScoreInterpreter.analyze(labels,gray,W,H,M).notes().stream().anyMatch(n->Math.abs(n.positionInMeasure()*W-x)<8);}
    }
    @Test public void splitKeyFlatDoesNotBecomeANote(){var p=new Page(true);p.flat(0,true);assertFalse(p.headAt(92));}
    @Test public void headerNormalizationRemovesOnlyTheFalseBowl(){var p=new Page(true);p.flat(0,true);assertTrue(p.count(p.labels)>0);assertEquals(0,p.count(p.normalized()));assertTrue(p.headAt(280));}
    @Test public void aNearbyClefIsRequired(){var p=new Page(false);p.flat(0,true);assertTrue(p.headAt(92));}
    @Test public void accidentalSpineEvidenceIsRequired(){var p=new Page(true);p.flat(0,false);assertTrue(p.headAt(92));}
    @Test public void distantInkIsNotAKeyHeader(){var p=new Page(true);p.flat(150,true);assertTrue(p.headAt(242));}
    @Test public void actualUpStemHeadSurvives(){var p=new Page(true);p.note(92,140,false,false);assertTrue(p.headAt(92));}
    @Test public void actualDownStemHeadSurvives(){var p=new Page(true);p.note(92,140,true,false);assertTrue(p.headAt(92));}
    @Test public void hollowHeadSurvives(){var p=new Page(true);p.note(92,140,false,true);assertTrue(p.headAt(92));}
    @Test public void sourceMasksAndRawInkAreUnchanged(){var p=new Page(true);p.flat(0,true);var labels=p.labels.clone();var gray=p.gray.clone();p.normalized();p.headAt(92);assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);}
    @Test public void normalizationIsIdempotent(){var p=new Page(true);p.flat(0,true);var once=p.normalized();assertArrayEquals(once,OmrScoreInterpreter.normalizeHeaderSymbols(once,p.gray,W,H,M));}
}
