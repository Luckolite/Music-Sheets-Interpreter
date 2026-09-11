// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Rejects an inferred separator contradicted by two complete written voices. */
final class PrintedMeasureRhythmGuard {
    private PrintedMeasureRhythmGuard() { }

    static List<MeasureRegion> reconcile(List<MeasureRegion> raw, List<MeasureRegion> fitted,
                                         byte[] labels, byte[] gray, int width, int height) {
        if (raw.stream().noneMatch(region -> fragments(region, fitted).size() == 2)) return fitted;
        var notes = OmrScoreInterpreter.analyze(labels, gray, width, height, raw).notes();
        var aligned = alignPrintedSeparators(raw, fitted, notes, gray, width, height);
        return preserveCompleteRuns(raw, aligned, notes, gray, width, height);
    }

    static List<MeasureRegion> alignPrintedSeparators(List<MeasureRegion> raw, List<MeasureRegion> fitted,
            List<ScoreNoteEvent> notes, byte[] gray, int width, int height) {
        if (gray == null || gray.length != width * height) return fitted;
        List<MeasureRegion> result = new ArrayList<>(fitted);
        for (int index = 0; index < raw.size(); index++) {
            final int measure = index;
            // On a grand staff, a barline crosses the empty space between staves.
            // A single-staff note stem can be just as tall as its measure region.
            if (notes.stream().filter(n -> n.measureIndex() == measure)
                    .map(ScoreNoteEvent::staffIndex).distinct().count() < 2) continue;
            var region = raw.get(index);
            var pieces = fragments(region, result);
            if (pieces.size() != 2 || Math.abs(pieces.get(0).left() - region.left()) > .006f
                    || Math.abs(pieces.get(1).right() - region.right()) > .006f) continue;
            int top = Math.max(0, Math.round(region.top() * height));
            int bottom = Math.min(height - 1, Math.round(region.bottom() * height));
            float span = region.right() - region.left();
            int left = Math.max(0, Math.round((region.left() + span * .15f) * width));
            int right = Math.min(width - 1, Math.round((region.right() - span * .15f) * width));
            float inferred = (pieces.get(0).right() + pieces.get(1).left()) * .5f * width;
            int best = -1; double bestScore = 0;
            for (int x = left; x <= right; x++) {
                int ink = 0;
                for (int y = top; y <= bottom; y++) if ((gray[y * width + x] & 255) < 180) ink++;
                if (ink <= (bottom - top + 1) * .55f) continue;
                double score = ink - Math.abs(x - inferred) * .02;
                if (score > bestScore) { best = x; bestScore = score; }
            }
            if (best < 0) continue;
            float cut = best / (float) width, gap = Math.min(.004f, span * .04f);
            int at = result.indexOf(pieces.get(0));
            result.set(at, new MeasureRegion(region.left(), cut - gap / 2, region.top(), region.bottom()));
            result.set(at + 1, new MeasureRegion(cut + gap / 2, region.right(), region.top(), region.bottom()));
        }
        return List.copyOf(result);
    }

    private record Span(double beats, int groups, boolean allBeamed) { }

    static List<MeasureRegion> preserveCompleteRuns(List<MeasureRegion> raw, List<MeasureRegion> fitted,
            List<ScoreNoteEvent> notes, byte[] gray, int width, int height) {
        List<MeasureRegion> result = new ArrayList<>(fitted);
        for (int index = 0; index < raw.size(); index++) {
            MeasureRegion region = raw.get(index);
            List<MeasureRegion> pieces = fragments(region, result);
            if (pieces.size() != 2 || Math.abs(pieces.get(0).left() - region.left()) > .006f
                    || Math.abs(pieces.get(1).right() - region.right()) > .006f) continue;
            float cut = (pieces.get(0).right() + pieces.get(1).left()) * .5f;
            if (printedBarline(region, cut, gray, width, height)) continue;
            List<Span> voices = spans(notes, index);
            boolean complete = false;
            for (Span voice : voices) {
                if (!voice.allBeamed || voice.groups < 8 || voice.beats < .5 || voice.beats > 16) continue;
                if (voices.stream().filter(other -> same(other.beats, voice.beats)).count() < 2) continue;
                int matchingBars = 0;
                for (int neighbor = 0; neighbor < raw.size(); neighbor++) {
                    if (neighbor == index || !sameRow(raw.get(neighbor), region)
                            || fragments(raw.get(neighbor), fitted).size() != 1) continue;
                    if (spans(notes, neighbor).stream().anyMatch(other -> same(other.beats, voice.beats)))
                        matchingBars++;
                }
                if (matchingBars >= 2) { complete = true; break; }
            }
            if (!complete) continue;
            int at = result.indexOf(pieces.get(0));
            result.removeAll(pieces);
            result.add(at, region);
        }
        return List.copyOf(result);
    }

    private static boolean same(double a, double b) { return Math.abs(a - b) < .01; }
    private static boolean sameRow(MeasureRegion a, MeasureRegion b) {
        return Math.abs(a.top() - b.top()) < .006f && Math.abs(a.bottom() - b.bottom()) < .006f;
    }
    private static List<MeasureRegion> fragments(MeasureRegion region, List<MeasureRegion> fitted) {
        return fitted.stream().filter(part -> sameRow(region, part)
                && part.left() >= region.left() - .006f && part.right() <= region.right() + .006f)
                .sorted(Comparator.comparingDouble(MeasureRegion::left)).toList();
    }
    private static List<Span> spans(List<ScoreNoteEvent> notes, int measure) {
        List<Span> result = new ArrayList<>();
        var lanes = notes.stream().filter(n -> n.measureIndex() == measure)
                .map(n -> n.staffCount() * 16 + n.staffIndex()).distinct().toList();
        for (int lane : lanes) {
            var voice = notes.stream().filter(n -> n.measureIndex() == measure
                    && n.staffCount() * 16 + n.staffIndex() == lane)
                    .sorted(Comparator.comparingDouble(ScoreNoteEvent::positionInMeasure)).toList();
            double beats = 0;
            int groups = 0;
            boolean beamed = true, valid = true;
            for (int start = 0; start < voice.size();) {
                int end = start;
                double duration = 0, rest = 0, leading = 0;
                while (end < voice.size() && voice.get(end).positionInMeasure()
                        - voice.get(start).positionInMeasure() <= .018f) {
                    var note = voice.get(end++);
                    double written = ScoreNoteTiming.writtenDurationBeats(note);
                    if (!Double.isFinite(written) || (note.articulations() & NoteOrnament.GRACE) != 0)
                        valid = false;
                    duration = Math.max(duration, written);
                    rest = Math.max(rest, note.followingRestBeats());
                    leading = Math.max(leading, note.leadingRestBeats());
                    beamed &= note.beamCount() > 0 && rest == 0 && leading == 0;
                }
                beats += duration + rest + (groups == 0 ? leading : 0);
                groups++;
                start = end;
            }
            if (valid) result.add(new Span(beats, groups, beamed));
        }
        return result;
    }
    private static boolean printedBarline(MeasureRegion region, float cut, byte[] gray, int width, int height) {
        if (gray == null || gray.length != width * height) return true;
        int top = Math.max(0, Math.round(region.top() * height));
        int bottom = Math.min(height - 1, Math.round(region.bottom() * height));
        int center = Math.round(cut * width), radius = Math.max(3, Math.round(width * .006f));
        for (int x = Math.max(0, center - radius); x <= Math.min(width - 1, center + radius); x++) {
            int ink = 0;
            for (int y = top; y <= bottom; y++) if ((gray[y * width + x] & 255) < 180) ink++;
            if (ink > (bottom - top + 1) * .55f) return true;
        }
        return false;
    }
}
