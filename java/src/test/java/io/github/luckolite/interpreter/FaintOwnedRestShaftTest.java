// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Additional original paired-shaft controls at the supplemental faint-ink boundary. */
public class FaintOwnedRestShaftTest extends ContrastedRestAccidentalTest {
    @Test
    public void faintPairedShaftsRetainIndependentContrastProof() throws Exception {
        setup(245, 180);
        assertTrue(pair(true));
    }

    @Test
    public void faintSingleShaftIsNotPair() throws Exception {
        setup(245, 180);
        line(35, 10, 46, 245);
        assertFalse(pair(true));
    }

    @Test
    public void faintUncontrastedPairIsNotProof() throws Exception {
        setup(205, 180);
        assertFalse(pair(true));
    }

    @Test
    public void beyondFaintBandIsNotProof() throws Exception {
        setup(255, 190);
        assertFalse(pair(true));
    }

    @Test
    public void faintSingleShaftModeRemainsUnchanged() throws Exception {
        setup(245, 180);
        assertFalse(pair(false));
    }
}
