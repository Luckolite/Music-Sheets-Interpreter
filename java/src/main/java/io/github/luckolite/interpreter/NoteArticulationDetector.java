// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Conservative isolated-ink recognition. Unknown marks never shorten a note. */
final class NoteArticulationDetector {
    record Anchor(float x, float y, float gap, int staff) {}

    private record Glyph(int left, int top, int right, int bottom, int count, int[] pixels) {
        float x() {
            return (left + right) * .5f;
        }

        float y() {
            return (top + bottom) * .5f;
        }
    }

    private NoteArticulationDetector() {}

    /** Recover an angular mark whose mask was mistaken for a small notehead.
     * Staff rules may cross its tip or feet; exclude only proven horizontal rules. */
    static boolean marcatoAtHead(
            byte[] gray,
            int width,
            int height,
            int left,
            int top,
            int right,
            int bottom,
            float gap,
            boolean above) {
        if (gray == null || gray.length != width * height) return false;
        int padding = Math.max(3, Math.round(gap * 1.1f));
        int x0 = Math.max(0, left - padding), x1 = Math.min(width - 1, right + padding);
        int y0 = Math.max(0, top - padding), y1 = Math.min(height - 1, bottom + padding);
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        boolean[] rules = new boolean[h], seen = new boolean[w * h];
        for (int y = y0; y <= y1; y++)
            rules[y - y0] = recoveryStaffRule(gray, width, height, (left + right) / 2, y, gap);
        int[] queue = new int[w * h];
        List<Integer> pixels = new ArrayList<>();
        int gx0 = width, gx1 = 0, gy0 = height, gy1 = 0;
        for (int origin = 0; origin < seen.length; origin++) {
            int ox = origin % w, oy = origin / w;
            if (seen[origin] || rules[oy] || (gray[(y0 + oy) * width + x0 + ox] & 255) >= 155)
                continue;
            int size = 1, take = 0;
            queue[0] = origin;
            seen[origin] = true;
            boolean clipped = false;
            while (take < size) {
                int at = queue[take++], x = at % w, y = at / w;
                if (x == 0 || x == w - 1 || y == 0 || y == h - 1) clipped = true;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int next = ny * w + nx;
                        if (!seen[next]
                                && !rules[ny]
                                && (gray[(y0 + ny) * width + x0 + nx] & 255) < 155) {
                            seen[next] = true;
                            queue[size++] = next;
                        }
                    }
            }
            if (clipped || size < 3) continue;
            int partLeft = w, partRight = 0, partTop = h, partBottom = 0;
            for (int i = 0; i < size; i++) {
                int x = queue[i] % w, y = queue[i] / w;
                partLeft = Math.min(partLeft, x);
                partRight = Math.max(partRight, x);
                partTop = Math.min(partTop, y);
                partBottom = Math.max(partBottom, y);
            }
            if (partRight - partLeft + 1 < gap * .2f && partBottom - partTop + 1 > gap * .45f)
                continue;
            for (int i = 0; i < size; i++) {
                int x = x0 + queue[i] % w, y = y0 + queue[i] / w;
                gx0 = Math.min(gx0, x);
                gx1 = Math.max(gx1, x);
                gy0 = Math.min(gy0, y);
                gy1 = Math.max(gy1, y);
                pixels.add(y * width + x);
            }
        }
        if (pixels.isEmpty() || right < gx0 || left > gx1 || bottom < gy0 || top > gy1)
            return false;
        Glyph glyph =
                new Glyph(
                        gx0,
                        gy0,
                        gx1,
                        gy1,
                        pixels.size(),
                        pixels.stream().mapToInt(Integer::intValue).toArray());
        if (classify(glyph, width, gap, above) == NoteArticulation.MARCATO) return true;
        // Removing staff stripes can trim both the peak and feet of a small caret.
        // Only relax its aspect ratio when both edges were actually cut by rules.
        float gw = gx1 - gx0 + 1, gh = gy1 - gy0 + 1;
        boolean trimmed = gy0 > y0 && gy1 < y1 && rules[gy0 - y0 - 1] && rules[gy1 - y0 + 1];
        return trimmed
                && gw >= gap * .5f
                && gw <= gap * 1.3f
                && gh >= gap * .6f
                && gh <= gap * 1.7f
                && gh / gw >= .6f
                && chevronVertical(glyph, width, above);
    }

    /** A handwritten up-bow can leave its rounded tip in the notehead mask. */
    static boolean upBowAtHead(
            byte[] gray,
            int width,
            int height,
            int left,
            int top,
            int right,
            int bottom,
            float gap) {
        if (gray == null || gray.length != width * height || gap <= 0) return false;
        int pad = Math.max(3, Math.round(gap * 2.5f));
        int x0 = Math.max(0, left - pad), x1 = Math.min(width - 1, right + pad);
        int y0 = Math.max(0, top - pad),
                y1 = Math.min(height - 1, bottom + Math.max(2, Math.round(gap * .3f)));
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        boolean[] seen = new boolean[w * h];
        int[] queue = new int[w * h];
        for (int sy = Math.max(y0, top); sy <= Math.min(y1, bottom); sy++)
            for (int sx = Math.max(x0, left); sx <= Math.min(x1, right); sx++) {
                int origin = (sy - y0) * w + sx - x0;
                if (seen[origin] || (gray[sy * width + sx] & 255) >= 155) continue;
                int size = 1, take = 0;
                queue[0] = origin;
                seen[origin] = true;
                boolean clipped = false;
                int gx0 = width, gx1 = 0, gy0 = height, gy1 = 0;
                while (take < size) {
                    int at = queue[take++], x = at % w, y = at / w;
                    if (x == 0 || x == w - 1 || y == 0 || y == h - 1) clipped = true;
                    gx0 = Math.min(gx0, x + x0);
                    gx1 = Math.max(gx1, x + x0);
                    gy0 = Math.min(gy0, y + y0);
                    gy1 = Math.max(gy1, y + y0);
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                            int next = ny * w + nx;
                            if (!seen[next] && (gray[(y0 + ny) * width + x0 + nx] & 255) < 155) {
                                seen[next] = true;
                                queue[size++] = next;
                            }
                        }
                }
                float gw = gx1 - gx0 + 1, gh = gy1 - gy0 + 1;
                if (clipped
                        || gw < gap * .65f
                        || gw > gap * 3f
                        || gh < gap * .8f
                        || gh > gap * 3.5f
                        || gh / gw < .65f
                        || gh / gw > 2.2f) continue;
                // The mistaken head must be the bottom tip, not a note attached to an arm.
                if ((top + bottom) * .5f < gy0 + gh * .55f) continue;
                int[] pixels = new int[size];
                for (int i = 0; i < size; i++)
                    pixels[i] = (y0 + queue[i] / w) * width + x0 + queue[i] % w;
                if (upBowShape(new Glyph(gx0, gy0, gx1, gy1, size, pixels), width)) return true;
            }
        return false;
    }

    private static boolean upBowShape(Glyph g, int width) {
        double tip = 0;
        int tips = 0;
        for (int p : g.pixels)
            if (p / width >= g.bottom - (g.bottom - g.top) * .15) {
                tip += p % width;
                tips++;
            }
        if (tips == 0) return false;
        double apex = (tip / tips - g.left) / Math.max(1, g.right - g.left);
        if (apex < .2 || apex > .85) return false;
        int leftTop = g.bottom, rightTop = g.bottom;
        for (int p : g.pixels) {
            double x = (p % width - g.left) / (double) Math.max(1, g.right - g.left);
            if (x < .25) leftTop = Math.min(leftTop, p / width);
            if (x > .75) rightTop = Math.min(rightTop, p / width);
        }
        double leftY = (leftTop - g.top) / (double) Math.max(1, g.bottom - g.top);
        double rightY = (rightTop - g.top) / (double) Math.max(1, g.bottom - g.top);
        if (leftY > .45 || rightY > .45) return false;
        int hits = 0;
        boolean[] bins = new boolean[12];
        for (int p : g.pixels) {
            double x = (p % width - g.left) / (double) Math.max(1, g.right - g.left);
            double y = (p / width - g.top) / (double) Math.max(1, g.bottom - g.top);
            double expected =
                    x < apex
                            ? leftY + (1 - leftY) * x / apex
                            : rightY + (1 - rightY) * (1 - x) / (1 - apex);
            double slope = x < apex ? (1 - leftY) / apex : (1 - rightY) / (1 - apex);
            double distance = Math.abs(y - expected) / Math.sqrt(1 + slope * slope);
            if (distance < .14 + .75 / Math.max(1, Math.min(g.right - g.left, g.bottom - g.top))) {
                hits++;
                bins[Math.min(11, (int) (x * 12))] = true;
            }
        }
        int covered = 0;
        for (boolean bin : bins) if (bin) covered++;
        return hits >= g.count * .85 && covered >= 10;
    }

    private static boolean recoveryStaffRule(
            byte[] gray, int width, int height, int x, int y, float gap) {
        if (horizontalRuleInk(gray, width, height, x, y, gap, 155, .85f)) return true;
        // A cue staff may start less than four spaces before its first mark.
        // Shorter horizontal support is sufficient only with two parallel rules.
        if (!shortRuleInk(gray, width, height, x, y, gap)) return false;
        int neighbors = 0;
        for (int offset : new int[] {-2, -1, 1, 2})
            if (shortRuleInk(gray, width, height, x, Math.round(y + offset * gap), gap))
                neighbors++;
        return neighbors >= 2;
    }

    private static boolean shortRuleInk(
            byte[] gray, int width, int height, int x, int y, float gap) {
        int near = Math.max(3, Math.round(gap * .8f)), far = Math.round(gap * 2f);
        for (int direction : new int[] {-1, 1}) {
            int hits = 0, samples = 0;
            for (int d = near; d <= far; d++) {
                int xx = x + direction * d;
                if (xx < 0 || xx >= width) return false;
                samples++;
                for (int yy = Math.max(0, y - 1); yy <= Math.min(height - 1, y + 1); yy++)
                    if ((gray[yy * width + xx] & 255) < 155) {
                        hits++;
                        break;
                    }
            }
            if (samples == 0 || hits < samples * .85f) return false;
        }
        return true;
    }

    static int[] detect(byte[] labels, byte[] gray, int width, int height, List<Anchor> notes) {
        int[] result = new int[notes.size()];
        if (notes.isEmpty() || labels == null || labels.length != width * height) return result;
        // Raw components retain symbols that the semantic model omitted. Never erase staff
        // lines: doing so would manufacture dashes/dots from beams, stems and noteheads.
        boolean raw = gray != null && gray.length == labels.length;
        boolean[] seen = new boolean[labels.length];
        int[] queue = new int[labels.length];
        List<Glyph> glyphs = new ArrayList<>();
        for (int p = 0; p < labels.length; p++) {
            if (seen[p] || !ink(labels, gray, p, raw)) continue;
            int start = 0, end = 1;
            queue[0] = p;
            seen[p] = true;
            int left = p % width, right = left, top = p / width, bottom = top;
            while (start < end) {
                int point = queue[start++], x = point % width, y = point / width;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                        int next = ny * width + nx;
                        if (!seen[next] && ink(labels, gray, next, raw)) {
                            seen[next] = true;
                            queue[end++] = next;
                        }
                    }
            }
            if (end < 3 || right - left > width * .025f || bottom - top > height * .018f) continue;
            glyphs.add(
                    new Glyph(left, top, right, bottom, end, java.util.Arrays.copyOf(queue, end)));
        }
        for (Glyph glyph : glyphs) {
            // Semantic notation normally vetoes an articulation. A raw isolated
            // dot may have been mislabeled as a head and rejected by the pitch
            // reader; it can still be staccato if it is not an accepted head.
            int notation = 0;
            for (int pixel : glyph.pixels) {
                byte label = labels[pixel];
                if (label == OmrMeasurePostProcessor.NOTEHEAD
                        || label == OmrMeasurePostProcessor.STEM_OR_REST
                        || label == OmrMeasurePostProcessor.CLEF_OR_KEY
                        || label == OmrMeasurePostProcessor.STAFF) notation++;
            }
            boolean semanticNotation = notation > glyph.count * .25f;
            int best = -1, mark = 0;
            double distance = Double.MAX_VALUE;
            for (int n = 0; n < notes.size(); n++) {
                Anchor note = notes.get(n);
                float dx = Math.abs(glyph.x() - note.x) / note.gap,
                        dy = Math.abs(glyph.y() - note.y) / note.gap;
                if (dx > .7f || dy < .7f || dy > 8f) continue;
                int candidate = classify(glyph, width, note.gap, glyph.y() < note.y);
                if (candidate == 0)
                    candidate = roundedAngular(glyph, width, note.gap, glyph.y() < note.y);
                // A faint rounded dash needs nearby note ownership, not a lyric
                // extender several spaces beyond the staff.
                if (candidate == 0 && dy <= 3f && roundedTenuto(glyph, width, note.gap))
                    candidate = NoteArticulation.TENUTO;
                boolean stemOwnedDot =
                        raw
                                && candidate == NoteArticulation.STACCATO
                                && dy > 5.5f
                                && longStemDot(glyph, note, gray, width, height);
                if (candidate == 0
                        || dy > 6f && !stemOwnedDot
                        || dy > 5.5f && candidate != NoteArticulation.MARCATO && !stemOwnedDot)
                    continue;
                if (raw
                        && (candidate == NoteArticulation.TENUTO
                                || candidate == NoteArticulation.STACCATO)
                        && directionText(glyph, glyphs, note.gap, labels)) continue;
                if (raw
                        && candidate == NoteArticulation.STACCATO
                        && endingNumberDot(glyph, glyphs, note.gap, gray, labels, width, height))
                    continue;
                if (semanticNotation
                        && (!raw
                                || candidate != NoteArticulation.STACCATO
                                || nearHead(glyph, notes))) continue;
                if (candidate == NoteArticulation.TENUTO && nearHead(glyph, notes)) continue;
                if (raw
                        && candidate == NoteArticulation.TENUTO
                        && glyph.bottom - glyph.top + 1 <= Math.max(2, Math.round(note.gap * .23f))
                        && horizontalRuleInk(
                                gray,
                                width,
                                height,
                                Math.round(glyph.x()),
                                Math.round(glyph.y()),
                                note.gap,
                                235,
                                .50f)) continue;
                if (candidate == NoteArticulation.STACCATO
                        && (durationDot(glyph, notes)
                                || (raw
                                        && shelteredDot(
                                                glyph,
                                                labels,
                                                gray,
                                                width,
                                                height,
                                                note.gap,
                                                dy,
                                                stemOwnedDot
                                                        || pairedTenuto(
                                                                glyph, glyphs, note, width)))))
                    continue;
                // A small off-axis dot beside a head is a duration dot, not staccato.
                if (candidate == NoteArticulation.STACCATO && dx > .35f) continue;
                double score = dx * 3 + dy;
                if (score < distance) {
                    distance = score;
                    best = n;
                    mark = candidate;
                }
            }
            if (best < 0) continue;
            Anchor owner = notes.get(best);
            // One mark on a chord affects that chord, never another staff or another onset.
            for (int n = 0; n < notes.size(); n++) {
                Anchor note = notes.get(n);
                if (note.staff == owner.staff && Math.abs(note.x - owner.x) < owner.gap * .45f)
                    result[n] |= mark;
            }
        }
        return result;
    }

    /** A text word and its spaced extension dashes are not note articulations. */
    private static boolean directionText(
            Glyph target, List<Glyph> glyphs, float gap, byte[] labels) {
        List<Glyph> letters = new ArrayList<>();
        for (Glyph g : glyphs) {
            float h = g.bottom - g.top + 1, w = g.right - g.left + 1;
            if (g == target
                    || g.right > target.left + gap * .2f
                    || target.left - g.right > gap * 36
                    || h < gap * .5f
                    || h > gap * 2.2f
                    || w < gap * .25f
                    || w > gap * 3f
                    || g.top > target.y() + gap * .15f
                    || g.bottom < target.y() - gap * .15f) continue;
            int notation = 0;
            for (int p : g.pixels)
                if (labels[p] == OmrMeasurePostProcessor.NOTEHEAD
                        || labels[p] == OmrMeasurePostProcessor.STEM_OR_REST
                        || labels[p] == OmrMeasurePostProcessor.CLEF_OR_KEY
                        || labels[p] == OmrMeasurePostProcessor.STAFF) notation++;
            if (notation <= g.count * .25f) letters.add(g);
        }
        letters.sort(java.util.Comparator.comparingInt(Glyph::left));
        for (int i = 0; i + 2 < letters.size(); i++) {
            Glyph first = letters.get(i), last = first;
            int count = 1;
            for (int j = i + 1; j < letters.size(); j++) {
                Glyph next = letters.get(j);
                if (next.left - last.right > gap * .65f
                        || Math.abs(next.bottom - first.bottom) > gap * .45f) break;
                last = next;
                count++;
            }
            if (count < 3) continue;
            if (target.left - last.right < gap * .65f) return true;
            // Require a continuous run of thin dashes back to the word. Three
            // isolated tenutos elsewhere on the page do not constitute text.
            List<Glyph> dashes = new ArrayList<>();
            for (Glyph g : glyphs)
                if (g.left >= last.right
                        && g.left <= target.right + gap * 12
                        && Math.abs(g.y() - target.y()) <= gap * .15f
                        && g.bottom - g.top + 1 <= gap * .25f
                        && g.right - g.left + 1 >= gap * .55f
                        && g.right - g.left + 1 <= gap * 1.8f) dashes.add(g);
            dashes.sort(java.util.Comparator.comparingInt(Glyph::left));
            float edge = last.right;
            int matched = 0;
            for (Glyph dash : dashes) {
                if (dash.left - edge > gap * 4.5f) break;
                edge = dash.right;
                matched++;
            }
            if (matched >= 3 && edge >= target.right - gap * .2f) return true;
        }
        return false;
    }

    /** Number punctuation under an ending bracket is not a performance dot. */
    private static boolean endingNumberDot(
            Glyph dot,
            List<Glyph> glyphs,
            float gap,
            byte[] gray,
            byte[] labels,
            int width,
            int height) {
        for (Glyph digit : glyphs) {
            float h = digit.bottom - digit.top + 1, w = digit.right - digit.left + 1;
            if (digit.right >= dot.left
                    || dot.left - digit.right > gap * .7f
                    || Math.abs(digit.bottom - dot.bottom) > gap * .25f
                    || h < gap * .9f
                    || h > gap * 2.5f
                    || w < gap * .3f
                    || w > gap * 1.5f) continue;
            int notation = 0;
            for (int p : digit.pixels)
                if (labels[p] == OmrMeasurePostProcessor.NOTEHEAD
                        || labels[p] == OmrMeasurePostProcessor.STEM_OR_REST
                        || labels[p] == OmrMeasurePostProcessor.STAFF) notation++;
            if (notation > digit.count * .25f) continue;
            for (int top = Math.max(0, Math.round(digit.top - gap)); top < digit.top; top++) {
                for (int left = Math.max(0, Math.round(digit.left - gap));
                        left < digit.left;
                        left++) {
                    int right = Math.min(width - 1, Math.round(dot.right + gap * 3)),
                            bottom = Math.min(height - 1, Math.round(top + gap));
                    int horizontal = 0, vertical = 0;
                    for (int x = left; x <= right; x++)
                        if ((gray[top * width + x] & 255) < 155) horizontal++;
                    if (horizontal < (right - left + 1) * .95f) continue;
                    for (int y = top; y <= bottom; y++)
                        if ((gray[y * width + left] & 255) < 155) vertical++;
                    if (vertical >= (bottom - top + 1) * .95f) return true;
                }
            }
        }
        return false;
    }

    private static boolean ink(byte[] labels, byte[] gray, int p, boolean raw) {
        return raw ? (gray[p] & 255) < 155 : labels[p] == OmrMeasurePostProcessor.SYMBOL;
    }

    private static boolean durationDot(Glyph glyph, List<Anchor> notes) {
        for (Anchor note : notes) {
            float dx = (glyph.x() - note.x) / note.gap,
                    dy = Math.abs(glyph.y() - note.y) / note.gap;
            if (dx > .4f && dx < 1.8f && dy < .6f) return true;
        }
        return false;
    }

    private static boolean nearHead(Glyph glyph, List<Anchor> notes) {
        for (Anchor note : notes)
            if (Math.abs(glyph.x() - note.x) < note.gap * 1.2f
                    && Math.abs(glyph.y() - note.y) < note.gap * .7f) return true;
        return false;
    }

    /** A long printed stem and attached beam can own a dot beyond the usual head radius. */
    private static boolean longStemDot(Glyph dot, Anchor note, byte[] gray, int width, int height) {
        int direction = dot.y() < note.y ? -1 : 1;
        int from = Math.round(note.y + direction * note.gap * .3f),
                to = Math.round(dot.y() - direction * note.gap * .65f);
        if (from < 0 || to < 0 || from >= height || to >= height) return false;
        for (int side : new int[] {-1, 1})
            for (int offset = Math.round(note.gap * .3f);
                    offset <= Math.round(note.gap * .85f);
                    offset++) {
                int x = Math.round(note.x) + side * offset;
                if (x < 1 || x >= width - 1) continue;
                int hits = 0, total = Math.abs(to - from) + 1;
                for (int y = Math.min(from, to); y <= Math.max(from, to); y++)
                    if ((gray[y * width + x] & 255) < 155) hits++;
                if (hits < total * .95f) continue;
                for (int y = Math.max(0, to - Math.round(note.gap * .3f));
                        y <= Math.min(height - 1, to + Math.round(note.gap * .3f));
                        y++)
                    for (int beamSide : new int[] {-1, 1}) {
                        int reach = Math.round(note.gap * 1.7f), end = x + beamSide * reach;
                        if (end < 0 || end >= width) continue;
                        int ink = 0;
                        for (int d = 0; d <= reach; d++)
                            if ((gray[y * width + x + beamSide * d] & 255) < 155) ink++;
                        if (ink >= (reach + 1) * .95f) return true;
                    }
            }
        return false;
    }

    /** A detached dash beyond a dot supplies independent portato evidence. */
    private static boolean pairedTenuto(Glyph dot, List<Glyph> glyphs, Anchor note, int width) {
        int direction = dot.y() < note.y ? -1 : 1;
        for (Glyph glyph : glyphs)
            if (glyph != dot
                    && Math.abs(glyph.x() - dot.x()) <= note.gap * .2f
                    && direction * (glyph.y() - dot.y()) >= note.gap * .35f
                    && direction * (glyph.y() - dot.y()) <= note.gap * 1.2f
                    && classify(glyph, width, note.gap, glyph.y() < note.y)
                            == NoteArticulation.TENUTO) return true;
        return false;
    }

    /** The dot under a fermata arch is not a staccato instruction. */
    private static boolean shelteredDot(
            Glyph glyph,
            byte[] labels,
            byte[] gray,
            int width,
            int height,
            float gap,
            float distance,
            boolean independentlyOwned) {
        for (int direction : new int[] {-1, 1}) {
            int occupied = 0;
            for (int bin = -2; bin <= 2; bin++) {
                int x = Math.round(glyph.x() + bin * gap * .3f);
                boolean found = false;
                for (int d = Math.max(1, Math.round(gap * .35f));
                        d <= Math.round(gap * 1.45f);
                        d++) {
                    int y = Math.round(glyph.y()) + direction * d;
                    if (x < 0 || x >= width || y < 0 || y >= height) continue;
                    int p = y * width + x;
                    // Nearby notation does not shelter a fermata dot.
                    if (labels[p] == OmrMeasurePostProcessor.NOTEHEAD
                            || labels[p] == OmrMeasurePostProcessor.STAFF
                            || labels[p] == OmrMeasurePostProcessor.STEM_OR_REST) continue;
                    if (horizontalStaffInk(gray, width, height, x, y, gap)) continue;
                    if ((gray[p] & 255) < 155) found = true;
                }
                if (found) occupied++;
            }
            if (occupied == 5) {
                if (distance > 2.5f
                        && !independentlyOwned
                        && !longStraightShelter(glyph, gray, width, height, gap, direction))
                    return true;
                int[] tone = new int[glyph.pixels.length];
                for (int i = 0; i < tone.length; i++) tone[i] = gray[glyph.pixels[i]] & 255;
                java.util.Arrays.sort(tone);
                // Only a near, independently dark dot may override the original
                // shelter veto. Gray scan grain and distant text dots abstain.
                if (tone[tone.length / 2] > 130
                        || connectedFermataRoof(glyph, labels, gray, width, height, gap, direction))
                    return true;
            }
        }
        return false;
    }

    /** A fermata roof is one compact centered curve, not unrelated scan specks or a long slur. */
    private static boolean connectedFermataRoof(
            Glyph dot,
            byte[] labels,
            byte[] gray,
            int width,
            int height,
            float gap,
            int direction) {
        int l = Math.max(0, Math.round(dot.x() - gap * 1.8f)),
                r = Math.min(width - 1, Math.round(dot.x() + gap * 1.8f));
        int a = Math.round(dot.y() + direction * gap * .30f),
                b = Math.round(dot.y() + direction * gap * 1.7f);
        int t = Math.max(0, Math.min(a, b)), bottom = Math.min(height - 1, Math.max(a, b));
        int w = r - l + 1, h = bottom - t + 1;
        if (w < 3 || h < 3) return false;
        boolean[] valid = new boolean[w * h], seen = new boolean[w * h];
        int[] queue = new int[w * h];
        for (int y = t; y <= bottom; y++)
            for (int x = l; x <= r; x++) {
                int p = y * width + x;
                byte label = labels[p];
                valid[(y - t) * w + x - l] =
                        (gray[p] & 255) < 155
                                && label != OmrMeasurePostProcessor.NOTEHEAD
                                && label != OmrMeasurePostProcessor.STAFF
                                && label != OmrMeasurePostProcessor.STEM_OR_REST
                                && !horizontalStaffInk(gray, width, height, x, y, gap);
            }
        for (int seed = 0; seed < valid.length; seed++) {
            if (seen[seed] || !valid[seed]) continue;
            int read = 0, count = 1, minX = w, maxX = 0, minY = h, maxY = 0;
            boolean clipped = false;
            queue[0] = seed;
            seen[seed] = true;
            while (read < count) {
                int at = queue[read++], x = at % w, y = at / w;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                if (x == 0 || x == w - 1 || (direction < 0 ? y == 0 : y == h - 1)) clipped = true;
                // A one-pixel antialias gap must not split a genuine roof.
                for (int dy = -2; dy <= 2; dy++)
                    for (int dx = -2; dx <= 2; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int next = ny * w + nx;
                        if (!seen[next] && valid[next]) {
                            seen[next] = true;
                            queue[count++] = next;
                        }
                    }
            }
            if (!clipped
                    && count >= gap
                    && maxX - minX >= gap
                    && maxY - minY >= gap * .20f
                    && count < (maxX - minX + 1) * (maxY - minY + 1) * .55f
                    && Math.abs(l + (minX + maxX) * .5f - dot.x()) <= gap * .35f
                    && l + minX <= dot.x() - gap * .45f
                    && l + maxX >= dot.x() + gap * .45f
                    && curvedRoof(queue, count, w, minX, maxX, gap, direction)) return true;
        }
        return false;
    }

    /** Both ends of an arch turn toward its dot; a ledger stripe or beam does not. */
    private static boolean curvedRoof(
            int[] pixels, int count, int width, int left, int right, float gap, int direction) {
        double[] sums = new double[3];
        int[] sizes = new int[3];
        for (int i = 0; i < count; i++) {
            int x = pixels[i] % width, y = pixels[i] / width;
            float position = (x - left) / (float) Math.max(1, right - left);
            int band =
                    position <= .2f
                            ? 0
                            : position >= .8f ? 2 : position >= .4f && position <= .6f ? 1 : -1;
            if (band >= 0) {
                sums[band] += y;
                sizes[band]++;
            }
        }
        if (sizes[0] == 0 || sizes[1] == 0 || sizes[2] == 0) return false;
        double center = sums[1] / sizes[1];
        return -direction * (sums[0] / sizes[0] - center) >= gap * .12f
                && -direction * (sums[2] / sizes[2] - center) >= gap * .12f;
    }

    /** A long shallow beam/slur is not the compact arch belonging to a fermata dot. */
    private static boolean longStraightShelter(
            Glyph dot, byte[] gray, int width, int height, float gap, int direction) {
        int reach = Math.round(gap * 2), center = Math.round(dot.x());
        if (center - reach < 0 || center + reach >= width) return false;
        for (int d = Math.max(1, Math.round(gap * .35f)); d <= Math.round(gap * 1.45f); d++) {
            int middle = Math.round(dot.y()) + direction * d;
            for (int rise = -4; rise <= 4; rise++) {
                int hits = 0, total = 0;
                for (int x = center - reach; x <= center + reach; x++) {
                    int y = middle + Math.round((x - center) * rise * .05f);
                    if (y < 1 || y >= height - 1) continue;
                    total++;
                    if ((gray[y * width + x] & 255) < 155
                            || (gray[(y - 1) * width + x] & 255) < 155
                            || (gray[(y + 1) * width + x] & 255) < 155) hits++;
                }
                if (total >= reach * 2 && hits >= total * .95f) return true;
            }
        }
        return false;
    }

    /** A missed semantic staff stripe must not become a fermata roof over a dot. */
    private static boolean horizontalStaffInk(
            byte[] gray, int width, int height, int x, int y, float gap) {
        return horizontalRuleInk(gray, width, height, x, y, gap, 155, .85f);
    }

    private static boolean horizontalRuleInk(
            byte[] gray,
            int width,
            int height,
            int x,
            int y,
            float gap,
            int threshold,
            float coverage) {
        int near = Math.max(3, Math.round(gap * 1.5f)), far = Math.round(gap * 4f);
        for (int direction : new int[] {-1, 1}) {
            int hits = 0, samples = 0;
            for (int d = near; d <= far; d++) {
                int xx = x + direction * d;
                if (xx < 0 || xx >= width) return false;
                samples++;
                for (int yy = Math.max(0, y - 1); yy <= Math.min(height - 1, y + 1); yy++)
                    if ((gray[yy * width + xx] & 255) < threshold) {
                        hits++;
                        break;
                    }
            }
            if (samples == 0 || hits < samples * coverage) return false;
        }
        return true;
    }

    /** Antialiased outer pixels need not fill an entire bounding-box row. */
    private static boolean roundedTenuto(Glyph g, int width, float gap) {
        int w = g.right - g.left + 1, h = g.bottom - g.top + 1;
        if (w < gap * .65f || w > gap * 1.65f + 1 || h > gap * .4f + 1 || w / (float) h < 3.5f)
            return false;
        int[] rows = new int[h];
        for (int p : g.pixels) rows[p / width - g.top]++;
        int top = 0, bottom = h - 1;
        if (rows[top] < w * .25f) top++;
        if (bottom > top && rows[bottom] < w * .25f) bottom--;
        int count = 0, wide = 0;
        for (int y = top; y <= bottom; y++) {
            count += rows[y];
            if (rows[y] >= w * .75f) wide++;
        }
        return bottom >= top && wide >= 2 && count > w * (bottom - top + 1) * .7f;
    }

    private static int classify(Glyph g, int width, float gap, boolean above) {
        float w = g.right - g.left + 1, h = g.bottom - g.top + 1;
        float density = g.count / (w * h);
        if (w >= gap * .26f
                && w <= gap * .62f
                && h >= gap * .26f
                && h <= gap * .62f
                && g.count >= gap * gap * .065f
                && w / h > .65f
                && w / h < 1.55f
                && density > .6f) return NoteArticulation.STACCATO;
        if (w >= gap * .65f && w <= gap * 1.65f && h <= gap * .4f && w / h >= 3.5f && density > .7f)
            return NoteArticulation.TENUTO;
        // A fractional staff gap and two rasterized outer edges can make an
        // otherwise complete accent one pixel wider than its scaled bound.
        if (w >= gap * .75f
                && w <= gap * 2.1f + 1
                && h >= gap * .35f
                && h <= gap * 1.25f
                && w / h >= 1.25f
                && fit(g, width, 0)) return NoteArticulation.ACCENT;
        // A V above a note is an up-bow. Only an upward peak above / downward peak below
        // may be marcato; flat-topped down-bow squares fail the two-line fit.
        if (w >= gap * .5f
                && w <= gap * 1.3f
                && h >= gap * .6f
                && h <= gap * 1.7f
                && h / w >= .8f
                && chevronVertical(g, width, above)) return NoteArticulation.MARCATO;
        if (w >= gap * .18f
                && w <= gap * .65f
                && h >= gap * .7f
                && h <= gap * 1.65f
                && h / w >= 1.8f
                && density > .45f
                && density < .82f
                && wedge(g, width, above)) return NoteArticulation.STACCATISSIMO;
        return 0;
    }

    private static boolean chevronVertical(Glyph g, int width, boolean above) {
        return fit(g, width, above ? 1 : 2);
    }

    /** Both precision and coverage matter: a slur, text letter or isolated slash is not a >. */
    /** Rounded-arm recovery is only for detached marks, never the head-demotion path. */
    private static int roundedAngular(Glyph g, int width, float gap, boolean above) {
        float w = g.right - g.left + 1, h = g.bottom - g.top + 1;
        if (w >= gap * .75f
                && w <= gap * 2.1f + 1
                && h >= gap * .35f
                && h <= gap * 1.25f
                && w / h >= 1.25f
                && RoundedAngularArticulation.matches(
                        g.pixels, width, g.left, g.top, g.right, g.bottom, 0))
            return NoteArticulation.ACCENT;
        if (w >= gap * .5f
                && w <= gap * 1.3f
                && h >= gap * .6f
                && h <= gap * 1.7f
                && h / w >= .8f
                && RoundedAngularArticulation.matches(
                        g.pixels, width, g.left, g.top, g.right, g.bottom, above ? 1 : 2))
            return NoteArticulation.MARCATO;
        return 0;
    }

    private static boolean fit(Glyph g, int width, int kind) {
        int hits = 0;
        boolean[] bins = new boolean[12];
        for (int p : g.pixels) {
            double x = (p % width - g.left) / (double) Math.max(1, g.right - g.left);
            double y = (p / width - g.top) / (double) Math.max(1, g.bottom - g.top);
            double a, b;
            if (kind == 0) {
                a = x;
                b = y;
            } else {
                a = kind == 1 ? 1 - y : y;
                b = x;
            }
            double expected = 1 - 2 * Math.abs(b - .5);
            // Small printed accents have antialiased, rounded stroke edges. Allow less than
            // one source pixel of edge rounding while retaining two-arm coverage.
            double tolerance =
                    .22 + .75 / Math.max(1, kind == 0 ? g.right - g.left : g.bottom - g.top);
            if (Math.abs(a - expected) < tolerance) {
                hits++;
                bins[Math.min(11, (int) (b * 12))] = true;
            }
        }
        int covered = 0;
        for (boolean bin : bins) if (bin) covered++;
        return hits >= g.count * .78 && covered >= 10;
    }

    private static boolean wedge(Glyph g, int width, boolean above) {
        int broad = 0, tip = 0;
        for (int p : g.pixels) {
            double y = (p / width - g.top) / (double) Math.max(1, g.bottom - g.top);
            if (!above) y = 1 - y;
            if (y < .33) broad++;
            if (y > .67) tip++;
        }
        return broad >= tip * 1.6 && tip > 0;
    }
}
