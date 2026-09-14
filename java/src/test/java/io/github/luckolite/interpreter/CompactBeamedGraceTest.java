// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original beamed ellipses with reduced heads and short stems. */
public class CompactBeamedGraceTest {
    private List<ScoreNoteEvent> notes(boolean beam,boolean longStems,boolean principal,boolean fullSize) {
        var p=new GracePrefixGeometryTest.Page();
        for(int x:new int[]{100,129}) {
            p.head(x,152,fullSize?11:9,8,false);
            p.stem(x+(fullSize?11:9),152,longStems?58:38,true);
        }
        if(beam)for(int x=109;x<=138;x++)for(int y=114;y<=117;y++)p.pixel(x,y,1);
        if(principal){p.head(160,160,12,9,false);p.stem(172,160,55,true);}
        return p.notes();
    }
    private long graces(List<ScoreNoteEvent> notes){return notes.stream().filter(n->(n.articulations()&NoteOrnament.GRACE)!=0).count();}
    @Test public void reducedBeamedPairBorrowsOneGraceBudget(){
        var ns=notes(true,false,true,false);assertEquals(3,ns.size());assertEquals(2,graces(ns));
        double budget=ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(0),ns,4)
                +ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(1),ns,4);
        assertEquals(.25,budget,.0001);
        assertEquals(1,budget+ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(2),ns,4),.0001);
    }
    @Test public void separateShortNotesAreNotAGraceGroup(){assertEquals(0,graces(notes(false,false,true,false)));}
    @Test public void fullLengthStemsRemainMetrical(){assertEquals(0,graces(notes(true,true,true,false)));}
    @Test public void uniformlySmallPassageRemainsMetrical(){assertEquals(0,graces(notes(true,false,false,false)));}
    @Test public void ordinaryBeamedNotesRemainMetrical(){assertEquals(0,graces(notes(true,false,true,true)));}
}
