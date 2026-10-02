// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Locale;
import java.util.Set;

/** Agreement checks shared by platform-independent musical OCR consumers. */
final class MusicalOcrEvidence {
    private MusicalOcrEvidence() {}

    static String fontMeter(
            String text, float upperScore, float lowerScore, Set<String> upper, Set<String> lower) {
        if (text == null
                || !text.matches("[0-9]{1,2}/(?:2|4|8|16|32)")
                || upperScore < .84f
                || lowerScore < .84f) return "";
        String[] parts = text.split("/");
        return (upper.isEmpty() || upper.contains(parts[0]))
                        && (lower.isEmpty() || lower.contains(parts[1]))
                ? text
                : "";
    }

    static String ornamentToken(String value) {
        if (value == null) return "";
        String token = value.trim().toLowerCase(Locale.ROOT);
        return token.matches("(?:tr|[pd]ort?)[.,]?")
                ? (token.startsWith("tr") ? "tr" : "port")
                : "";
    }

    static void addDirectionWords(
            OcrText text,
            java.util.List<PlayingTechniqueDetector.Word> words,
            float scale,
            int top,
            int width,
            int height) {
        for (var block : text.getTextBlocks())
            for (var line : block.getLines()) {
                if (line.getText()
                        .toLowerCase(java.util.Locale.ROOT)
                        .matches(".*\\b(non|senza)\\b.*")) continue;
                if (ScoreNavigationDetector.kind(line.getText()) != null) {
                    var box = line.getBoundingBox();
                    if (box != null)
                        words.add(
                                new PlayingTechniqueDetector.Word(
                                        line.getText(),
                                        box.left / scale / width,
                                        (top + box.top / scale) / height,
                                        box.right / scale / width,
                                        (top + box.bottom / scale) / height));
                }
                // Keep a compound dynamic/expression line at its printed starting slot.
                // The expression element alone begins farther right than the affected head.
                if (PlayingTechniqueDetector.technique(line.getText()) >= 0
                        && line.getText().trim().matches(".*\\s+.*")) {
                    var box = line.getBoundingBox();
                    if (box != null)
                        words.add(
                                new PlayingTechniqueDetector.Word(
                                        line.getText(),
                                        box.left / scale / width,
                                        (top + box.top / scale) / height,
                                        box.right / scale / width,
                                        (top + box.bottom / scale) / height));
                    continue;
                }
                for (var element : line.getElements()) {
                    if (PlayingTechniqueDetector.technique(element.getText()) < 0
                            && OctaveMarkDetector.shift(element.getText()) == 0
                            && ScoreNavigationDetector.kind(element.getText()) == null
                            && !FingeringAnnotationFilter.isFingering(element.getText())) continue;
                    var box = element.getBoundingBox();
                    if (box == null) continue;
                    words.add(
                            new PlayingTechniqueDetector.Word(
                                    element.getText(),
                                    box.left / scale / width,
                                    (top + box.top / scale) / height,
                                    box.right / scale / width,
                                    (top + box.bottom / scale) / height));
                }
            }
    }
}
