// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** Binds proved pedal hooks to written attacks or barlines, never proportional page spacing. */
final class PedalBracketAnchors {
    private PedalBracketAnchors() {}

    private record Endpoint(ScoreAnchor anchor, float visualX) {}

    /** Kind 0 is a barline, 1 a written attack column, 2 a rest column. */
    record Hook(int measure, int kind, float position, float tolerance) {
        Hook {
            if (measure < 0
                    || kind < 0
                    || kind > 2
                    || !Float.isFinite(position)
                    || position < 0
                    || position > 1
                    || !Float.isFinite(tolerance)
                    || tolerance < 0
                    || tolerance > .1f)
                throw new IllegalArgumentException("Invalid pedal hook column");
        }
    }

    static List<Hook> locate(
            ScorePageInterpretation score,
            PedalBracketDetector.Bracket bracket,
            List<PlayingTechniqueDetector.Staff> staffs,
            int width,
            int height) {
        var staff = owner(bracket, staffs);
        if (staff == null || width <= 0 || height <= 0) return List.of();
        var start = geometryEndpoint(score, bracket.left(), staff, width, height, true);
        var end = geometryEndpoint(score, bracket.right(), staff, width, height, false);
        if (start == null || end == null || start.measure() > end.measure()) return List.of();
        for (int m = start.measure(); m < end.measure(); m++)
            if (!ownedMeasure(score, m, staff, height)) return List.of();
        return List.of(start, end);
    }

    private static Hook geometryEndpoint(
            ScorePageInterpretation score,
            float x,
            PlayingTechniqueDetector.Staff staff,
            int width,
            int height,
            boolean start) {
        Hook best = null;
        float distance = Float.MAX_VALUE;
        boolean ambiguous = false;
        for (int m = 0; m < score.measures().size(); m++) {
            if (!ownedMeasure(score, m, staff, height)) continue;
            var region = score.measures().get(m);
            float left = region.left() * width, right = region.right() * width;
            if (!Float.isFinite(left)
                    || !Float.isFinite(right)
                    || right <= left
                    || x < left - staff.gap() * .65f
                    || x > right + staff.gap() * .65f) continue;
            float delta = Math.abs(x - (start ? left : right));
            if (delta <= staff.gap() * .65f) {
                var hook = new Hook(start ? m : m + 1, 0, 0, 0);
                if (delta < distance - .01f) {
                    best = hook;
                    distance = delta;
                    ambiguous = false;
                } else if (Math.abs(delta - distance) <= .01f && !hook.equals(best))
                    ambiguous = true;
                continue;
            }
            var candidates = new ArrayList<Hook>();
            float tolerance = Math.max(.000001f, .15f / (right - left));
            for (var note : score.notes())
                if (note.measureIndex() == m
                        && owned(note, staff, height)
                        && (note.articulations() & NoteOrnament.GRACE) == 0
                        && Float.isFinite(note.positionInMeasure()))
                    candidates.add(new Hook(m, 1, note.positionInMeasure(), tolerance));
            for (var rest : score.rests())
                if (rest.measureIndex() == m
                        && rest.staffIndex() == staff.index()
                        && rest.staffCount() == staff.count()
                        && Float.isFinite(rest.positionInMeasure()))
                    candidates.add(new Hook(m, 2, rest.positionInMeasure(), tolerance));
            for (var hook : candidates) {
                delta = Math.abs(left + hook.position() * (right - left) - x);
                if (delta > staff.gap() * .8f) continue;
                if (delta < distance - .15f) {
                    best = hook;
                    distance = delta;
                    ambiguous = false;
                } else if (Math.abs(delta - distance) <= .15f
                        && best != null
                        && (best.measure() != hook.measure()
                                || best.kind() != hook.kind()
                                || Math.abs(best.position() - hook.position()) > tolerance))
                    ambiguous = true;
            }
        }
        return ambiguous ? null : best;
    }

    static Optional<ScoreAnchor> anchor(
            Hook hook, ScorePageInterpretation score, int staff, int count, ScoreMeterMap meter) {
        if (hook.measure() > score.measures().size()) return Optional.empty();
        if (hook.kind() == 0) return Optional.of(new ScoreAnchor(hook.measure(), 0));
        if (hook.measure() == score.measures().size()) return Optional.empty();
        double beat = Double.NaN;
        float beats = meter.beatsInMeasure(hook.measure());
        if (hook.kind() == 1) {
            for (var note : score.notes())
                if (note.measureIndex() == hook.measure()
                        && note.staffIndex() == staff
                        && note.staffCount() == count
                        && (note.articulations() & NoteOrnament.GRACE) == 0
                        && Math.abs(note.positionInMeasure() - hook.position())
                                <= hook.tolerance()) {
                    var placed = ScoreNoteTiming.writtenPlacement(note, score.notes(), beats);
                    if (placed.isEmpty()) return Optional.empty();
                    double candidate = placed.get().onsetBeats();
                    if (Double.isFinite(beat) && Math.abs(beat - candidate) > .001)
                        return Optional.empty();
                    beat = candidate;
                }
        } else {
            for (var rest : score.rests())
                if (rest.measureIndex() == hook.measure()
                        && rest.staffIndex() == staff
                        && rest.staffCount() == count
                        && Math.abs(rest.positionInMeasure() - hook.position())
                                <= hook.tolerance()) {
                    double candidate =
                            ScoreRestFermataDetector.provedOnset(
                                    rest, score.rests(), score.notes(), beats);
                    if (!Double.isFinite(candidate)
                            || Double.isFinite(beat) && Math.abs(beat - candidate) > .001)
                        return Optional.empty();
                    beat = candidate;
                }
        }
        return !Double.isFinite(beat)
                        || beat < 0
                        || beat > meter.quarterBeatsInMeasure(hook.measure())
                ? Optional.empty()
                : Optional.of(
                        new ScoreAnchor(hook.measure(), beat)
                                .canonical(meter, score.measures().size()));
    }

    static List<ScoreExpressiveEvent> bind(
            ScorePageInterpretation score,
            List<PedalBracketDetector.Bracket> brackets,
            List<PlayingTechniqueDetector.Staff> staffs,
            int width,
            int height,
            ScoreMeterMap meter) {
        return bind(
                score,
                brackets,
                staffs,
                width,
                height,
                meter,
                0,
                score == null ? 0 : score.measures().size());
    }

    static List<ScoreExpressiveEvent> bind(
            ScorePageInterpretation score,
            List<PedalBracketDetector.Bracket> brackets,
            List<PlayingTechniqueDetector.Staff> staffs,
            int width,
            int height,
            ScoreMeterMap meter,
            int first,
            int afterLast) {
        if (score == null
                || brackets == null
                || staffs == null
                || meter == null
                || width <= 0
                || height <= 0
                || first < 0
                || afterLast > score.measures().size()
                || afterLast <= first) return List.of();
        var result = new ArrayList<ScoreExpressiveEvent>();
        var identities = new HashSet<String>();
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (var bracket : brackets) {
                var staff = owner(bracket, staffs);
                if (staff == null) continue;
                var start =
                        endpoint(
                                score,
                                bracket.left(),
                                staff,
                                width,
                                height,
                                meter,
                                true,
                                first,
                                afterLast);
                var end =
                        endpoint(
                                score,
                                bracket.right(),
                                staff,
                                width,
                                height,
                                meter,
                                false,
                                first,
                                afterLast);
                if (start == null || end == null || start.anchor().compareTo(end.anchor()) >= 0)
                    continue;
                // A rail cannot jump between systems or an unproved gap in its owned bars.
                boolean connected = true;
                for (int m = start.anchor().measureIndex(); m < end.anchor().measureIndex(); m++)
                    if (!ownedMeasure(score, m, staff, height)) {
                        connected = false;
                        break;
                    }
                if (!connected) continue;
                String id =
                        "printed-pedal:"
                                + bracket.staffCount()
                                + ":"
                                + bracket.staffIndex()
                                + ":"
                                + start.anchor()
                                + ":"
                                + end.anchor();
                if (!identities.add(id)) continue;
                var down =
                        new ScoreExpressiveEvent.Evidence(
                                "printed-pedal-bracket",
                                0,
                                start.visualX(),
                                staff.index(),
                                staff.count(),
                                "pedal down");
                var up =
                        new ScoreExpressiveEvent.Evidence(
                                "printed-pedal-bracket",
                                0,
                                end.visualX(),
                                staff.index(),
                                staff.count(),
                                "pedal up");
                result.add(
                        new ScoreExpressiveEvent(
                                id + ":down",
                                ScoreExpressiveEvent.Kind.PEDAL_DOWN,
                                Optional.of(start.anchor()),
                                Optional.of(end.anchor()),
                                ScoreExpressiveEvent.Scope.PART,
                                staff.index(),
                                staff.count(),
                                Optional.empty(),
                                ScoreExpressiveEvent.Strength.UNSPECIFIED,
                                "continuous bracket",
                                List.of(down)));
                result.add(
                        new ScoreExpressiveEvent(
                                id + ":up",
                                ScoreExpressiveEvent.Kind.PEDAL_UP,
                                Optional.of(end.anchor()),
                                Optional.empty(),
                                ScoreExpressiveEvent.Scope.PART,
                                staff.index(),
                                staff.count(),
                                Optional.empty(),
                                ScoreExpressiveEvent.Strength.UNSPECIFIED,
                                "continuous bracket",
                                List.of(up)));
            }
        }
        return List.copyOf(result);
    }

    static PlayingTechniqueDetector.Staff owner(
            PedalBracketDetector.Bracket bracket, List<PlayingTechniqueDetector.Staff> staffs) {
        if (bracket == null
                || !Float.isFinite(bracket.left())
                || !Float.isFinite(bracket.right())
                || bracket.left() < 0
                || bracket.right() <= bracket.left()
                || !Float.isFinite(bracket.baseline())
                || !Float.isFinite(bracket.gap())
                || bracket.gap() < 5) return null;
        PlayingTechniqueDetector.Staff best = null;
        float nearest = Float.MAX_VALUE;
        for (var staff : staffs) {
            if (staff.index() != bracket.staffIndex()
                    || staff.count() != bracket.staffCount()
                    || Math.abs(staff.gap() - bracket.gap()) > bracket.gap() * .2) continue;
            float distance = bracket.baseline() - staff.bottom();
            if (distance < bracket.gap() * .75 || distance > bracket.gap() * 16) continue;
            boolean intervening = false;
            for (var next : staffs)
                if (next.top() > staff.bottom() + bracket.gap() * .5
                        && next.top() <= bracket.baseline() + bracket.gap() * .3)
                    intervening = true;
            if (!intervening && distance < nearest) {
                best = staff;
                nearest = distance;
            }
        }
        return best;
    }

    private static boolean ownedMeasure(
            ScorePageInterpretation score,
            int measure,
            PlayingTechniqueDetector.Staff staff,
            int height) {
        if (measure < 0 || measure >= score.measures().size()) return false;
        var region = score.measures().get(measure);
        if ((staff.top() + staff.bottom()) * .5f / height < region.top() - staff.gap() / height
                || (staff.top() + staff.bottom()) * .5f / height
                        > region.bottom() + staff.gap() / height) return false;
        for (var note : score.notes())
            if (note.measureIndex() == measure && owned(note, staff, height)) return true;
        return false;
    }

    private static boolean owned(
            ScoreNoteEvent note, PlayingTechniqueDetector.Staff staff, int height) {
        return note.staffIndex() == staff.index()
                && note.staffCount() == staff.count()
                && !note.crossStaffBeam()
                && Float.isFinite(note.pageY())
                && note.pageY() > 0
                && Math.abs(
                                note.pageY() * height
                                        + note.staffStep() * staff.gap() * .5f
                                        - staff.bottom())
                        <= staff.gap() * 1.5f;
    }

    private static Endpoint endpoint(
            ScorePageInterpretation score,
            float x,
            PlayingTechniqueDetector.Staff staff,
            int width,
            int height,
            ScoreMeterMap meter,
            boolean start,
            int first,
            int afterLast) {
        if (!Float.isFinite(x) || x < 0 || x >= width) return null;
        ScoreAnchor accepted = null;
        float best = Float.MAX_VALUE;
        boolean ambiguous = false;
        for (int m = first; m < afterLast; m++) {
            if (!ownedMeasure(score, m, staff, height)) continue;
            var region = score.measures().get(m);
            float left = region.left() * width, right = region.right() * width;
            if (x < left - staff.gap() * .65f || x > right + staff.gap() * .65f) continue;
            // Hook tips align with the barline, while heads have a distinct inset.
            float boundaryDistance = Math.abs(x - (start ? left : right));
            if (boundaryDistance <= staff.gap() * .65f) {
                var anchor = new ScoreAnchor(start ? m : m + 1, 0);
                if (boundaryDistance < best - .01f) {
                    accepted = anchor;
                    best = boundaryDistance;
                    ambiguous = false;
                } else if (Math.abs(boundaryDistance - best) <= .01f && !anchor.equals(accepted))
                    ambiguous = true;
                continue;
            }
            double beat = Double.NaN;
            float nearest = Float.MAX_VALUE;
            boolean conflict = false;
            for (var note : score.notes()) {
                if (note.measureIndex() != m
                        || !owned(note, staff, height)
                        || (note.articulations() & NoteOrnament.GRACE) != 0) continue;
                float nx = left + note.positionInMeasure() * (right - left),
                        distance = Math.abs(nx - x);
                if (distance > staff.gap() * .8f || distance > nearest + .15f) continue;
                var placement =
                        ScoreNoteTiming.writtenPlacement(
                                note, score.notes(), meter.beatsInMeasure(m));
                if (placement.isEmpty()) continue;
                double candidate = placement.get().onsetBeats();
                if (distance < nearest - .15f) {
                    nearest = distance;
                    beat = candidate;
                    conflict = false;
                } else if (Math.abs(candidate - beat) > .001) conflict = true;
            }
            // Rest hooks use the same silent-slot resolver as the existing viewer.
            for (var rest : score.rests()) {
                if (rest.measureIndex() != m
                        || rest.staffIndex() != staff.index()
                        || rest.staffCount() != staff.count()) continue;
                float rx = left + rest.positionInMeasure() * (right - left),
                        distance = Math.abs(rx - x);
                if (distance > staff.gap() * .8f || distance > nearest + .15f) continue;
                double candidate =
                        ScoreRestFermataDetector.provedOnset(
                                rest, score.rests(), score.notes(), meter.beatsInMeasure(m));
                if (!Double.isFinite(candidate)) continue;
                if (distance < nearest - .15f) {
                    nearest = distance;
                    beat = candidate;
                    conflict = false;
                } else if (Math.abs(candidate - beat) > .001) conflict = true;
            }
            if (conflict
                    || !Double.isFinite(beat)
                    || beat < 0
                    || beat > meter.quarterBeatsInMeasure(m)) continue;
            var anchor = new ScoreAnchor(m, beat).canonical(meter, score.measures().size());
            if (nearest < best - .01f) {
                accepted = anchor;
                best = nearest;
                ambiguous = false;
            } else if (Math.abs(nearest - best) <= .01f && !anchor.equals(accepted))
                ambiguous = true;
        }
        return accepted == null || ambiguous ? null : new Endpoint(accepted, x / width);
    }
}
