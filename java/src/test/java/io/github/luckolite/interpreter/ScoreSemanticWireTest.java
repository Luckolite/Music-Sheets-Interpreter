// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreSemanticWireTest {
    private static final ScoreExpressiveEvent.Evidence EVIDENCE =
            new ScoreExpressiveEvent.Evidence(
                    "synthetic:𝄋", 1, .5f, 1, 2, "D.S. al Coda — con repliche");

    private static ScorePlaybackDirection direction() {
        return new ScorePlaybackDirection(
                1,
                ScorePlaybackDirection.Kind.ENDING,
                new ScorePlaybackDirection.Details(
                        1.5,
                        "ending",
                        "segno",
                        "coda",
                        "outer",
                        3,
                        List.of(1, 3),
                        Optional.of(new ScoreAnchor(5, 0)),
                        ScorePlaybackDirection.AfterJumpRepeats.PLAY,
                        "1., 3. — 𝄋",
                        List.of(EVIDENCE)));
    }

    private static ScoreExpressiveEvent expression() {
        return new ScoreExpressiveEvent(
                "fermata",
                ScoreExpressiveEvent.Kind.FERMATA,
                Optional.of(new ScoreAnchor(1, 1)),
                Optional.of(new ScoreAnchor(2, 0)),
                ScoreExpressiveEvent.Scope.NOTE,
                1,
                2,
                Optional.of("note:α"),
                ScoreExpressiveEvent.Strength.MOLTO,
                "molto",
                List.of(EVIDENCE));
    }

    private static byte[] encode() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        ScoreSemanticWire.writeDirections(out, List.of(direction()), 3);
        ScoreSemanticWire.writeExpressions(out, List.of(expression()), 3);
        return bytes.toByteArray();
    }

    private static void decode(byte[] bytes) throws Exception {
        var input = new DataInputStream(new ByteArrayInputStream(bytes));
        ScoreSemanticWire.readDirections(input, 3);
        ScoreSemanticWire.readExpressions(input, 3);
    }

    @Test
    public void unicodeIdentityEvidenceTargetsAndCrossPageEndRoundTrip() throws Exception {
        var input = new DataInputStream(new ByteArrayInputStream(encode()));
        assertEquals(List.of(direction()), ScoreSemanticWire.readDirections(input, 3));
        assertEquals(List.of(expression()), ScoreSemanticWire.readExpressions(input, 3));
        assertEquals(0, input.available());
    }

    @Test
    public void everyTruncatedPrefixFailsInsteadOfDroppingMetadata() throws Exception {
        byte[] bytes = encode();
        for (int length = 0; length < bytes.length; length++) {
            try {
                decode(Arrays.copyOf(bytes, length));
                fail("Accepted prefix length " + length);
            } catch (IOException expected) {
            }
        }
    }

    @Test
    public void unknownNavigationKindIsNotAcceptedByEnumOrder() throws Exception {
        byte[] bytes = encode();
        java.nio.ByteBuffer.wrap(bytes).putInt(12, 999);
        try {
            decode(bytes);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void excessiveRecordLengthIsRejectedBeforeAllocation() throws Exception {
        byte[] bytes = encode();
        java.nio.ByteBuffer.wrap(bytes).putInt(4, Integer.MAX_VALUE);
        try {
            decode(bytes);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void nonfiniteOffsetIsRejected() throws Exception {
        byte[] bytes = encode();
        java.nio.ByteBuffer.wrap(bytes).putDouble(16, Double.NaN);
        try {
            decode(bytes);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void negativeCountIsRejected() throws Exception {
        byte[] bytes = encode();
        java.nio.ByteBuffer.wrap(bytes).putInt(0, -1);
        try {
            decode(bytes);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void duplicateExpressionIdentitiesAreRejected() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        ScoreSemanticWire.writeExpressions(out, List.of(expression(), expression()), 3);
        try {
            ScoreSemanticWire.readExpressions(
                    new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 3);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void legacyDefaultsHaveExplicitRoundTrip() throws Exception {
        var values =
                List.of(
                        new ScorePlaybackDirection(0, ScorePlaybackDirection.Kind.SEGNO),
                        new ScorePlaybackDirection(
                                3, ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA));
        var bytes = new ByteArrayOutputStream();
        ScoreSemanticWire.writeDirections(new DataOutputStream(bytes), values, 3);
        assertEquals(
                values,
                ScoreSemanticWire.readDirections(
                        new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 3));
    }

    @Test
    public void unpairedSurrogateCannotBeSilentlyReplacedInSourceIdentity() throws Exception {
        var original = expression();
        var malformed =
                new ScoreExpressiveEvent(
                        "bad-\uD800",
                        original.kind(),
                        original.start(),
                        original.end(),
                        original.scope(),
                        original.staffIndex(),
                        original.staffCount(),
                        original.targetEventId(),
                        original.strength(),
                        original.qualifierText(),
                        original.evidence());
        try {
            ScoreSemanticWire.writeExpressions(
                    new DataOutputStream(new ByteArrayOutputStream()), List.of(malformed), 3);
            fail("Invalid UTF16 identity was silently replaced");
        } catch (IOException expected) {
        }
    }

    @Test
    public void writerCannotEmitEvidencePageRejectedByReader() throws Exception {
        var original = expression();
        var event =
                new ScoreExpressiveEvent(
                        original.eventId(),
                        original.kind(),
                        original.start(),
                        original.end(),
                        original.scope(),
                        original.staffIndex(),
                        original.staffCount(),
                        original.targetEventId(),
                        original.strength(),
                        original.qualifierText(),
                        List.of(
                                new ScoreExpressiveEvent.Evidence(
                                        "original", 100001, .5f, 0, 1, "mark")));
        try {
            ScoreSemanticWire.writeExpressions(
                    new DataOutputStream(new ByteArrayOutputStream()), List.of(event), 3);
            fail("Writer accepted evidence outside the reader limit");
        } catch (IOException expected) {
        }
    }
}
