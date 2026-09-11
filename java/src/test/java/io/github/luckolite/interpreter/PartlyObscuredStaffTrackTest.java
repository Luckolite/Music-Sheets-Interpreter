// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PartlyObscuredStaffTrackTest {
    private static final int WIDTH=2000, HEIGHT=320;
    private static final float GAP=14;
    private static float bottom(int x,int direction) {
        return 220+direction*14*x*x/(WIDTH*(float)WIDTH);
    }
    private static byte[] page(int direction,boolean obscured) {
        byte[] gray=new byte[WIDTH*HEIGHT];Arrays.fill(gray,(byte)245);
        for(int x=30;x<WIDTH-30;x++)for(int line=0;line<5;line++) {
            int row=Math.round(bottom(x,direction)-line*GAP);
            gray[row*WIDTH+x]=40;gray[(row+1)*WIDTH+x]=40;
        }
        // A beam overlaps the bottom rule in the middle and near the closing bar.
        if(obscured)for(int x=920;x<1970;x++)if(x<1470||x>1800) {
            int row=Math.round(bottom(x,direction));
            for(int y=row-5;y<=row+5;y++)gray[y*WIDTH+x]=0;
        }
        return gray;
    }
    private static void followsObscuredStaff(int direction) {
        var track=StaffPitchTrack.detect(page(direction,true),WIDTH,HEIGHT,164,220,GAP);
        assertNotNull("Intervening clear windows must establish the shallow curve",track);
        for(int x:new int[]{320,800,1680,1880}) {
            assertEquals(bottom(x,direction)+.5f,track.at(x)[0],1.6f);
            assertEquals(GAP,track.at(x)[1],.6f);
        }
        float[] pitch=track.at(1880);
        assertEquals("The space below the top rule keeps its pitch beneath the beam",7,
                Math.round((pitch[0]-(bottom(1880,direction)+.5f-GAP*3.5f))*2/pitch[1]));
    }
    @Test public void followsShallowUpwardCurveWithObscuredMiddleAndEnding() { followsObscuredStaff(-1); }
    @Test public void followsShallowDownwardCurveWithObscuredMiddleAndEnding() { followsObscuredStaff(1); }
    @Test public void completeStraightRulesKeepTheirExistingReference() {
        assertNull(StaffPitchTrack.detect(page(0,false),WIDTH,HEIGHT,164,220,GAP));
    }
    @Test public void disconnectedLedgerGroupsCannotSupplyDenseStaffEvidence() {
        byte[] gray=new byte[WIDTH*HEIGHT];Arrays.fill(gray,(byte)245);
        for(int center=300;center<1800;center+=120)for(int x=center-20;x<center+20;x++)
            for(int line=0;line<5;line++)gray[(Math.round(bottom(center,-1))-line*14)*WIDTH+x]=40;
        assertNull(StaffPitchTrack.detect(gray,WIDTH,HEIGHT,164,220,GAP));
    }
}
