// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original enlarged raster grace heads with short beams and a larger principal. */
public class RoundedGraceGroupTest {
    private List<ScoreNoteEvent> notes(int count,boolean principal,boolean longStems)throws Exception {
        var p=new GracePrefixGeometryTest.Page();
        for(int i=0;i<count;i++) {
            int x=100+i*25,y=144;
            p.head(x,y,8,7,false);p.stem(x+8,y,longStems?55:34,true);
            for(int xx=x+8;xx<x+29;xx++)for(int yy=110;yy<=113;yy++)p.pixel(xx,yy,5);
        }
        if(principal) {
            int x=100+count*25;p.head(x,136,12,9,false);p.stem(x+12,136,55,true);
        }
        return p.notes();
    }
    private long graces(List<ScoreNoteEvent> notes){return notes.stream().filter(n->(n.articulations()&NoteOrnament.GRACE)!=0).count();}
    @Test public void roundedPairBeforeLargerPrincipalIsGrace()throws Exception {assertEquals(2,graces(notes(2,true,false)));}
    @Test public void longerRoundedPrefixKeepsEveryGrace()throws Exception {assertEquals(3,graces(notes(3,true,false)));}
    @Test public void isolatedLargerSmallHeadIsNotEnough()throws Exception {assertEquals(0,graces(notes(1,true,false)));}
    @Test public void uniformSmallPhraseIsStillMetrical()throws Exception {assertEquals(0,graces(notes(3,false,false)));}
    @Test public void fullLengthStemsRemainMetrical()throws Exception {assertEquals(0,graces(notes(2,true,true)));}
}
