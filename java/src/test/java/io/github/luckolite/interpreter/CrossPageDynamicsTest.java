// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class CrossPageDynamicsTest {
    @Test public void aHairpinKeepsItsFullEndpointOnThePageWhereItStarts() {
        var hairpin = new ScoreDynamicChange(1, .7f, 0, 1, 3, .3f, -4, 1);
        var level = new ScoreDynamicChange(3, .3f, 0, 1, 3, .3f, 2, 0);
        var markings = List.of(hairpin, level);
        assertEquals(List.of(hairpin),
                ScorePageInterpretation.dynamicsStartingOnPage(markings, 0, 2));
        assertEquals(List.of(level.offset(-2)),
                ScorePageInterpretation.dynamicsStartingOnPage(markings, 2, 4));
    }
}
