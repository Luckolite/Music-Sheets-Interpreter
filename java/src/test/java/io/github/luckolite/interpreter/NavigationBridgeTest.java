// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationBridgeTest {
    @Test
    public void pageMappingUsesSharedRulesAndRejectsMalformedRequests() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeInt(0x4e415031);
        out.writeInt(2);
        out.writeInt(1);
        out.writeInt(1);
        out.writeInt(1);
        out.writeInt(1);
        byte[] request = bytes.toByteArray();
        assertEquals(
                Main.json(ScorePageTimeline.arrangements(new int[] {1, 1}, new int[] {1, 1})),
                Main.json(
                        NavigationBridge.readArrangements(
                                new DataInputStream(new ByteArrayInputStream(request)))));
        for (int size = 0; size < request.length; size++) {
            final int length = size;
            assertThrows(
                    IOException.class,
                    () ->
                            NavigationBridge.readArrangements(
                                    new DataInputStream(
                                            new ByteArrayInputStream(
                                                    java.util.Arrays.copyOf(request, length)))));
        }
        assertThrows(
                IOException.class,
                () ->
                        NavigationBridge.readArrangements(
                                new DataInputStream(
                                        new ByteArrayInputStream(
                                                java.util.Arrays.copyOf(
                                                        request, request.length + 1)))));
        for (int offset : new int[] {4, 8, 12, 16, 20}) {
            byte[] malformed = request.clone();
            java.nio.ByteBuffer.wrap(malformed).putInt(offset, -1);
            assertThrows(
                    IOException.class,
                    () ->
                            NavigationBridge.readArrangements(
                                    new DataInputStream(new ByteArrayInputStream(malformed))));
        }
        byte[] oversized = request.clone();
        java.nio.ByteBuffer.wrap(oversized).putInt(8, 100000);
        assertThrows(
                IOException.class,
                () ->
                        NavigationBridge.readArrangements(
                                new DataInputStream(new ByteArrayInputStream(oversized))));
    }

    private static byte[] request() throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeInt(0x4e415631);
        out.writeInt(2);
        out.writeFloat(3);
        out.writeFloat(2.5f);
        ScoreSemanticWire.writeDirections(
                out,
                List.of(
                        new ScorePlaybackDirection(0, ScorePlaybackDirection.Kind.REPEAT_START),
                        new ScorePlaybackDirection(2, ScorePlaybackDirection.Kind.REPEAT_END)),
                2);
        ScoreSemanticWire.writeExpressions(out, List.of(), 2);
        return bytes.toByteArray();
    }

    @Test
    public void decodedVariableMeterUsesActualKernel() throws Exception {
        var plan = NavigationBridge.read(new DataInputStream(new ByteArrayInputStream(request())));
        assertEquals(List.of(0, 1, 0, 1), plan.sourceMeasures());
        assertEquals(11, plan.traversal().performedBeats(), 0);
    }

    @Test
    public void truncatedTrailingAndNonfiniteInputIsRejected() throws Exception {
        byte[] bytes = request();
        for (int n = 0; n < bytes.length; n++) {
            final int size = n;
            assertThrows(
                    IOException.class,
                    () ->
                            NavigationBridge.read(
                                    new DataInputStream(
                                            new ByteArrayInputStream(
                                                    java.util.Arrays.copyOf(bytes, size)))));
        }
        assertThrows(
                IOException.class,
                () ->
                        NavigationBridge.read(
                                new DataInputStream(
                                        new ByteArrayInputStream(
                                                java.util.Arrays.copyOf(
                                                        bytes, bytes.length + 1)))));
        java.nio.ByteBuffer.wrap(bytes).putFloat(8, Float.NaN);
        assertThrows(
                IOException.class,
                () -> NavigationBridge.read(new DataInputStream(new ByteArrayInputStream(bytes))));
    }
}
