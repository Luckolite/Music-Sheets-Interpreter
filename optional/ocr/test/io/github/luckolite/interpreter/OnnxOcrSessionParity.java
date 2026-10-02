// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Original generated tensors compare the binding with the previous session configuration. */
public final class OnnxOcrSessionParity {
    private static float[] input(int width, int height, int pattern) {
        float[] values = new float[3 * width * height];
        Random random = new Random(0x6291731L + width * 19 + height * 31 + pattern);
        for (int channel = 0; channel < 3; channel++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float value =
                            pattern == 0
                                    ? 1f
                                    : pattern == 1
                                            ? (x % 19 < 3 || y % 13 < 2 ? -1f : 1f)
                                            : random.nextFloat() * 2 - 1;
                    values[channel * width * height + y * width + x] = value;
                }
            }
        }
        return values;
    }

    private static void sameInput(float[] expected, float[] actual) {
        for (int index = 0; index < expected.length; index++) {
            if (Float.floatToRawIntBits(expected[index])
                    != Float.floatToRawIntBits(actual[index])) {
                throw new AssertionError("Caller tensor changed at " + index);
            }
        }
    }

    private static void sameOutput(float[][] expected, float[][] actual, String context) {
        if (expected.length != actual.length) throw new AssertionError(context + ": row count");
        for (int row = 0; row < expected.length; row++) {
            if (expected[row].length != actual[row].length) {
                throw new AssertionError(context + ": column count at " + row);
            }
            for (int column = 0; column < expected[row].length; column++) {
                if (Float.floatToRawIntBits(expected[row][column])
                        != Float.floatToRawIntBits(actual[row][column])) {
                    throw new AssertionError(
                            context + ": probability bits at " + row + "," + column);
                }
            }
        }
    }

    private static float[][] reference(
            OrtEnvironment environment,
            OrtSession session,
            float[] input,
            int width,
            int height,
            boolean detector)
            throws Exception {
        try (var tensor =
                        OnnxTensor.createTensor(
                                environment,
                                FloatBuffer.wrap(input),
                                new long[] {1, 3, height, width});
                var output =
                        session.run(Map.of(session.getInputNames().iterator().next(), tensor))) {
            return detector
                    ? ((float[][][][]) output.get(0).getValue())[0][0]
                    : ((float[][][]) output.get(0).getValue())[0];
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Expected detector.onnx recognizer.onnx; dictionary adjacent to recognizer");
        }
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        List<String> dictionary =
                Files.readAllLines(Path.of(args[1] + ".dictionary"), StandardCharsets.UTF_8);
        try (var options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2);
            options.setInterOpNumThreads(1);
            try (var binding = new OnnxOcrInference(args[0], args[1]);
                    var detector = environment.createSession(args[0], options);
                    var recognizer = environment.createSession(args[1], options)) {
                if (!dictionary.equals(binding.dictionary()))
                    throw new AssertionError("Dictionary changed");
                int[][] shapes = {
                    {32, 32},
                    {96, 64},
                    {64, 96},
                    {128, 128},
                    {32, 48},
                    {64, 48},
                    {128, 48},
                    {320, 48},
                    {512, 48}
                };
                int cases = 0;
                for (int shape = 0; shape < shapes.length; shape++) {
                    int width = shapes[shape][0], height = shapes[shape][1];
                    boolean isDetector = shape < 4;
                    for (int pattern = 0; pattern < 3; pattern++) {
                        float[] values = input(width, height, pattern), original = values.clone();
                        float[][] expected =
                                reference(
                                        environment,
                                        isDetector ? detector : recognizer,
                                        values,
                                        width,
                                        height,
                                        isDetector);
                        sameInput(original, values);
                        for (int repeat = 0; repeat < 2; repeat++) {
                            float[][] actual =
                                    isDetector
                                            ? binding.detect(values, width, height)
                                            : binding.recognize(values, width, height);
                            sameOutput(
                                    expected,
                                    actual,
                                    "shape=" + shape + " pattern=" + pattern + " repeat=" + repeat);
                            sameInput(original, values);
                        }
                        cases++;
                    }
                }
                System.out.println(
                        "PASS "
                                + cases
                                + " generated OCR tensors: every probability bit, repeated call, dictionary and caller input exact");
            }
        }
    }
}
