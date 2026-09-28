// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationJsonTest {
    @Test
    public void directionKindsHaveStableNumericWireValues() throws Exception {
        for (var kind :
                java.util.List.of(
                        ScorePlaybackDirection.Kind.SEGNO,
                        ScorePlaybackDirection.Kind.TO_CODA,
                        ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA,
                        ScorePlaybackDirection.Kind.CODA))
            assertEquals(
                    "{\"measureBoundary\":3,\"kind\":" + kind.ordinal() + "}",
                    Main.json(new ScorePlaybackDirection(3, kind)));
    }
}
