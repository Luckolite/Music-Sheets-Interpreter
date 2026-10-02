// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original inclined five-rule staffs with semantic stripes in their spaces. */
public class CompressedStaffGeometryTest {
    private int staffs(float slope, boolean tenPrinted) throws Exception {
        int width = 640, height = 360;
        byte[] labels = new byte[width * height], gray = new byte[width * height];
        Arrays.fill(gray, (byte) 220);
        for (int line = 0; line < 10; line++)
            for (int x = 40; x <= 600; x++) {
                int y = 130 + line * 8 + Math.round(slope * (x - width * .5f));
                labels[y * width + x] = 4;
                if (tenPrinted || line % 2 == 0) gray[y * width + x] = 40;
            }
        for (int x : new int[] {40, 300, 600})
            for (int y = 130 + Math.round(slope * (x - width * .5f));
                    y <= 202 + Math.round(slope * (x - width * .5f));
                    y++) {
                labels[y * width + x] = 1;
                gray[y * width + x] = 40;
            }
        var m =
                OmrMeasurePostProcessor.class.getDeclaredMethod(
                        "findStaffs", byte[].class, byte[].class, int.class, int.class);
        m.setAccessible(true);
        return ((List<?>) m.invoke(null, labels, gray, width, height)).size();
    }

    @Test
    public void upwardFiveRuleFrameRejectsSemanticHalfSpacing() throws Exception {
        assertEquals(1, staffs(-.01875f, false));
    }

    @Test
    public void downwardFiveRuleFrameRejectsSemanticHalfSpacing() throws Exception {
        assertEquals(1, staffs(.01875f, false));
    }

    @Test
    public void upwardTwoPrintedStaffsRemainSeparate() throws Exception {
        assertEquals(2, staffs(-.01875f, true));
    }

    @Test
    public void downwardTwoPrintedStaffsRemainSeparate() throws Exception {
        assertEquals(2, staffs(.01875f, true));
    }
}
