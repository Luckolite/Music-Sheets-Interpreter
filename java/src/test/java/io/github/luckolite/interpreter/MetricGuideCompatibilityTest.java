// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

/** Actual length-framed kind 19 records must not masquerade as guide 263-276. */
public final class MetricGuideCompatibilityTest {
    private ScoreExpressiveEvent mark(Kind kind) {
        return new ScoreExpressiveEvent(
                "original:relation",
                kind,
                Optional.of(new ScoreAnchor(0, 1)),
                Optional.empty(),
                Scope.SCORE,
                0,
                1,
                Optional.empty(),
                Strength.UNSPECIFIED,
                kind == Kind.METRIC_MODULATION ? "metric-pulse-v1:0.75:1.0" : "",
                List.of(
                        new Evidence(
                                "original-synthetic", 0, .3f, 0, 1, "dotted eighth = quarter")));
    }

    private byte[] encode(ScoreExpressiveEvent mark, int version) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ScoreSemanticWire.writeExpressions(new DataOutputStream(bytes), List.of(mark), 2, version);
        return bytes.toByteArray();
    }

    private List<ScoreExpressiveEvent> decode(byte[] bytes, int version) throws Exception {
        return ScoreSemanticWire.readExpressions(
                new DataInputStream(new ByteArrayInputStream(bytes)), 2, version);
    }

    @Test
    public void guide277RetainsExactPulseRelationAndOwnership() throws Exception {
        var event = mark(Kind.METRIC_MODULATION);
        assertEquals(List.of(event), decode(encode(event, 277), 277));
    }

    @Test
    public void olderHeaderRejectsNewRecordRatherThanLosingTempoSemantics() throws Exception {
        var bytes = encode(mark(Kind.METRIC_MODULATION), 277);
        for (int version : List.of(263, 275, 276))
            try {
                decode(bytes, version);
                fail("Kind 19 accepted as guide " + version);
            } catch (IOException expected) {
            }
    }

    @Test
    public void olderWriterCannotEmitKind19ButExistingRecordsRemainIdentical() throws Exception {
        for (int version : List.of(263, 275, 276)) {
            try {
                encode(mark(Kind.METRIC_MODULATION), version);
                fail("Wrote relation as guide " + version);
            } catch (IOException expected) {
            }
            var old = mark(Kind.RITARDANDO);
            assertEquals(List.of(old), decode(encode(old, version), version));
            assertArrayEquals(encode(old, 277), encode(old, version));
        }
    }
}
