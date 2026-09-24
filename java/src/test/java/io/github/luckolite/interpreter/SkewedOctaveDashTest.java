// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SkewedOctaveDashTest {
    private List<ScoreNoteEvent> read(boolean rising,boolean broken) {
        var gray=OctaveMarkDetectorTest.page();int w=OctaveMarkDetectorTest.W;
        for(int x=120;x<=600;x++)if((x-120)%14<7&&(!broken||x<330||x>410)) {
            int y=55+(rising?-1:1)*Math.round((x-120)*.012f);
            gray[y*w+x]=0;gray[(y+1)*w+x]=0;
        }
        return OctaveMarkDetectorTest.apply(gray,List.of(OctaveMarkDetectorTest.word("8va",80,40)),
                List.of(OctaveMarkDetectorTest.note(150,0),OctaveMarkDetectorTest.note(570,0),OctaveMarkDetectorTest.note(650,0)));
    }
    @Test public void descendingScanSkewKeepsTheFullPrintedSpan(){var n=read(false,false);assertEquals(1,n.get(1).octaveShift());assertEquals(0,n.get(2).octaveShift());}
    @Test public void ascendingScanSkewKeepsTheFullPrintedSpan(){assertEquals(1,read(true,false).get(1).octaveShift());}
    @Test public void largeGapStillEndsTheSpan(){assertEquals(0,read(false,true).get(1).octaveShift());}
    @Test public void terminalHookKeepsLastNoteButNotFollowingNotes() {
        var g=OctaveMarkDetectorTest.page();int w=OctaveMarkDetectorTest.W;
        OctaveMarkDetectorTest.dash(g,120,600,55);
        for(int x=594;x<=600;x++)g[55*w+x]=0;
        for(int y=55;y<=66;y++)g[y*w+600]=0;
        var n=OctaveMarkDetectorTest.apply(g,List.of(OctaveMarkDetectorTest.word("8va",80,40)),
                List.of(OctaveMarkDetectorTest.note(599,0),OctaveMarkDetectorTest.note(630,0)));
        assertEquals(1,n.get(0).octaveShift());assertEquals(0,n.get(1).octaveShift());
    }
}
