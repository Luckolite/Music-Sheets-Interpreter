// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Procedural tilted whole-note chords with closed white interiors. */
public class HollowChordTremoloGuardTest {
    @Test public void wholeChordIsNotAStackOfDetachedTremoloStrokes() {
        int w=320,h=240;byte[] l=new byte[w*h],g=new byte[w*h];Arrays.fill(g,(byte)255);
        for(int cy:new int[]{92,104,116})for(int y=cy-10;y<=cy+10;y++)for(int x=109;x<=131;x++) {
            double dx=x-120,dy=y-cy+dx*.3;
            if(dx*dx/121+dy*dy/25<=1){l[y*w+x]=2;g[y*w+x]=(byte)(dx*dx/36+dy*dy/6.25<1?255:0);}
        }
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++){if(l[y*w+x]!=2)l[y*w+x]=4;g[y*w+x]=0;}
        var notes=OmrScoreInterpreter.extract(l,g,w,h,List.of(new MeasureRegion(.03f,.97f,.15f,.8f)));
        assertEquals(3,notes.size());for(var n:notes){assertEquals(4,n.unbeamedDurationBeats(),0);assertEquals(0,NoteOrnament.tremoloBeams(n.articulations()));}
    }
}
