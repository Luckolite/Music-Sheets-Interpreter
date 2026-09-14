// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small ellipses rasterized between pixel rows. */
public class PixelRoundedGraceTest {
    private List<ScoreNoteEvent> notes(boolean stem,boolean principal,boolean large) {
        var p=new GracePrefixGeometryTest.Page();Arrays.fill(p.l,(byte)0);Arrays.fill(p.g,(byte)255);
        for(int y:new int[]{112,129,146,163,180})for(int x=15;x<685;x++)p.pixel(x,y,4);
        float rx=large?11.5f:8.5f,ry=large?9:7;
        for(int y=170;y<=191;y++)for(int x=88;x<=112;x++)
            if(Math.pow((x-100)/rx,2)+Math.pow((y-180.5f)/ry,2)<=1)p.pixel(x,y,2);
        if(stem)p.stem(large?111:108,180,31,true);
        if(principal){p.head(140,188,12,9,false);p.stem(152,188,55,true);}
        return p.notes();
    }
    @Test public void roundedSmallHeadWithLargerPrincipalIsGrace() {
        var ns=notes(true,true,false);assertEquals(2,ns.size());assertTrue((ns.get(0).articulations()&NoteOrnament.GRACE)!=0);
    }
    @Test public void graceAndPrincipalShareTheWrittenDuration() {
        var ns=notes(true,true,false);assertEquals(2,ns.size());
        double a=ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(0),ns,4),b=ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(1),ns,4);
        assertTrue(a>0&&a<=.25);assertEquals(1,a+b,.0001);
    }
    @Test public void missingStemCannotCreateAGrace() {
        assertTrue(notes(false,true,false).stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));
    }
    @Test public void isolatedSmallNoteRemainsMetrical() {
        assertTrue(notes(true,false,false).stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));
    }
    @Test public void fullSizeNoteRemainsMetrical() {
        assertTrue(notes(true,true,true).stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));
    }
}
