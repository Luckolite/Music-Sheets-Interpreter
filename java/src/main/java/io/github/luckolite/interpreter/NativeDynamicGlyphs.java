// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.awt.image.BufferedImage;
import java.util.*;

/** Desktop equivalent of the app's asset-backed dynamic glyph comparison. */
final class NativeDynamicGlyphs {
    private static final int W = 32, H = 48;
    private static final String[] MARKS = {"p", "pp", "ppp", "m", "mp", "mf", "f", "ff", "fff"};
    private record Template(String text, float aspect, float[] mask) { }
    private record Match(String text, float score, float margin) {
        boolean accepted() {
            return score >= .50f && margin >= .07f
                    || (text.equals("mf") || text.equals("mp"))
                    && (score >= .40f && margin >= .14f || score >= .55f && margin >= .06f);
        }
    }
    private final List<Template> templates;
    private static final boolean TRACE = "1".equals(System.getenv("NATIVE_MUSICAL_OCR_TRACE"));

    NativeDynamicGlyphs() throws java.io.IOException {
        var loaded = new ArrayList<Template>();
        for (String mark : MARKS) {
            BufferedImage image = GlyphResources.image("dynamic_templates/" + mark + ".png");
            loaded.add(new Template(mark, image.getWidth() / (float) image.getHeight(), mask(image)));
        }
        templates = List.copyOf(loaded);
    }

    List<PlayingTechniqueDetector.Word> recognize(byte[] gray, int width, int height,
            List<PlayingTechniqueDetector.Staff> staffs,
            List<PlayingTechniqueDetector.Word> ocrWords) {
        var cleanedWords=DynamicOcrBounds.clean(ocrWords,gray,width,height,staffs);
        var result = new ArrayList<PlayingTechniqueDetector.Word>(cleanedWords);
        var boxes = new ArrayList<>(ScoreDynamicsDetector.symbolBoxes(gray, staffs, width, height));
        // Generic OCR often pads a short italic mark beyond the staff-local size limit.
        // Its independently read word supplies a bounded, ink-tight shape candidate.
        for(var word:cleanedWords)if(Float.isFinite(ScoreDynamicsDetector.level(word.text()))
                &&boxes.stream().noneMatch(box->box.left()==word.left()&&box.top()==word.top()
                &&box.right()==word.right()&&box.bottom()==word.bottom()))boxes.add(word);
        if (TRACE) System.err.println("dynamic glyph boxes=" + boxes.size());
        for (var box : boxes) {
            int left = Math.max(0, Math.round(box.left() * width));
            int top = Math.max(0, Math.round(box.top() * height));
            int right = Math.min(width, Math.round(box.right() * width));
            int bottom = Math.min(height, Math.round(box.bottom() * height));
            if (right <= left || bottom <= top) continue;
            Match match = match(gray, width, left, top, right, bottom);
            if (TRACE && match.score() >= .25f) System.err.println("dynamic glyph=" + box
                    + " match=" + match);
            boolean corroborated=cleanedWords.stream().anyMatch(word->
                    DynamicGlyphEvidence.corroborated(match.text(),match.score(),match.margin(),word.text())
                    &&word.left()<=box.left()&&word.right()>=box.right()
                    &&word.top()<=box.top()&&word.bottom()>=box.bottom()
                    &&word.bottom()-word.top()<1.8f*(box.bottom()-box.top())
                    &&word.right()-word.left()<1.8f*(box.right()-box.left()));
            if (!match.accepted()&&!corroborated) continue;
            boolean clipped=cleanedWords.stream().anyMatch(word->
                    word.top()<box.bottom()&&word.bottom()>box.top()
                    &&DynamicGlyphEvidence.clippedCompound(match.text(),word.text(),box.left(),
                            box.right(),word.left(),word.right()));
            if (clipped) continue;
            result.removeIf(word -> word.left() < box.right() && word.right() > box.left()
                    && word.top() < box.bottom() && word.bottom() > box.top());
            result.add(new PlayingTechniqueDetector.Word(match.text(), box.left(), box.top(),
                    box.right(), box.bottom()));
        }
        return result;
    }

    private Match match(byte[] gray, int width, int left, int top, int right, int bottom) {
        float aspect = (right - left) / (float) (bottom - top);
        float[] candidate = mask(gray, width, left, top, right, bottom);
        String text = ""; float best = 0, second = 0;
        for (var template : templates) {
            double ratio = Math.abs(Math.log(aspect / template.aspect()));
            if (ratio > .4) continue;
            float intersection = 0, total = 0;
            for (int i = 0; i < candidate.length; i++) {
                intersection += Math.min(candidate[i], template.mask()[i]);
                total += Math.max(candidate[i], template.mask()[i]);
            }
            float score = intersection / Math.max(.001f, total) - (float) ratio * .25f;
            if (score > best) { second = best; best = score; text = template.text(); }
            else second = Math.max(second, score);
        }
        return new Match(text, best, best - second);
    }

    private static float[] mask(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        byte[] gray = new byte[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            gray[y * w + x] = (byte) ((image.getRGB(x, y) >>> 16) & 255);
        return mask(gray, w, 0, 0, w, h);
    }

    private static float[] mask(byte[] gray, int width, int left, int top, int right, int bottom) {
        float[] result = new float[W * H];
        float sourceWidth = right - left, sourceHeight = bottom - top;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float sx = left + (x + .5f) * sourceWidth / W - .5f;
            float sy = top + (y + .5f) * sourceHeight / H - .5f;
            int x0 = Math.max(left, Math.min(right - 1, (int) Math.floor(sx)));
            int y0 = Math.max(top, Math.min(bottom - 1, (int) Math.floor(sy)));
            int x1 = Math.min(right - 1, x0 + 1), y1 = Math.min(bottom - 1, y0 + 1);
            float fx = Math.max(0, Math.min(1, sx - x0)), fy = Math.max(0, Math.min(1, sy - y0));
            float value = (gray[y0 * width + x0] & 255) * (1 - fx) * (1 - fy)
                    + (gray[y0 * width + x1] & 255) * fx * (1 - fy)
                    + (gray[y1 * width + x0] & 255) * (1 - fx) * fy
                    + (gray[y1 * width + x1] & 255) * fx * fy;
            result[y * W + x] = value;
        }
        return GlyphInkContrast.mask(result);
    }
}
