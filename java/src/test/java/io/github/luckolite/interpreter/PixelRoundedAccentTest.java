// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original chevrons, broad wedges and independent adjacent staff anchors. */
public final class PixelRoundedAccentTest {
    static final int W=1600,H=1000;
    byte[] gray=new byte[W*H];
    public PixelRoundedAccentTest(){Arrays.fill(gray,(byte)255);}
    void accent(int y,int span,boolean oneArm) {
        for(int dx=0;dx<span;dx++) {
            int rise=Math.round(8*dx/(float)(span-1));
            for(int dy=0;dy<2;dy++) {
                gray[(y+rise+dy)*W+282+dx]=0;
                if(!oneArm)gray[(y+16-rise+dy)*W+282+dx]=0;
            }
        }
    }
    int[] read(List<NoteArticulationDetector.Anchor> notes){return NoteArticulationDetector.detect(new byte[W*H],gray,W,H,notes);}
    @Test public void acceptsOnePixelRoundedWidth(){accent(165,36,false);assertEquals(1,read(List.of(new NoteArticulationDetector.Anchor(300,200,16.8f,0)))[0]);}
    @Test public void separateStaffAccentsKeepTheirOwnChords(){
        accent(165,36,false);accent(285,36,false);
        assertArrayEquals(new int[]{1,1,1},read(List.of(new NoteArticulationDetector.Anchor(300,200,16.8f,0),
                new NoteArticulationDetector.Anchor(300,320,16.8f,1),new NoteArticulationDetector.Anchor(300,335,16.8f,1))));
    }
    @Test public void isolatedArmIsNotAnAccent(){accent(165,35,true);assertEquals(0,read(List.of(new NoteArticulationDetector.Anchor(300,200,16.8f,0)))[0]);}
    @Test public void wideHairpinIsNotAnAccent(){accent(165,39,false);assertEquals(0,read(List.of(new NoteArticulationDetector.Anchor(300,200,16.8f,0)))[0]);}
}
