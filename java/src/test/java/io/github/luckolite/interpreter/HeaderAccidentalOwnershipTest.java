// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact key signatures and separate note accidentals. */
public class HeaderAccidentalOwnershipTest {
    static final int W=420,H=260;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(120f/W,1,.1f,.9f));
    static class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];
        Page(boolean clef) {this(clef,134);}
        Page(boolean clef,int firstHead) {
            Arrays.fill(gray,(byte)255);
            for(int y=100;y<=164;y+=16)for(int x=10;x<410;x++)ink(x,y,4);
            if(clef)for(int y=68;y<=190;y++)for(int x=25;x<=51;x++)
                if(x<28||x>48||y<71||y>187)ink(x,y,3);
            flat(76,132);flat(99,108);note(firstHead,100);note(280,132);
        }
        void ink(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
        void flat(int x,int cy) {
            for(int y=cy-28;y<=cy+7;y++)for(int xx=x;xx<=x+2;xx++)ink(xx,y,3);
            for(int xx=1;xx<=14;xx++) {
                double t=xx/14d;int upper=(int)Math.round(cy-11+7*t*t),lower=(int)Math.round(cy+7-14*t);
                for(int y:new int[]{upper-1,upper,upper+1,lower-1,lower,lower+1})ink(x+xx,y,3);
            }
        }
        void note(int cx,int cy) {
            for(int y=cy-6;y<=cy+6;y++)for(int x=cx-8;x<=cx+8;x++)
                if(Math.pow((x-cx)/8d,2)+Math.pow((y-cy)/6d,2)<=1)ink(x,y,2);
            for(int y=cy;y<=cy+48;y++)ink(cx-8,y,1);
        }
        OmrScoreInterpreter.Analysis read(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M);}
        int accidental(int index){return read().notes().get(index).writtenAccidental();}
    }
    @Test public void repeatedKeyFlatDoesNotFlattenTheAdjacentHigherPitch(){
        var p=new Page(true);assertEquals(List.of(new ScoreKeyChange(0,-2)),p.read().keyChanges());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(0));
        assertEquals(8,p.read().notes().get(0).staffStep());
    }
    @Test public void keyOwnershipRequiresAProvenClefHeader(){
        var p=new Page(false);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.accidental(0));
    }
    @Test public void aSeparateLocalFlatAfterTheHeaderIsRetained(){
        var p=new Page(true);p.flat(250,132);
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.accidental(1));
    }
    @Test public void aGenuineFlatOnTheFirstNoteRemainsExplicit(){
        var p=new Page(true,154);p.flat(120,100);
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.accidental(0));
    }
    @Test public void keyDoesNotCreateAnExplicitAccidentalOnLaterNotes(){
        var p=new Page(true);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(1));
    }
    @Test public void notesAndTheirStaffPositionsArePreserved(){
        var p=new Page(true);var notes=p.read().notes();assertEquals(2,notes.size());
        assertEquals(List.of(8,4),notes.stream().map(ScoreNoteEvent::staffStep).toList());
    }
    @Test public void sourceArraysRemainUnchanged(){
        var p=new Page(true);var l=p.labels.clone();var g=p.gray.clone();p.read();
        assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);
    }
}
