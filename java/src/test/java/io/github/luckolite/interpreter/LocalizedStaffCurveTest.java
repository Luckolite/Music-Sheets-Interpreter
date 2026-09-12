// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original five-rule pages with a smooth curl confined to one edge. */
public class LocalizedStaffCurveTest {
    private static final int WIDTH=1200,HEIGHT=300;
    private static float bottom(int x,boolean left) {
        float position=left?WIDTH-x:x;
        return 210+Math.max(0,(position-WIDTH*.64f)/(WIDTH*.32f))*10;
    }
    private byte[] page(boolean left) {
        byte[] gray=new byte[WIDTH*HEIGHT];Arrays.fill(gray,(byte)245);
        for(int x=30;x<WIDTH-30;x++)for(int line=0;line<5;line++) {
            int y=Math.round(bottom(x,left)-line*12);
            gray[y*WIDTH+x]=55;gray[(y+1)*WIDTH+x]=55;
        }
        return gray;
    }
    @Test public void followsRightEdgeEvenWhenMostOfThePageIsStraight() {
        var track=StaffPitchTrack.detect(page(false),WIDTH,HEIGHT,164,212,12);
        assertNotNull(track);
        assertEquals(bottom(1100,false)+.5f,track.at(1100)[0],1.5f);
        assertEquals(12,track.at(1100)[1],.5f);
    }
    @Test public void followsLeftEdgeEvenWhenMostOfThePageIsStraight() {
        var track=StaffPitchTrack.detect(page(true),WIDTH,HEIGHT,164,212,12);
        assertNotNull(track);
        assertEquals(bottom(140,true)+.5f,track.at(140)[0],1.5f);
        assertEquals(12,track.at(140)[1],.5f);
    }
}
