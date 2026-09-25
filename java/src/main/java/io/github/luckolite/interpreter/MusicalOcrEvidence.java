// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Locale;
import java.util.Set;

/** Agreement checks shared by platform-independent musical OCR consumers. */
final class MusicalOcrEvidence {
    private MusicalOcrEvidence() { }
    static String fontMeter(String text,float upperScore,float lowerScore,Set<String> upper,Set<String> lower) {
        if(text==null||!text.matches("[0-9]{1,2}/(?:2|4|8|16|32)")||upperScore<.84f||lowerScore<.84f)return "";
        String[] parts=text.split("/");
        return (upper.isEmpty()||upper.contains(parts[0]))&&(lower.isEmpty()||lower.contains(parts[1]))?text:"";
    }
    static String ornamentToken(String value) {
        if(value==null)return "";String token=value.trim().toLowerCase(Locale.ROOT);
        return token.matches("(?:tr|[pd]ort?)[.,]?")?(token.startsWith("tr")?"tr":"port"):"";
    }
}
