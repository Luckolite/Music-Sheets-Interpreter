// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class MainSemanticJsonTest {
    private String json(Object value) throws Exception {
        var method = Main.class.getDeclaredMethod("json", Object.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }

    @Test
    public void richDirectionDetailsDoNotBreakStandaloneJson() throws Exception {
        String value =
                json(
                        new ScorePlaybackDirection(
                                2,
                                ScorePlaybackDirection.Kind.SEGNO,
                                new ScorePlaybackDirection.Details(
                                        0,
                                        "segno",
                                        "",
                                        "",
                                        "",
                                        2,
                                        List.of(),
                                        Optional.empty(),
                                        ScorePlaybackDirection.AfterJumpRepeats.DEFAULT,
                                        "",
                                        List.of())));
        assertTrue(value.contains("\"kind\":0"));
        assertTrue(value.contains("\"end\":null"));
        assertTrue(value.contains("\"afterJumpRepeats\":\"DEFAULT\""));
    }

    @Test
    public void canonicalOptionalAnchorRemainsStructuredJson() throws Exception {
        assertEquals(
                "{\"measureIndex\":1,\"quarterBeatOffset\":1.5}",
                json(Optional.of(new ScoreAnchor(1, 1.5))));
        assertEquals("null", json(Optional.empty()));
    }
}
