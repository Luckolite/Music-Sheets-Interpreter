// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Original generated grayscale controls compare the previous two-thread session. */
public final class NativeSegmentationThreadParity {
    public static void main(String[] args) throws Exception {
        Path model = Path.of(args[0]);
        int[][] dimensions = {{319, 317}, {320, 320}, {321, 319}, {384, 321}, {513, 515}, {32, 96}};
        for (int id = 0; id < dimensions.length; id++) {
            int width = dimensions[id][0], height = dimensions[id][1];
            byte[] gray = new byte[width * height];
            Random random = new Random(0x419256L + id);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    gray[y * width + x] =
                            (byte)
                                    (id == 0
                                            ? 255
                                            : id == 1
                                                    ? 245
                                                    : id == 2
                                                            ? (x % 29 < 2 || y % 31 < 2 ? 40 : 255)
                                                            : id == 3
                                                                    ? 160 + (x + y) % 96
                                                                    : random.nextInt(256));
            byte[] original = gray.clone(), expected;
            try (var baseline = new TwoThreadSegmentationReference(model)) {
                expected = baseline.predict(gray, width, height);
            }
            try (var candidate = new NativeSegmentation(model)) {
                byte[] actual = candidate.predict(gray, width, height);
                if (!Arrays.equals(expected, actual) || !Arrays.equals(original, gray))
                    throw new AssertionError("Frame " + id + " changed");
                if (!Arrays.equals(expected, candidate.predict(gray, width, height)))
                    throw new AssertionError("Repeat " + id + " changed");
            }
            System.out.println(
                    "PASS generated segmentation frame="
                            + id
                            + " pixels="
                            + gray.length
                            + " labels/caller/repeat exact");
            System.out.flush();
        }
    }
}
