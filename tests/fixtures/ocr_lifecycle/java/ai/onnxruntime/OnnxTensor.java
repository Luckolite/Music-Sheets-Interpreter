// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package ai.onnxruntime;

import java.nio.*;

public final class OnnxTensor implements AutoCloseable {
    private final Object value;
    private final boolean input;
    private boolean closed;

    private OnnxTensor(Object value, boolean input) {
        this.value = value;
        this.input = input;
    }

    public static OnnxTensor createTensor(
            OrtEnvironment environment, FloatBuffer input, long[] shape) throws OrtException {
        int position = input.position(), limit = input.limit();
        FloatBuffer snapshot = input.duplicate();
        int[] bits = new int[snapshot.remaining()];
        for (int i = 0; i < bits.length; i++) bits[i] = Float.floatToRawIntBits(snapshot.get());
        if (input.position() != position || input.limit() != limit)
            throw new AssertionError("caller cursor");
        FakeOrt.lastInputBits = bits;
        FakeOrt.lastInputShape = shape.clone();
        FakeOrt.tensorsCreated++;
        FakeOrt.events.add("tensor.new");
        return new OnnxTensor(null, true);
    }

    static OnnxTensor output(String path) {
        return new OnnxTensor(
                path.equals("detector")
                        ? new float[][][][] {{FakeOrt.detectorOutput}}
                        : new float[][][] {FakeOrt.recognizerOutput},
                false);
    }

    public Object getValue() {
        if (closed) throw new AssertionError("closed tensor");
        return value;
    }

    public void close() {
        if (closed) throw new AssertionError("double tensor close");
        closed = true;
        if (input) {
            FakeOrt.tensorsClosed++;
            FakeOrt.events.add("tensor.close");
        }
    }
}
