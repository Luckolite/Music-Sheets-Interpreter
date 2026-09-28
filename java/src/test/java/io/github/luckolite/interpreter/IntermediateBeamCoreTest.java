// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two pale beams with darker small centers and a lighter connecting halo. */
public class IntermediateBeamCoreTest extends BeamCoreSeparationTest {
    private Page pale() {
        var p = new Page();
        Arrays.fill(p.gray, (byte) 255);
        Arrays.fill(p.labels, (byte) 0);
        p.rect(60, 73, 150, 91, 155, 5);
        p.rect(60, 73, 150, 79, 140, 5);
        p.rect(60, 85, 150, 91, 140, 5);
        p.rect(60, 75, 150, 77, 100, 5);
        p.rect(60, 87, 150, 89, 100, 5);
        return p;
    }

    @Test
    public void intermediateContrastKeepsTwoFullBeamBodies() throws Exception {
        assertEquals(2, pale().count());
    }

    @Test
    public void equallyDarkBridgeStillStaysSingle() throws Exception {
        var p = pale();
        p.rect(60, 80, 150, 84, 100, 5);
        assertEquals(1, p.count());
    }

    @Test
    public void narrowSecondaryCoreDoesNotGainThickness() throws Exception {
        var p = pale();
        p.rect(60, 85, 150, 91, 255, 0);
        p.rect(60, 87, 150, 89, 100, 5);
        assertEquals(1, p.count());
    }

    @Test
    public void noteheadBodyIsNotSecondaryBeam() throws Exception {
        var p = pale();
        p.rect(60, 85, 150, 91, 140, 2);
        assertEquals(1, p.count());
    }
}
