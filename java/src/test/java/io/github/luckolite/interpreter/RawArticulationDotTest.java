// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated dots with incomplete semantic masks and complete raw ink. */
public final class RawArticulationDotTest {
    private static final int W=320,H=480;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    private void ink(int x,int y,int label) { gray[y*W+x]=0;labels[y*W+x]=(byte)label; }
    private void page(boolean upperHalf,boolean stem,boolean oval) {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++)ink(x,y,4);
        for(int y=99;y<=109;y++)for(int x=82;x<=98;x++)
            if(Math.pow((x-90)/8d,2)+Math.pow((y-104)/5d,2)<=1)ink(x,y,2);
        for(int y=104;y<=139;y++)ink(82,y,1);
        for(int y=80;y<=92;y++)for(int x=84;x<=96;x++) {
            if(Math.pow((x-90)/(oval?6d:3.2),2)+Math.pow((y-86)/3.2,2)>1)continue;
            gray[y*W+x]=0;
            boolean selected=upperHalf?y<=86:y>=86;
            if(selected&&Math.abs(x-90)<=3)labels[y*W+x]=2;
        }
        if(stem)for(int y=86;y<=121;y++)ink(87,y,1);
    }
    private List<ScoreNoteEvent> notes(boolean raw) {
        return OmrScoreInterpreter.analyze(labels,raw?gray:null,W,H,List.of(new MeasureRegion(.05f,.95f,.05f,.6f))).notes();
    }
    @Test public void lowerHalfOfRoundDotCannotBecomeANote() {
        page(false,false,false);var result=notes(true);assertEquals(1,result.size());
        assertTrue((result.get(0).articulations()&NoteArticulation.STACCATO)!=0);
    }
    @Test public void upperHalfOfRoundDotCannotBecomeANote() {
        page(true,false,false);assertEquals(1,notes(true).size());
    }
    @Test public void incompleteMaskAloneCannotProveARoundDot() {
        page(false,false,false);assertEquals(2,notes(false).size());
    }
    @Test public void completeRawOvalIsNotReclassifiedAsRound() {
        page(false,false,true);assertEquals(2,notes(true).size());
    }
    @Test public void connectedRawInkDoesNotQualifyAsIsolatedDot() {
        page(false,false,false);for(int x=90;x<125;x++)gray[83*W+x]=0;
        assertEquals(2,notes(true).size());
    }
    @Test public void roundHeadWithAnActualStemIsPreserved() {
        page(false,true,false);assertEquals(2,notes(true).size());
    }
}
