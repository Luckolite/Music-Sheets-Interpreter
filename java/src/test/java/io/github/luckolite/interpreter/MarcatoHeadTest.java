// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small angular marks touching staff rules, with false semantic head paint. */
public class MarcatoHeadTest {
    private static final int W=360,H=240;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void build(boolean upBow,boolean round,boolean rules) {
        Arrays.fill(gray,(byte)255);
        if(rules)for(int y=80;y<=128;y+=12)for(int x=16;x<344;x++)ink(x,y,4);
        for(int x=104;x<=136;x++)for(int y:new int[]{140,152})ink(x,y,4);
        for(int y=145;y<=159;y++)for(int x=110;x<=130;x++)
            if(Math.pow((x-120)/10.,2)+Math.pow((y-152)/7.,2)<=1)ink(x,y,2);
        for(int y=97;y<=152;y++)ink(130,y,1);
        for(int dx=-7;dx<=7;dx++)for(int dy=-1;dy<=1;dy++) {
            int y=upBow?94-Math.abs(dx)*2:80+Math.abs(dx)*2;
            if(!round)ink(120+dx,y+dy,5);
        }
        if(round) {
            for(int y=82;y<=92;y++)for(int x=114;x<=126;x++)
                if(Math.pow((x-120)/6.,2)+Math.pow((y-87)/5.,2)<=1)ink(x,y,2);
        } else for(int y=86;y<=94;y++)for(int x=121;x<=128;x++)if((gray[y*W+x]&255)==0)labels[y*W+x]=2;
    }
    private List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.03f,.97f,.1f,.9f))).notes();}
    @Test public void aMarcatoWingDoesNotBecomeAnotherPitch() {
        build(false,false,true);var notes=notes();assertEquals(1,notes.size());assertEquals(-4,notes.get(0).staffStep());
        assertTrue((notes.get(0).articulations()&NoteArticulation.MARCATO)!=0);
    }
    @Test public void aRoundNoteheadIsNotAnAngularMark() {
        build(false,true,true);assertEquals(2,notes().size());
    }
    @Test public void anUpBowIsNotReclassifiedAsMarcato() {
        build(true,false,true);
        assertFalse(NoteArticulationDetector.marcatoAtHead(gray,W,H,121,86,128,94,12,true));
    }
    @Test public void angularRecoveryRequiresRawInk() {
        assertFalse(NoteArticulationDetector.marcatoAtHead(null,W,H,121,86,128,94,12,true));
    }
    @Test public void recoveredMarkAppliesToTheWholeChord() {
        build(false,false,true);
        for(int y=121;y<=135;y++)for(int x=110;x<=130;x++)
            if(Math.pow((x-120)/10.,2)+Math.pow((y-128)/7.,2)<=1)ink(x,y,2);
        var notes=notes();assertEquals(2,notes.size());
        for(var note:notes)assertTrue((note.articulations()&NoteArticulation.MARCATO)!=0);
    }
    @Test public void asymmetricRasterEdgesStillFormAnUpwardPeak() {
        Arrays.fill(gray,(byte)255);
        for(int x=16;x<344;x++)for(int y:new int[]{80,96})ink(x,y,4);
        for(int dx=-8;dx<=8;dx++)for(int dy=-1;dy<=(dx>0?2:0);dy++)
            ink(120+dx,80+Math.abs(dx)*2+dy,5);
        assertTrue(NoteArticulationDetector.marcatoAtHead(gray,W,H,124,90,129,96,16.5f,true));
    }
}
