// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original complete pale rules with correct seed spacing but an offset phase. */
public class CompletePrintedPhaseTest extends CompressedFadedStaffTest {
    @Test
    public void matchingSpacingStillRecoversPrintedPhase() throws Exception {
        assertArrayEquals(
                new float[] {660, 15},
                calibrate(page(5, 215, 1, 760), 3, 603, 663, 15, false),
                .1f);
    }

    @Test
    public void incompleteGroupCannotMoveMatchingSpacing() throws Exception {
        assertArrayEquals(
                new float[] {663, 15},
                calibrate(page(4, 215, 1, 760), 3, 603, 663, 15, false),
                .1f);
    }

    @Test
    public void alreadyProvedPhaseIsNotReassigned() throws Exception {
        assertArrayEquals(
                new float[] {663, 15}, calibrate(page(5, 215, 1, 760), 3, 603, 663, 15, true), .1f);
    }
}
