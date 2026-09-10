// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** Normalized page geometry for one optically recognized measure. */
public record MeasureRegion(float left, float right, float top, float bottom) { }
