// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

final class Diagnostics {
    private Diagnostics() { }
    static void log(String message) {
        if (Boolean.getBoolean("sheet.interpreter.debug")) System.err.println(message);
    }
}
