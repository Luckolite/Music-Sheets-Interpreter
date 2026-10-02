// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Checks the exact model tensor, including its padded pixels and all channels. */
final class ExactWhiteTileInput {
    private ExactWhiteTileInput() {}

    static boolean matches(float[] input) {
        for (float pixel : input) if (pixel != 255f) return false;
        return true;
    }
}
