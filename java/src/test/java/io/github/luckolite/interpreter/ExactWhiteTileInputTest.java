// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tensors protect exact equality across all channels and padded positions. */
public final class ExactWhiteTileInputTest {
    @Test
    public void checksEveryChannelAndPaddingPosition() {
        int plane = 320 * 320;
        float[] input = new float[3 * plane];
        Arrays.fill(input, 255f);
        assertTrue(ExactWhiteTileInput.matches(input));
        int[] positions = {0, 319, plane - 1, plane, 2 * plane - 1, 2 * plane, input.length - 1};
        float[] different = {
            0f,
            -0f,
            254f,
            Math.nextDown(255f),
            Math.nextUp(255f),
            Float.NaN,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY
        };
        for (int position : positions) {
            for (float value : different) {
                input[position] = value;
                assertFalse(
                        "position=" + position + " value=" + value,
                        ExactWhiteTileInput.matches(input));
                input[position] = 255f;
            }
        }
        assertTrue(ExactWhiteTileInput.matches(input));
    }

    @Test
    public void inputValuesAndFloatBitsAreUnchanged() {
        float[] input = new float[3 * 320 * 320];
        Arrays.fill(input, 255f);
        float[] before = input.clone();
        assertTrue(ExactWhiteTileInput.matches(input));
        assertArrayEquals(before, input, 0f);
        input[input.length - 1] = Float.intBitsToFloat(0x7fc00425);
        int[] bits = new int[input.length];
        for (int i = 0; i < bits.length; i++) bits[i] = Float.floatToRawIntBits(input[i]);
        assertFalse(ExactWhiteTileInput.matches(input));
        for (int i = 0; i < bits.length; i++)
            assertEquals(bits[i], Float.floatToRawIntBits(input[i]));
    }
}
