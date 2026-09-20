// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Exact ink bounds for every vertical split, computed once rather than rescanning each half. */
public final class ColumnInkBounds {
    public record Bounds(int left, int top, int right, int bottom) { }
    private final Bounds[] prefixes, suffixes;

    public ColumnInkBounds(int[] argb, int width, int height, int redThreshold) {
        if (width < 1 || height < 1 || (long) width * height != argb.length)
            throw new IllegalArgumentException("raster size");
        Bounds[] columns = new Bounds[width];
        for (int x = 0; x < width; x++) {
            int top = height, bottom = 0;
            for (int y = 0; y < height; y++) if (((argb[y * width + x] >>> 16) & 255) < redThreshold) {
                top = Math.min(top, y); bottom = y + 1;
            }
            if (bottom > top) columns[x] = new Bounds(x, top, x + 1, bottom);
        }
        prefixes = new Bounds[width + 1]; suffixes = new Bounds[width + 1];
        for (int x = 0; x < width; x++) prefixes[x + 1] = union(prefixes[x], columns[x]);
        for (int x = width - 1; x >= 0; x--) suffixes[x] = union(columns[x], suffixes[x + 1]);
    }
    public Bounds leftOf(int cut) { return prefixes[cut]; }
    public Bounds rightOf(int cut) { return suffixes[cut]; }
    private static Bounds union(Bounds a, Bounds b) {
        if (a == null) return b;
        if (b == null) return a;
        return new Bounds(Math.min(a.left, b.left), Math.min(a.top, b.top),
                Math.max(a.right, b.right), Math.max(a.bottom, b.bottom));
    }
}

