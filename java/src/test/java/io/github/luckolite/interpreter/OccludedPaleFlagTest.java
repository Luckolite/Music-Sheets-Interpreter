// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original companion-head occlusion and insufficient-evidence controls. */
public class OccludedPaleFlagTest extends BoundedPaleFlagTest {
    private void occlusion(int top, int bottom, boolean head) {
        rect(100, 108, top, bottom, 35);
        if (head)
            for (int y = top; y <= bottom; y++)
                for (int x = 100; x <= 108; x++)
                    labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
    }

    @Test
    public void labeledCompanionHeadDoesNotEraseVisibleShaftProof() throws Exception {
        bounded(true, true);
        occlusion(117, 129, true);
        assertNotNull(endpoint());
    }

    @Test
    public void nonHeadBlockCannotAuthorizeOcclusion() throws Exception {
        bounded(true, true);
        occlusion(117, 129, false);
        assertNull(endpoint());
    }

    @Test
    public void fullyOccludedShaftHasInsufficientIndependentEvidence() throws Exception {
        bounded(true, true);
        occlusion(100, 139, true);
        assertNull(endpoint());
    }
}
