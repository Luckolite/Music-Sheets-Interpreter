// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Fingerprint of this standalone build, deliberately distinct from an app build. */
final class NativeDecoderBuild {
    static final String SOURCE_SHA256=load();
    private static String load() {
        try(var in=NativeDecoderBuild.class.getResourceAsStream("native-decoder-sha256.txt")) {
            if(in==null)throw new IOException("Run scripts/build_java.py first");
            String value=new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();
            if(!value.matches("[a-f0-9]{64}"))throw new IOException("Invalid decoder fingerprint");
            return value;
        }catch(IOException failure){throw new ExceptionInInitializerError(failure);}
    }
}
