// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original stemless-note notation, including misleading model mask islands. */
public class DetachedTremoloTest {
    private static final int W=320,H=240;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void build(int direction,int strokes,boolean wide) {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++)ink(x,y,4);
        for(int y=91;y<=105;y++)for(int x=108;x<=132;x++) {
            double oval=Math.pow((x-120)/12.,2)+Math.pow((y-98)/7.,2);
            double hole=Math.pow((x-120)/5.,2)+Math.pow((y-98)/5.,2);
            if(oval<=1&&hole>=1)ink(x,y,2);
        }
        for(int i=0;i<strokes;i++)for(int dx=wide?-50:-8;dx<=(wide?50:8);dx++)for(int dy=-2;dy<=2;dy++)
            ink(120+dx,98+direction*(20+i*9)+Math.round(-.5f*dx)+dy,5);
    }
    private List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.03f,.97f,.1f,.9f))).notes();}
    @Test public void strokesBelowWholeNotePreserveItsPitchAndDuration() {
        build(1,3,false);var notes=notes();assertEquals(1,notes.size());
        assertEquals(5,notes.get(0).staffStep());assertEquals(4,ScoreNoteTiming.writtenDurationBeats(notes.get(0)),0);
        assertEquals(.125,NoteOrnament.tremoloBeats(notes.get(0).articulations()),0);
    }
    @Test public void strokesAboveWholeNoteAreRecognized() {
        build(-1,3,false);assertEquals(1,notes().size());
        assertEquals(.125,NoteOrnament.tremoloBeats(notes().get(0).articulations()),0);
    }
    @Test public void strokeFragmentsDoNotBecomeChordPitches() {
        build(1,3,false);
        for(int y=114;y<=142;y++)for(int x=112;x<=128;x++)if((gray[y*W+x]&255)==0)labels[y*W+x]=2;
        var notes=notes();assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
    }
    @Test public void plainWholeNoteHasNoRepeatedAttack() {
        build(1,0,false);assertEquals(1,notes().size());assertEquals(0,NoteOrnament.tremoloBeams(notes().get(0).articulations()));
    }
    @Test public void longBeamsAreNotDetachedStrokes() {
        build(1,3,true);assertEquals(0,NoteOrnament.tremoloBeams(notes().get(0).articulations()));
    }
    @Test public void anIsolatedSlashDoesNotBecomeThreeStrokes() {
        build(1,1,false);assertEquals(0,NoteOrnament.tremoloBeams(notes().get(0).articulations()));
    }
    @Test public void threeRoundHeadsBelowAWholeNoteAreNotSlashes() {
        build(1,0,false);
        for(int cy:new int[]{118,127,136})for(int y=cy-4;y<=cy+4;y++)for(int x=112;x<=128;x++)
            if(Math.pow((x-120)/8.,2)+Math.pow((y-cy)/4.,2)<=1)ink(x,y,2);
        var notes=notes();assertEquals(4,notes.size());
        for(var note:notes)assertEquals(0,NoteOrnament.tremoloBeams(note.articulations()));
    }
    @Test public void falseStemPaintDoesNotHalveAWholeNote() {
        build(1,3,false);
        for(int y=100;y<=145;y++)labels[y*W+109]=1;
        var notes=notes();assertEquals(1,notes.size());
        assertEquals(4,ScoreNoteTiming.writtenDurationBeats(notes.get(0)),0);
        assertEquals(.125,NoteOrnament.tremoloBeats(notes.get(0).articulations()),0);
    }
}
