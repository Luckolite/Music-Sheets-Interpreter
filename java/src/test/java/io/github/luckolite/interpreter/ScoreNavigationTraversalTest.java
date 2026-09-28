// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.AfterJumpRepeats.*;

/** Original logical fixtures, not score scans or title-specific production exceptions. */
public class ScoreNavigationTraversalTest {
    private static ScorePlaybackDirection mark(
            int at,
            ScorePlaybackDirection.Kind kind,
            String id,
            String target,
            String coda,
            String group,
            int plays,
            List<Integer> passes,
            Integer end,
            ScorePlaybackDirection.AfterJumpRepeats policy) {
        return new ScorePlaybackDirection(
                at,
                kind,
                new ScorePlaybackDirection.Details(
                        0,
                        id,
                        target,
                        coda,
                        group,
                        plays,
                        passes,
                        end == null ? Optional.empty() : Optional.of(new ScoreAnchor(end, 0)),
                        policy,
                        "",
                        List.of()));
    }

    private static ScorePlaybackDirection d(int at, ScorePlaybackDirection.Kind kind) {
        return new ScorePlaybackDirection(at, kind);
    }

    private static ScorePlaybackDirection named(
            int at, ScorePlaybackDirection.Kind kind, String id, String target) {
        return mark(at, kind, id, target, "", "", 2, List.of(), null, DEFAULT);
    }

    private static void repeat(
            List<ScorePlaybackDirection> marks, Integer first, int end, int plays, String id) {
        if (first != null)
            marks.add(
                    mark(
                            first,
                            REPEAT_START,
                            id + ":start",
                            "",
                            "",
                            id,
                            plays,
                            List.of(),
                            null,
                            DEFAULT));
        marks.add(mark(end, REPEAT_END, id + ":end", "", "", id, plays, List.of(), null, DEFAULT));
    }

    private static ScoreNavigationTraversal.Result route(
            int count, List<ScorePlaybackDirection> marks) {
        return ScoreNavigationTraversal.traverse(count, new ScoreMeterMap(4, List.of()), marks);
    }

    public static Map<String, List<Integer>> oracleRoutes() {
        var result = new LinkedHashMap<String, List<Integer>>();
        var m = new ArrayList<ScorePlaybackDirection>();
        repeat(m, 1, 4, 2, "a");
        result.put("R1", route(8, m).sourceMeasures());
        m = new ArrayList<>();
        repeat(m, null, 3, 3, "a");
        result.put("R2", route(5, m).sourceMeasures());
        m = new ArrayList<>();
        repeat(m, 0, 4, 2, "outer");
        repeat(m, 1, 3, 2, "inner");
        result.put("R3", route(5, m).sourceMeasures());
        m = new ArrayList<>();
        repeat(m, 0, 3, 2, "a");
        m.add(mark(2, ENDING, "first", "", "", "a", 2, List.of(1), 3, DEFAULT));
        m.add(mark(3, ENDING, "second", "", "", "a", 2, List.of(2), 4, DEFAULT));
        result.put("R4", route(6, m).sourceMeasures());
        result.put("R5", route(8, List.of(d(6, DA_CAPO_AL_FINE), d(3, FINE))).sourceMeasures());
        result.put(
                "R6",
                route(
                                8,
                                List.of(
                                        named(2, SEGNO, "s", ""),
                                        named(6, DAL_SEGNO_AL_FINE, "jump", "s"),
                                        d(4, FINE)))
                        .sourceMeasures());
        result.put(
                "R7",
                route(
                                10,
                                List.of(
                                        mark(
                                                7,
                                                DA_CAPO_AL_CODA,
                                                "jump",
                                                "",
                                                "c",
                                                "",
                                                2,
                                                List.of(),
                                                null,
                                                DEFAULT),
                                        named(3, TO_CODA, "to", "c"),
                                        named(8, CODA, "c", "")))
                        .sourceMeasures());
        result.put(
                "R8",
                route(8, List.of(named(2, SEGNO, "s", ""), named(6, DAL_SEGNO, "jump", "s")))
                        .sourceMeasures());
        m = new ArrayList<>();
        repeat(m, 0, 2, 2, "a");
        repeat(m, 2, 4, 2, "b");
        result.put("R15", route(6, m).sourceMeasures());
        m = new ArrayList<>();
        repeat(m, 1, 3, 1, "a");
        result.put("R16", route(5, m).sourceMeasures());
        for (var policy : List.of(SKIP, PLAY)) {
            m = new ArrayList<>();
            repeat(m, 0, 2, 2, "a");
            m.add(d(3, FINE));
            m.add(mark(5, DA_CAPO_AL_FINE, "jump", "", "", "", 2, List.of(), null, policy));
            result.put(policy == SKIP ? "R17" : "R18", route(6, m).sourceMeasures());
        }
        result.put("R19", route(0, List.of()).sourceMeasures());
        return result;
    }

    /** Print only actual production-engine routes for an independent expectation checker. */
    public static void main(String[] args) {
        var json = new StringBuilder("{\"routes\":{");
        boolean first = true;
        for (var entry : oracleRoutes().entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        System.out.println(json.append("}}"));
    }

    @Test
    public void exactRepeatEndingAndJumpRoutes() {
        var actual = oracleRoutes();
        assertEquals(List.of(0, 1, 2, 3, 1, 2, 3, 4, 5, 6, 7), actual.get("R1"));
        assertEquals(List.of(0, 1, 2, 0, 1, 2, 0, 1, 2, 3, 4), actual.get("R2"));
        assertEquals(List.of(0, 1, 2, 1, 2, 3, 0, 1, 2, 1, 2, 3, 4), actual.get("R3"));
        assertEquals(List.of(0, 1, 2, 0, 1, 3, 4, 5), actual.get("R4"));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 0, 1, 2), actual.get("R5"));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 2, 3), actual.get("R6"));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 0, 1, 2, 8, 9), actual.get("R7"));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 2, 3, 4, 5, 6, 7), actual.get("R8"));
        assertEquals(List.of(0, 1, 0, 1, 2, 3, 2, 3, 4, 5), actual.get("R15"));
        assertEquals(List.of(0, 1, 2, 3, 4), actual.get("R16"));
        assertEquals(List.of(0, 1, 0, 1, 2, 3, 4, 0, 1, 2), actual.get("R17"));
        assertEquals(List.of(0, 1, 0, 1, 2, 3, 4, 0, 1, 0, 1, 2), actual.get("R18"));
        assertEquals(List.of(), actual.get("R19"));
    }

    @Test
    public void midbarFineRetainsOnlyPrefixInMusicalBeats() {
        var details =
                new ScorePlaybackDirection.Details(
                        1.5,
                        "fine",
                        "",
                        "",
                        "",
                        2,
                        List.of(),
                        Optional.empty(),
                        DEFAULT,
                        "",
                        List.of());
        var result =
                route(
                        4,
                        List.of(
                                d(3, DA_CAPO_AL_FINE),
                                new ScorePlaybackDirection(1, FINE, details)));
        assertTrue(result.complete());
        assertEquals(17.5, result.performedBeats(), 0);
        var last = result.occurrences().get(result.occurrences().size() - 1);
        assertEquals(new ScoreAnchor(1, 1.5), last.end());
    }

    @Test
    public void equivalentCanonicalEndingEndpointsAreRejected() {
        var details =
                new ScorePlaybackDirection.Details(
                        4,
                        "bad",
                        "",
                        "",
                        "a",
                        2,
                        List.of(1),
                        Optional.of(new ScoreAnchor(1, 0)),
                        DEFAULT,
                        "",
                        List.of());
        var result = route(3, List.of(new ScorePlaybackDirection(0, ENDING, details)));
        assertEquals(List.of(0, 1, 2), result.sourceMeasures());
        assertEquals("INVALID_ANCHOR", result.diagnostics().get(0).code());
    }

    @Test
    public void namedDestinationsDoNotBecomeAmbiguous() {
        var result =
                route(
                        6,
                        List.of(
                                named(1, SEGNO, "a", ""),
                                named(2, SEGNO, "b", ""),
                                named(4, DAL_SEGNO, "jump", "a")));
        assertEquals(List.of(0, 1, 2, 3, 1, 2, 3, 4, 5), result.sourceMeasures());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    public void percentShorthandIsExplicitlyUnsupportedNotSilentlyAccepted() {
        var result = route(3, List.of(d(1, MEASURE_REPEAT)));
        assertEquals("UNSUPPORTED_MEASURE_REPEAT", result.diagnostics().get(0).code());
    }

    @Test
    public void sourceIdentityAndEndingEndpointSurviveOffset() {
        var direction = mark(2, ENDING, "e", "", "", "r", 2, List.of(1), 3, DEFAULT).offset(7);
        assertEquals("e", direction.details().eventId());
        assertEquals(new ScoreAnchor(10, 0), direction.details().end().orElseThrow());
    }

    @Test
    public void newKindIdsNeverChangeLegacyWireMeaning() {
        assertEquals(SEGNO, ScorePlaybackDirection.Kind.fromWireId(0));
        assertEquals(CODA, ScorePlaybackDirection.Kind.fromWireId(3));
        for (var kind : ScorePlaybackDirection.Kind.values())
            assertEquals(kind, ScorePlaybackDirection.Kind.fromWireId(kind.wireId()));
    }

    @Test
    public void conflictingIdentityNeverSelectsFirstInputDestination() {
        var a = named(1, SEGNO, "s", "");
        var b = named(2, SEGNO, "s", "");
        var jump = named(6, DAL_SEGNO, "jump", "s");
        var first = route(8, List.of(a, b, jump));
        var reversed = route(8, List.of(b, a, jump));
        assertEquals(first.sourceMeasures(), reversed.sourceMeasures());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), first.sourceMeasures());
        assertTrue(
                first.diagnostics().stream()
                        .anyMatch(d -> d.code().equals("CONFLICTING_IDENTITY")));
    }

    @Test
    public void parallelStaffEvidenceDoesNotCreateConflictingNavigation() {
        var a = named(2, SEGNO, "s", "");
        var base = a.details();
        var evidence = new ScoreExpressiveEvent.Evidence("page", 0, .2f, 1, 2, "segno");
        var b =
                new ScorePlaybackDirection(
                        2,
                        SEGNO,
                        new ScorePlaybackDirection.Details(
                                0,
                                "s",
                                "",
                                "",
                                "",
                                2,
                                List.of(),
                                Optional.empty(),
                                DEFAULT,
                                "Segno",
                                List.of(evidence)));
        var result = route(6, List.of(a, b, named(4, DAL_SEGNO, "jump", "s")));
        assertEquals(List.of(0, 1, 2, 3, 2, 3, 4, 5), result.sourceMeasures());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    public void richAnonymousNavigationCannotAliasDistinctRepeatGroups() {
        try {
            mark(3, REPEAT_END, "", "", "", "inner", 2, List.of(), null, DEFAULT);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void threeEndingPassesUseOnlyThePerformedEndingBackwardEdge() {
        var marks = new ArrayList<ScorePlaybackDirection>();
        repeat(marks, 0, 3, 3, "a");
        marks.add(mark(4, REPEAT_END, "second-end", "", "", "a", 3, List.of(), null, DEFAULT));
        marks.add(mark(2, ENDING, "first", "", "", "a", 3, List.of(1), 3, DEFAULT));
        marks.add(mark(3, ENDING, "second", "", "", "a", 3, List.of(2), 4, DEFAULT));
        marks.add(mark(4, ENDING, "third", "", "", "a", 3, List.of(3), 5, DEFAULT));
        var result = route(6, marks);
        assertEquals(List.of(0, 1, 2, 0, 1, 3, 0, 1, 4, 5), result.sourceMeasures());
        assertTrue(result.complete());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    public void incompatibleSameAnchorReturnsAreDiagnosedNotExecutedSerially() {
        var result =
                route(
                        8,
                        List.of(
                                named(2, SEGNO, "s", ""),
                                named(6, DA_CAPO, "dc", ""),
                                named(6, DAL_SEGNO, "ds", "s")));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), result.sourceMeasures());
        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(d -> d.code().equals("CONFLICTING_RETURNS")));
    }

    @Test
    public void semanticallyIdenticalReturnAliasesExecuteOnlyOnce() {
        var result =
                route(
                        8,
                        List.of(
                                named(2, SEGNO, "s", ""),
                                named(6, DAL_SEGNO, "ds", "s"),
                                named(6, DAL_SEGNO, "other-staff", "s")));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 2, 3, 4, 5, 6, 7), result.sourceMeasures());
        assertTrue(result.diagnostics().isEmpty());
    }
}
