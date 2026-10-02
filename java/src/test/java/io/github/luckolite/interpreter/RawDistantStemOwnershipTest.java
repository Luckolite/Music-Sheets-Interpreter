// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural staff, bar and disconnected semantic-stem geometry. */
public class RawDistantStemOwnershipTest {
    private List<Integer> boundaries(boolean printedExtension, int paper) throws Exception {
        int width = 400, height = 300;
        float gap = 12;
        byte[] labels = new byte[width * height], gray = new byte[width * height];
        Arrays.fill(gray, (byte) paper);
        for (int y = 140; y <= 188; y += 12)
            for (int x = 30; x <= 370; x++) {
                labels[y * width + x] = 4;
                gray[y * width + x] = 40;
            }
        for (int y = 104; y <= 188; y++) labels[y * width + 180] = 1;
        for (int y = 140; y <= 188; y++) gray[y * width + 180] = 40;
        if (printedExtension)
            for (int y = 104; y < 140; y++) gray[y * width + 180] = (byte) (paper - 60);
        for (int y = 100; y <= 108; y++)
            for (int x = 180; x <= 192; x++)
                if ((x - 186) * (x - 186) / 36d + (y - 104) * (y - 104) / 16d <= 1) {
                    labels[y * width + x] = 2;
                    if (printedExtension) gray[y * width + x] = 40;
                }
        var m =
                OmrMeasurePostProcessor.class.getDeclaredMethod(
                        "findBoundaries",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int[].class,
                        float.class,
                        int.class,
                        int.class,
                        float.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        var result =
                (List<Integer>)
                        m.invoke(
                                null,
                                labels,
                                gray,
                                width,
                                height,
                                new int[] {140, 152, 164, 176, 188},
                                gap,
                                30,
                                370,
                                0f);
        return result;
    }

    @Test
    public void semanticBridgeAcrossWhitePaperCannotOwnPrintedBar() throws Exception {
        assertEquals(List.of(30, 180, 370), boundaries(false, 255));
    }

    @Test
    public void semanticBridgeAcrossShadedPaperCannotOwnPrintedBar() throws Exception {
        assertEquals(List.of(30, 180, 370), boundaries(false, 185));
    }

    @Test
    public void printedLongStemStillBelongsToItsHead() throws Exception {
        assertEquals(List.of(30, 370), boundaries(true, 255));
    }

    @Test
    public void faintPrintedLongStemStillBelongsToItsHead() throws Exception {
        assertEquals(List.of(30, 370), boundaries(true, 185));
    }
}
