// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original notation geometry for independent tremolo and rhythm beams. */
public class TremoloRhythmTest {
    private static final int W=360,H=240;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void build(boolean beamed,int strokes,boolean hook,boolean hollow) {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<344;x++)ink(x,y,4);
        for(int cx:new int[]{80,180}) {
            for(int y=92;y<=104;y++)for(int x=cx-10;x<=cx+10;x++)
                if(Math.pow((x-cx)/10.,2)+Math.pow((y-98)/6.,2)<=1
                        &&(!hollow||Math.pow((x-cx)/6.,2)+Math.pow((y-98)/3.,2)>1))ink(x,y,2);
            for(int y=98;y<=174;y++)ink(cx-10,y,1);
            for(int n=0;n<strokes;n++)for(int x=hook?cx-10:cx-20;x<=cx;x++)for(int dy=-2;dy<=2;dy++)
                ink(x,157-n*12+dy-Math.round((x-(cx-10))*.2f),5);
        }
        if(beamed)for(int x=70;x<=170;x++)for(int y=170;y<=174;y++)ink(x,y,5);
    }
    private List<ScoreNoteEvent> notes() {
        return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.03f,.97f,.1f,.9f))).notes();
    }
    @Test public void eighthNotesWithOneStrokeKeepEighthDurationAndRepeatSixteenths() {
        build(true,1,false,false);var notes=notes();assertEquals(2,notes.size());
        for(var n:notes){assertEquals(1,n.beamCount());assertEquals(.5,ScoreNoteTiming.writtenDurationBeats(n),0);assertEquals(.25,NoteOrnament.tremoloBeats(n.articulations()),0);}
    }
    @Test public void unmarkedBeamsKeepOrdinaryRhythm() {
        build(true,0,false,false);
        for(var n:notes()){assertEquals(1,n.beamCount());assertEquals(0,NoteOrnament.tremoloBeams(n.articulations()));}
    }
    @Test public void aOneSidedBeamHookIsNotTremolo() {
        build(true,1,true,false);
        for(var n:notes())assertEquals(0,NoteOrnament.tremoloBeams(n.articulations()));
    }
    @Test public void twoLongBeamsAreSixteenthsWithoutTremolo() {
        build(true,0,false,false);
        for(int x=70;x<=170;x++)for(int y=157;y<=161;y++)ink(x,y,5);
        for(var n:notes()){assertEquals(2,n.beamCount());assertEquals(0,NoteOrnament.tremoloBeams(n.articulations()));}
    }
    @Test public void threeStrokesOnAQuarterPreserveOneBeat() {
        build(false,3,false,false);assertEquals(2,notes().size());
        for(var n:notes()){assertEquals(1,ScoreNoteTiming.writtenDurationBeats(n),0);assertEquals(.125,NoteOrnament.tremoloBeats(n.articulations()),0);}
    }
    @Test public void threeStrokesOnAHalfPreserveTwoBeats() {
        build(false,3,false,true);assertEquals(2,notes().size());
        for(var n:notes()){assertEquals(2,ScoreNoteTiming.writtenDurationBeats(n),0);assertEquals(.125,NoteOrnament.tremoloBeats(n.articulations()),0);}
    }
    @Test public void aStemStrokeMaskIslandIsNotAnotherPitch() {
        build(true,1,false,false);
        for(int y=155;y<=159;y++)for(int x=61;x<=64;x++)labels[y*W+x]=2;
        var notes=notes();assertEquals(2,notes.size());
        for(var n:notes())assertEquals(5,n.staffStep());
    }
    @Test public void articulationAndTremoloMetadataCoexist() {
        int marks=NoteOrnament.withTremolo(NoteArticulation.ACCENT|NoteArticulation.STACCATO,2);
        assertEquals(2,NoteOrnament.tremoloBeams(marks));
        assertEquals(NoteArticulation.ACCENT|NoteArticulation.STACCATO,NoteOrnament.withTremolo(marks,0));
    }
    @Test public void aStrokeOnAStaffRuleIsNotAnotherPitch() {
        build(true,1,false,false);
        // Move the complete written group up: the stroke now crosses the bottom rule.
        Arrays.fill(gray,(byte)255);Arrays.fill(labels,(byte)0);
        for(int y=80;y<=128;y+=12)for(int x=16;x<344;x++)ink(x,y,4);
        for(int cx:new int[]{80,180}) {
            for(int y=62;y<=74;y++)for(int x=cx-10;x<=cx+10;x++)
                if(Math.pow((x-cx)/10.,2)+Math.pow((y-68)/6.,2)<=1)ink(x,y,2);
            for(int y=68;y<=144;y++)ink(cx-10,y,1);
            for(int x=cx-20;x<=cx;x++)for(int dy=-2;dy<=2;dy++)
                ink(x,127+dy-Math.round((x-(cx-10))*.2f),5);
            for(int y=126;y<=130;y++)for(int x=cx-19;x<=cx-13;x++)labels[y*W+x]=2;
        }
        for(int x=70;x<=170;x++)for(int y=140;y<=144;y++)ink(x,y,5);
        var notes=notes();assertEquals(2,notes.size());
        for(var n:notes()){assertEquals(10,n.staffStep());assertEquals(.5,ScoreNoteTiming.writtenDurationBeats(n),0);}
    }
    @Test public void nearbyThinRuleDoesNotDoubleTheTremoloRate() {
        build(true,1,false,false);
        for(int y=152;y<=153;y++)for(int x=16;x<344;x++)ink(x,y,4);
        var notes=notes();assertEquals(2,notes.size());
        for(var n:notes())assertEquals(.25,NoteOrnament.tremoloBeats(n.articulations()),0);
    }
}
