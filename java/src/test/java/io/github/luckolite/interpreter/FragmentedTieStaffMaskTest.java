// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic slopes and incomplete model masks; no score image or coordinates. */
public class FragmentedTieStaffMaskTest {
    final int width = 900, height = 400;
    final byte[] labels = new byte[width * height], gray = new byte[width * height];
    float slope = .06f, gap = 16, firstX = 260, lastX = 475;
    int step = 9;

    float bottom(float x) {
        return 195 + slope * x;
    }

    float head(float x) {
        return bottom(x) - step * gap * .5f;
    }

    FragmentedTieStaffMaskTest page(int printedRules, int labelledRules) {
        Arrays.fill(gray, (byte) 242);
        for (int x = 25; x < width - 25; x++)
            for (int line = 0; line < printedRules; line++) {
                int y = Math.round(bottom(x) - line * gap);
                for (int dy = -1; dy <= 1; dy++) {
                    gray[(y + dy) * width + x] = 18;
                    if (line > 0 && line <= labelledRules) labels[(y + dy) * width + x] = 4;
                }
            }
        return this;
    }

    boolean same(float lastY) {
        return LocalTieStaffAlignment.same(
                labels,
                gray,
                width,
                height,
                firstX,
                head(firstX),
                Math.round(firstX) - 9,
                Math.round(firstX) + 9,
                gap,
                lastX,
                lastY,
                Math.round(lastX) - 9,
                Math.round(lastX) + 9,
                gap,
                step);
    }

    @Test
    public void missingOuterModelRuleRetainsMatchingPrintedLevel() {
        page(5, 4);
        assertTrue(same(head(lastX)));
    }

    @Test
    public void aMajorityOfLabelledRulesSupportsCompleteRawRules() {
        page(5, 3);
        assertTrue(same(head(lastX)));
    }

    @Test
    public void downwardSlopeUsesTheSameWrittenLevel() {
        slope = -.06f;
        page(5, 4);
        assertTrue(same(head(lastX)));
    }

    @Test
    public void compressedModelScaleDoesNotChangeWrittenPitch() {
        page(5, 4);
        gap = 15.5f;
        assertTrue(same(bottom(lastX) - 9 * 8));
    }

    @Test
    public void twoLabelledRulesCannotConfirmTheRawStaff() {
        page(5, 2);
        assertFalse(same(head(lastX)));
    }

    @Test
    public void rawRulesAloneCannotCreateAStaff() {
        page(5, 0);
        assertFalse(same(head(lastX)));
    }

    @Test
    public void fourPrintedRulesCannotRecoverTheMissingLevel() {
        page(4, 3);
        assertFalse(same(head(lastX)));
    }

    @Test
    public void sixthPrintedRuleMakesThePhaseAmbiguous() {
        page(6, 5);
        assertFalse(same(head(lastX)));
    }

    @Test
    public void neighbourSpaceCannotBecomeTheSameWrittenPitch() {
        page(5, 4);
        assertFalse(same(head(lastX) + gap * .5f));
    }

    @Test
    public void neighbourRuleCannotBecomeTheSameWrittenPitch() {
        page(5, 4);
        assertFalse(same(head(lastX) + gap));
    }

    @Test
    public void oneSidedRuleFragmentsDoNotProveBothEndpoints() {
        page(5, 4);
        for (int y = 0; y < height; y++)
            Arrays.fill(gray, y * width + 490, y * width + 540, (byte) 242);
        assertFalse(same(head(lastX)));
    }

    @Test
    public void recoveryDoesNotRewriteTheInputMaskOrPixels() {
        page(5, 4);
        byte[] l = labels.clone(), g = gray.clone();
        same(head(lastX));
        assertArrayEquals(l, labels);
        assertArrayEquals(g, gray);
    }
}
