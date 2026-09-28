// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original pale shaft with a secondary beam just outside the nominal ink threshold. */
public class FaintSecondaryBeamVetoTest extends StablePaleSingleBeamTest {
    @Test
    public void lighterSecondaryBeamPreventsSingleBeamOverride() throws Exception {
        setup();
        rect(40, 190, 92, 101, 170);
        assertFalse(proof());
    }

    @Test
    public void oneSidedLighterSecondaryAlsoPreventsOverride() throws Exception {
        setup();
        rect(110, 190, 94, 100, 170);
        assertFalse(proof());
    }
}
