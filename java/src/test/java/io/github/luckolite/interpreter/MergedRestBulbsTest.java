// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original width profiles with two rounded flags joined by a masked staff valley. */
public class MergedRestBulbsTest {
    private List<?> bulbs(int[] widths, boolean[] mask, float gap) throws Exception {
        var method =
                SixteenthRestDetector.class.getDeclaredMethod(
                        "restBulbs",
                        int[].class,
                        boolean[].class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        boolean.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(null, widths, mask, 0, 0, widths.length - 1, gap, false);
    }

    private int[] paired() {
        return new int[] {
            2, 4, 8, 12, 15, 16, 16, 15, 13, 11, 10, 10, 10, 10, 10, 10, 10, 11, 13, 15, 16, 16, 15,
            12, 8, 5, 4, 3, 3, 3, 2, 2, 2, 1
        };
    }

    @Test
    public void broadStaffMaskCannotMergeTwoRoundedFlags() throws Exception {
        int[] widths = paired();
        boolean[] mask = new boolean[widths.length];
        for (int i = 11; i <= 15; i++) {
            mask[i] = true;
            widths[i] = 0;
        }
        assertEquals(2, bulbs(widths, mask, 16).size());
    }

    @Test
    public void visibleValleySeparatesTwoBulbsEvenAboveWidthThreshold() throws Exception {
        int[] widths = paired();
        assertEquals(2, bulbs(widths, new boolean[widths.length], 16).size());
    }

    @Test
    public void singleBroadRoundedBulbRemainsOneFlag() throws Exception {
        int[] widths = {2, 5, 9, 12, 14, 16, 16, 15, 14, 13, 12, 10, 8, 5, 3, 3, 3, 2, 2, 1};
        assertEquals(1, bulbs(widths, new boolean[widths.length], 16).size());
    }

    @Test
    public void ordinarySingleBulbKeepsOriginalBandCenter() throws Exception {
        int[] widths = {2, 5, 9, 12, 14, 16, 16, 15, 14, 13, 12, 10, 8, 5, 3, 3, 3, 2, 2, 1};
        assertEquals(List.of(6), bulbs(widths, new boolean[widths.length], 16));
    }

    @Test
    public void flatPlateauDoesNotInventSecondFlag() throws Exception {
        int[] widths = new int[34];
        java.util.Arrays.fill(widths, 0, 24, 14);
        assertEquals(1, bulbs(widths, new boolean[widths.length], 16).size());
    }

    @Test
    public void tinyWidthNoiseDoesNotSplitOneBulb() throws Exception {
        int[] widths = paired();
        for (int i = 4; i <= 22; i++) widths[i] = 14 - (i % 3 == 0 ? 1 : 0);
        assertEquals(1, bulbs(widths, new boolean[widths.length], 16).size());
    }
}
