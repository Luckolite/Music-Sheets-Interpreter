// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RawStaffLineDetectorTest {
    @Test public void shortStaffWithFractionalPixelSpacingStillHasFiveRules() {
        int width=2048,height=240;byte[] gray=new byte[width*height];Arrays.fill(gray,(byte)255);
        int[] rows={80,94,108,121,135}; // 13.75-pixel engraving rounded to the raster grid.
        for(int row:rows)for(int x=275;x<810;x++)gray[row*width+x]=0;
        var staffs=RawStaffLineDetector.detect(gray,width,height);
        assertEquals(1,staffs.size());
        assertArrayEquals(rows,staffs.get(0).rows());
    }

    @Test public void denseBeamCannotCollapseSeveralStaffLinesIntoOneBand() {
        int width = 600, height = 220, top = 80, gap = 8;
        byte[] gray = new byte[width * height];
        Arrays.fill(gray, (byte) 0xff);
        for (int line = 0; line < 5; line++)
            for (int x = 60; x <= 540; x++) gray[(top + line * gap) * width + x] = 0;

        // A broad beam dark enough to pass the page-wide line threshold joins three staff-line
        // projection bands. The old contiguous-run detector emitted only three line rows here.
        for (int y = top + gap; y <= top + gap * 3; y++)
            for (int x = 120; x <= 480; x++) gray[y * width + x] = 0;

        List<RawStaffLineDetector.StaffLines> staffs =
                RawStaffLineDetector.detect(gray, width, height);

        assertEquals(1, staffs.size());
        assertArrayEquals(new int[]{80, 88, 96, 104, 112}, staffs.get(0).rows());
    }

    @Test public void nearbyNotationPeakDoesNotShiftAnOtherwiseRegularStaff() {
        int width = 600, height = 220, top = 80, gap = 8;
        byte[] gray = new byte[width * height];
        Arrays.fill(gray, (byte) 0xff);
        for (int line = 0; line < 5; line++)
            for (int x = 60; x <= 540; x++) gray[(top + line * gap) * width + x] = 0;
        for (int x = 180; x <= 420; x++) gray[71 * width + x] = 0;

        List<RawStaffLineDetector.StaffLines> staffs =
                RawStaffLineDetector.detect(gray, width, height);

        assertEquals(1, staffs.size());
        assertArrayEquals(new int[]{80, 88, 96, 104, 112}, staffs.get(0).rows());
    }

    @Test public void reducedStaffRequiresAVisibleConnectionToTheNormalStaffs() {
        int width=600,height=650;byte[] gray=new byte[width*height];Arrays.fill(gray,(byte)255);
        for(int top:new int[]{160,320,460})for(int line=0;line<5;line++)
            for(int x=60;x<=540;x++)gray[(top+line*10)*width+x]=0;
        for(int line=0;line<5;line++)for(int x=60;x<=540;x++)gray[(50+line*6)*width+x]=0;
        assertEquals("Unconnected narrow bands are rejected",3,RawStaffLineDetector.detect(gray,width,height).size());
        for(int y=50;y<=200;y++)gray[y*width+60]=0;
        assertEquals("The system rule validates the cue-sized part",4,RawStaffLineDetector.detect(gray,width,height).size());
    }

    @Test public void repeatedMiniatureBeamPatternsCannotBecomeExtraStaffs() {
        int width = 600, height = 760;
        byte[] gray = new byte[width * height];
        Arrays.fill(gray, (byte) 0xff);
        for (int staff = 0; staff < 6; staff++) {
            int top = 40 + staff * 90;
            for (int line = 0; line < 5; line++)
                for (int x = 45; x <= 555; x++) gray[(top + line * 8) * width + x] = 0;
        }
        for (int beam = 0; beam < 3; beam++) {
            int top = 590 + beam * 50;
            for (int line = 0; line < 5; line++)
                for (int x = 90; x <= 510; x++) gray[(top + line * 3) * width + x] = 0;
        }

        List<RawStaffLineDetector.StaffLines> staffs =
                RawStaffLineDetector.detect(gray, width, height);

        assertEquals(6, staffs.size());
        for (RawStaffLineDetector.StaffLines staff : staffs) assertEquals(8f, staff.gap(), .001f);
    }
}
