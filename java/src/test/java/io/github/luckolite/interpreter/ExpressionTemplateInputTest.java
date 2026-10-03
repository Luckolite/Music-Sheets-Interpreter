// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exact template payloads remain valid on streams that return one byte per read. */
public final class ExpressionTemplateInputTest {
    private static byte[] fixture() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            for (int value : new int[] {0x45584731, 1, 1, 3, 1}) out.writeInt(value);
            out.write(new byte[] {0, 127, (byte) 255});
        }
        return bytes.toByteArray();
    }

    @Test
    public void boundedShortReadsDoNotTruncateACompleteTemplate() throws Exception {
        var source =
                new ByteArrayInputStream(fixture()) {
                    @Override
                    public synchronized int read(byte[] bytes, int offset, int length) {
                        return super.read(bytes, offset, Math.min(1, length));
                    }
                };
        assertNotNull(PrintedExpressionGlyphs.load(source));
        assertEquals(0, source.available());
    }

    @Test
    public void incompletePixelPayloadCannotBecomeATemplate() throws Exception {
        var complete = fixture();
        try {
            PrintedExpressionGlyphs.load(
                    new ByteArrayInputStream(Arrays.copyOf(complete, complete.length - 1)));
            fail("Truncated template accepted");
        } catch (EOFException expected) {
            assertEquals("Truncated expression template", expected.getMessage());
        }
    }
}
