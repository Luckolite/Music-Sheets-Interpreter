// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads an isolated printed 3 over a short-note group or inside its explicit tuplet bracket. */
final class TripletRhythmDetector {
    private TripletRhythmDetector() { }

    static List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes, List<MeasureRegion> measures,
                                       byte[] gray, int width, int height) {
        if (notes == null || notes.size() < 3 || measures == null || gray == null
                || width < 1 || height < 1 || gray.length != width * height) return notes;
        List<ScoreNoteEvent> result = new ArrayList<>(notes);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) order.add(i);
        order.sort(Comparator.comparingInt((Integer i) -> notes.get(i).measureIndex())
                .thenComparingInt(i -> notes.get(i).staffIndex())
                .thenComparingDouble(i -> notes.get(i).positionInMeasure()));
        for (int i = 0; i + 2 < order.size(); i++) {
            ScoreNoteEvent a = result.get(order.get(i)), b = result.get(order.get(i + 1)),
                    c = result.get(order.get(i + 2));
            if (a.measureIndex() < 0 || a.measureIndex() >= measures.size()
                    || a.measureIndex() != b.measureIndex() || a.measureIndex() != c.measureIndex()
                    || a.staffIndex() != b.staffIndex() || a.staffIndex() != c.staffIndex()
                    || a.staffCount() != b.staffCount() || a.staffCount() != c.staffCount()
                    || ((a.articulations()|b.articulations()|c.articulations())&NoteOrnament.GRACE)!=0
                    || a.tupletDivisor() != 1 || b.tupletDivisor() != 1 || c.tupletDivisor() != 1
                    || a.augmentationDots() != 0 || b.augmentationDots() != 0 || c.augmentationDots() != 0
                    || a.beamCount() != b.beamCount() || a.beamCount() != c.beamCount()) continue;
            double value = ScoreNoteTiming.writtenDurationBeats(a);
            if (!Double.isFinite(value) || value > 1
                    || Math.abs(value - ScoreNoteTiming.writtenDurationBeats(b)) > .001
                    || Math.abs(value - ScoreNoteTiming.writtenDurationBeats(c)) > .001) continue;
            float firstGap = b.positionInMeasure() - a.positionInMeasure();
            float secondGap = c.positionInMeasure() - b.positionInMeasure();
            if (firstGap < .022f || secondGap < .022f
                    || Math.max(firstGap, secondGap) > Math.min(firstGap, secondGap) * 1.5f) continue;
            MeasureRegion region = measures.get(a.measureIndex());
            float gap = Math.max(4f, (region.bottom() - region.top()) * height / (8 * a.staffCount()));
            float x1 = (region.left() + a.positionInMeasure() * (region.right() - region.left())) * width;
            float x3 = (region.left() + c.positionInMeasure() * (region.right() - region.left())) * width;
            if (x3 - x1 < gap * 2 || x3 - x1 > gap * 18) continue;
            float y1 = Math.min(a.pageY(), Math.min(b.pageY(), c.pageY())) * height;
            float y2 = Math.max(a.pageY(), Math.max(b.pageY(), c.pageY())) * height;
            if (!hasPrintedThree(gray, width, height, x1, x3, y1, y2, gap, a.beamCount() > 0)) continue;
            for (int j = 0; j < 3; j++) {
                int index = order.get(i + j);
                ScoreNoteEvent n = result.get(index);
                result.set(index, new ScoreNoteEvent(n.measureIndex(), n.positionInMeasure(),
                        n.staffStep(), n.staffIndex(), n.staffCount(), n.pageY(), n.tiedFromPrevious(),
                        n.augmentationDots(), n.beamCount(), n.writtenAccidental(),
                        n.unbeamedDurationBeats(), 3, n.followingRestBeats(), n.articulations(), n.clefBottomDiatonic(), n.crossStaffBeam(), n.leadingRestBeats()));
            }
            i += 2;
        }
        return List.copyOf(result);
    }

    private static boolean hasPrintedThree(byte[] gray, int width, int height, float firstX,
                                            float lastX, float firstY, float lastY, float gap,
                                            boolean shortNotes) {
        float centerX = (firstX + lastX) * .5f;
        // Numerals align with the beam/stems, which can sit to one side of the
        // oval centres. Include that offset without clipping an italic 3.
        int left = Math.max(0, Math.round(centerX - gap * 1.65f));
        int right = Math.min(width - 1, Math.round(centerX + gap * 1.65f));
        int top = Math.max(0, Math.round(firstY - gap * 7));
        int bottom = Math.min(height - 1, Math.round(lastY + gap * 7));
        int localWidth = right - left + 1, localHeight = bottom - top + 1;
        if (localWidth < 3 || localHeight < 3) return false;
        boolean[] visited = new boolean[localWidth * localHeight];
        int[] queue = new int[visited.length];
        for (int origin = 0; origin < visited.length; origin++) {
            if (visited[origin] || !dark(gray, width, left + origin % localWidth,
                    top + origin / localWidth)) continue;
            int count = 0, pending = 1; queue[0] = origin; visited[origin] = true;
            int minX = right, maxX = left, minY = bottom, maxY = top;
            while (pending > 0) {
                int current = queue[--pending]; count++;
                int x = current % localWidth, y = current / localWidth;
                minX = Math.min(minX, left + x); maxX = Math.max(maxX, left + x);
                minY = Math.min(minY, top + y); maxY = Math.max(maxY, top + y);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || nx >= localWidth || ny < 0 || ny >= localHeight) continue;
                    int next = ny * localWidth + nx;
                    if (!visited[next] && dark(gray, width, left + nx, top + ny)) {
                        visited[next] = true; queue[pending++] = next;
                    }
                }
            }
            int gw = maxX - minX + 1, gh = maxY - minY + 1;
            if (minX <= left || maxX >= right || minY <= top || maxY >= bottom
                    || gh < gap * .7f || gh > gap * 2.3f || gw < gh * .30f || gw > gh * .95f
                    || count < gw * gh * .15f || count > gw * gh * .70f
                    || !(maxY < firstY - gap || minY > lastY + gap)) continue;
            if (!looksLikeThree(gray, width, minX, minY, gw, gh)) continue;
            // Quarter-note tuplets need the two bracket arms. For beamed/flagged short notes,
            // publishers routinely print only the numeral, so its shape/group alignment suffices.
            if (shortNotes || bracketArm(gray, width, height, Math.round(firstX - gap * .3f),
                    minX - 2, minY, maxY, gap)
                    && bracketArm(gray, width, height, maxX + 2, Math.round(lastX + gap * .3f),
                    minY, maxY, gap)) return true;
        }
        return false;
    }

    private static boolean looksLikeThree(byte[] gray, int width, int left, int top, int w, int h) {
        int[] min = new int[h], max = new int[h];
        for (int y = 0; y < h; y++) {
            min[y] = w; max[y] = -1;
            for (int x = 0; x < w; x++) if (dark(gray, width, left + x, top + y)) {
                min[y] = Math.min(min[y], x); max[y] = x;
            }
        }
        int upperOpen = 0, lowerOpen = 0, upperPocket = 0, lowerPocket = 0;
        int upperLobe = -1, lowerLobe = -1, waist = w;
        for (int y = 0; y < h; y++) {
            float fraction = y / (float) h;
            if (fraction >= .15f && fraction <= .36f) {
                upperLobe = Math.max(upperLobe, max[y]);
                if (min[y] >= w * .40f) upperOpen++;
                if (hasLobePocket(gray, width, left, top + y, w)) upperPocket++;
            }
            if (fraction >= .60f && fraction <= .82f) {
                lowerLobe = Math.max(lowerLobe, max[y]);
                if (min[y] >= w * .40f) lowerOpen++;
                if (hasLobePocket(gray, width, left, top + y, w)) lowerPocket++;
            }
            if (fraction >= .37f && fraction <= .55f) waist = Math.min(waist, max[y]);
        }
        int required = Math.max(2, h / 12);
        // Curled terminals put ink on the left of an otherwise open lobe. Allow
        // that ink only with a wide interior pocket and a truly open row in each
        // lobe: a closed 8 and the solid upper-left stem of a 5 still fail.
        boolean upper = upperOpen >= required || upperOpen >= 1 && upperPocket >= required;
        boolean lower = lowerOpen >= required || lowerOpen >= 1 && lowerPocket >= required;
        int indentation = Math.max(1, Math.round(w * .08f));
        return upper && lower && upperLobe - waist >= indentation
                && lowerLobe - waist >= indentation;
    }

    private static boolean hasLobePocket(byte[] gray, int width, int left, int y, int w) {
        int run = 0;
        for (int x = Math.round(w * .25f); x < w; x++) {
            if (!dark(gray, width, left + x, y)) run++;
            else {
                if (run >= Math.max(2, Math.round(w * .22f)) && x >= w * .55f) return true;
                run = 0;
            }
        }
        return false;
    }

    private static boolean bracketArm(byte[] gray, int width, int height, int left, int right,
                                        int top, int bottom, float gap) {
        left = Math.max(0, left); right = Math.min(width - 1, right);
        if (right - left < gap) return false;
        int occupied = 0;
        for (int x = left; x <= right; x++) {
            for (int y = Math.max(0, top); y <= Math.min(height - 1, bottom); y++)
                if (dark(gray, width, x, y)) { occupied++; break; }
        }
        return occupied >= (right - left + 1) * .76f;
    }

    private static boolean dark(byte[] gray, int width, int x, int y) {
        return (gray[y * width + x] & 0xff) <= 165;
    }
}
