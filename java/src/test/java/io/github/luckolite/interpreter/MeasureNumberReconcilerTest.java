// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MeasureNumberReconcilerTest {
    @Test public void partNumbersInsideConnectedSystemCannotInventExtraRows() {
        List<MeasureRegion> detected=new ArrayList<>();
        for(MeasureRegion r:row(.15f,7)) detected.add(new MeasureRegion(r.left(),r.right(),.15f,.34f));
        detected.addAll(row(.42f,7));
        detected.addAll(row(.54f,7));
        List<MeasureNumberReconciler.NumberToken> numbers=List.of(number(1,.17f),
                number(2,.25f),number(3,.31f),number(8,.42f),number(15,.54f));
        List<MeasureRegion> result=MeasureNumberReconciler.reconcile(detected,numbers);
        assertEquals(21,result.size());
        assertTrue(result.stream().noneMatch(r->r.top()>.16f && r.top()<.40f));
    }
    @Test public void restoresMissedMeasureFromPrintedSystemNumbers() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(0.20f, 4));
        detected.addAll(row(0.30f, 2));
        detected.addAll(row(0.40f, 3));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(21, 0.20f), number(25, 0.30f), number(28, 0.40f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(10, corrected.size());
        assertEquals(3, corrected.stream().filter(item -> item.top() == 0.30f).count());
    }

    @Test public void laterPrintedAnchorRecoversUnprintedMeasureOneAtPageStart() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 6));
        detected.addAll(row(.20f, 8));
        detected.addAll(row(.30f, 7));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(7, .20f), number(15, .30f));
        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(1, MeasureNumberReconciler.firstMeasureNumber(corrected, numbers));
    }

    @Test public void loneLaterAnchorCanProveAlternateArrangementRestartsAtOne() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 7));
        detected.addAll(row(.20f, 8));
        detected.addAll(row(.30f, 6));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(number(16, .30f));

        assertEquals(1, MeasureNumberReconciler.firstMeasureNumber(detected, numbers));
        assertEquals(0, MeasureNumberReconciler.firstMeasureNumber(detected,
                List.of(number(17, .30f))));
    }

    @Test public void continuationPageKeepsItsPrintedStartingMeasure() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 6));
        detected.addAll(row(.20f, 6));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(58, .10f), number(64, .20f));

        assertEquals(58, MeasureNumberReconciler.firstMeasureNumber(
                MeasureNumberReconciler.reconcile(detected, numbers), numbers));
    }

    @Test public void removesInventedStemBoundariesFromPrintedSystemNumbers() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(0.20f, 6));
        detected.addAll(row(0.30f, 3));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(number(31, 0.20f), number(34, 0.30f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(6, corrected.size());
        assertEquals(3, corrected.stream().filter(item -> item.top() == 0.20f).count());
    }

    @Test public void printedPatternAlsoCorrectsNoisyFinalNumberedSystem() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.20f, 4));
        detected.addAll(row(.30f, 4));
        detected.addAll(row(.40f, 12));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(12, .20f), number(16, .30f), number(20, .40f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(12, corrected.size());
        assertEquals(4, corrected.stream().filter(item -> item.top() == .40f).count());
    }

    @Test public void laterAnchorMayBeginADifferentWidthSystem() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 3));
        detected.addAll(row(.20f, 6));
        detected.addAll(row(.30f, 9));
        detected.addAll(row(.40f, 4));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(95, .20f), number(101, .30f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(22, corrected.size());
        assertEquals(9, corrected.stream().filter(item -> item.top() == .30f).count());
    }

    @Test public void splitsOneFullSystemBoxIntoItsFourPrintedMeasures() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.add(new MeasureRegion(0.1f, 0.9f, 0.20f, 0.28f));
        detected.addAll(row(0.30f, 4));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(17, 0.20f), number(21, 0.30f)));

        assertEquals(8, corrected.size());
        for (int index = 0; index < 4; index++) {
            MeasureRegion item = corrected.get(index);
            assertEquals(0.20f, item.top(), 0.0001f);
            assertTrue(item.right() - item.left() < 0.25f);
        }
    }

    @Test public void oneDetectedRegionSplitsThreeWaysWithoutStrandingHalfTheSystem() {
        float top = .20f;
        List<MeasureRegion> detected = new ArrayList<>();
        detected.add(region(.10f, .91f, top));
        detected.addAll(row(.30f, 3));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(27, top), number(30, .30f)));
        List<MeasureRegion> firstRow = corrected.stream()
                .filter(item -> item.top() == top).toList();

        assertEquals(3, firstRow.size());
        float firstWidth = firstRow.get(0).right() - firstRow.get(0).left();
        for (MeasureRegion measure : firstRow)
            assertEquals(firstWidth, measure.right() - measure.left(), .003f);
        assertEquals(.37f, firstRow.get(0).right(), .006f);
        assertEquals(.64f, firstRow.get(2).left(), .006f);
    }

    @Test public void printedSystemNumbersRecoverPageWhenStaffSegmentationIsEmpty() {
        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(List.of(), List.of(
                number(17, 0.20f), number(21, 0.30f), number(25, 0.40f)));

        assertEquals(12, corrected.size());
        assertEquals(0.195f, corrected.get(0).top(), 0.0001f);
        assertEquals(0.295f, corrected.get(4).top(), 0.0001f);
        assertEquals(0.395f, corrected.get(8).top(), 0.0001f);
    }

    @Test public void ignoresTimeSignatureAndImplausibleOcrJump() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(0.20f, 4));
        detected.addAll(row(0.30f, 4));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(number(4, 0.20f), number(95, 0.30f));

        assertEquals(8, MeasureNumberReconciler.reconcile(detected, numbers).size());
    }

    @Test public void fittedRegionsStayOrderedAndNonEmpty() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(0.20f, 2));
        detected.addAll(row(0.30f, 4));
        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(25, 0.20f), number(28, 0.30f)));
        for (int index = 0; index < 3; index++) {
            MeasureRegion item = corrected.get(index);
            assertTrue(item.left() < item.right());
            if (index > 0) assertTrue(corrected.get(index - 1).right() < item.left());
        }
    }

    @Test public void removesFalseStemLocallyWithoutEqualizingUnevenMeasures() {
        float top = .20f;
        List<MeasureRegion> detected = List.of(
                region(.10f, .196f, top), region(.204f, .336f, top),
                region(.344f, .446f, top), region(.454f, .576f, top),
                region(.584f, .716f, top), region(.724f, .90f, top));
        List<MeasureRegion> next = unevenRow(.30f);
        List<MeasureRegion> all = new ArrayList<>(detected);
        all.addAll(next);

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(all,
                List.of(number(72, top), number(77, .30f)));
        List<MeasureRegion> row = corrected.stream().filter(item -> item.top() == top).toList();

        assertEquals(5, row.size());
        assertEquals(.196f, row.get(0).right(), .0001f);
        assertEquals(.336f, row.get(1).right(), .0001f);
        assertEquals(.344f, row.get(2).left(), .0001f);
        assertEquals(.576f, row.get(2).right(), .0001f);
        assertEquals(.724f, row.get(4).left(), .0001f);
    }

    @Test public void highlyFragmentedRowDoesNotStrandATinyFinalMeasure() {
        float top = .20f;
        List<MeasureRegion> detected = new ArrayList<>(List.of(
                region(.10f, .20f, top), region(.205f, .37f, top),
                region(.378f, .60f, top), region(.605f, .767f, top),
                region(.775f, .82f, top), region(.825f, .90f, top)));
        detected.addAll(row(.30f, 3));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(21, top), number(24, .30f)));
        List<MeasureRegion> firstRow = corrected.stream()
                .filter(item -> item.top() == top).toList();

        assertEquals(3, firstRow.size());
        assertTrue(firstRow.stream().allMatch(item -> item.right() - item.left() > .16f));
    }

    @Test public void insertsMissingBoundaryOnlyInsideWidestMergedMeasure() {
        float top = .20f;
        List<MeasureRegion> detected = new ArrayList<>(List.of(
                region(.10f, .196f, top), region(.204f, .336f, top),
                region(.344f, .716f, top), region(.724f, .90f, top)));
        detected.addAll(unevenRow(.30f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(72, top), number(77, .30f)));
        List<MeasureRegion> row = corrected.stream().filter(item -> item.top() == top).toList();

        assertEquals(5, row.size());
        assertEquals(.196f, row.get(0).right(), .0001f);
        assertEquals(.204f, row.get(1).left(), .0001f);
        assertEquals(.336f, row.get(1).right(), .0001f);
        assertEquals(.724f, row.get(4).left(), .0001f);
    }

    @Test public void printedFiveMeasureWholeNoteRowOverridesDominantThreeMeasureRows() {
        List<MeasureRegion> detected = new ArrayList<>();
        List<MeasureNumberReconciler.NumberToken> numbers = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            float top = .10f + index * .10f;
            detected.addAll(row(top, 3));
            numbers.add(number(57 + index * 3, top));
        }
        detected.addAll(unevenRow(.60f));
        numbers.add(number(72, .60f));
        detected.addAll(row(.70f, 3));
        numbers.add(number(77, .70f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(5, corrected.stream().filter(item -> item.top() == .60f).count());
    }

    @Test public void fourMeasureRestDoesNotSplitLongWrittenMeasure() {
        float firstTop = .20f;
        MeasureRegion longWrittenMeasure = region(.10f, .39f, firstTop);
        MeasureRegion fourMeasureRest = region(.398f, .548f, firstTop);
        MeasureRegion measure21 = region(.556f, .718f, firstTop);
        MeasureRegion measure22 = region(.726f, .90f, firstTop);
        List<MeasureRegion> detected = new ArrayList<>(List.of(longWrittenMeasure,
                fourMeasureRest, measure21, measure22));
        detected.addAll(row(.30f, 4));
        detected.addAll(row(.40f, 4));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(16, firstTop), number(23, .30f), number(27, .40f));
        MeasureNumberReconciler.NumberToken restCount =
                new MeasureNumberReconciler.NumberToken(4, .46f, .21f, .48f, .23f);

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers,
                List.of(restCount));
        List<MeasureRegion> firstRow = corrected.stream()
                .filter(item -> item.top() == firstTop).toList();

        assertEquals(7, firstRow.size());
        assertEquals(longWrittenMeasure, firstRow.get(0));
        for (int index = 1; index <= 4; index++) assertEquals(fourMeasureRest, firstRow.get(index));
        assertEquals(measure21, firstRow.get(5));
        assertEquals(measure22, firstRow.get(6));
    }

    @Test public void loneSystemNumberDoesNotDiscardDetectedMultiMeasureRest() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 6));
        detected.addAll(row(.20f, 5));
        detected.addAll(row(.30f, 5));
        MeasureNumberReconciler.NumberToken twoRest =
                new MeasureNumberReconciler.NumberToken(2, .48f, .21f, .50f, .23f);

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected,
                List.of(number(13, .30f), twoRest), List.of(twoRest));

        assertEquals(17, corrected.size());
        assertEquals(6, corrected.stream().filter(item -> item.top() == .20f).count());
        assertEquals(6, corrected.stream().filter(item -> item.top() == .10f).count());
        assertEquals(5, corrected.stream().filter(item -> item.top() == .30f).count());
    }

    @Test public void repairsTinyStemFragmentEvenWhenPrintedCountAlreadyMatches() {
        float top = .20f;
        List<MeasureRegion> detected = new ArrayList<>(List.of(
                region(.10f, .238f, top), region(.244f, .264f, top),
                region(.270f, .382f, top), region(.388f, .50f, top),
                region(.506f, .618f, top), region(.624f, .736f, top),
                region(.742f, .90f, top)));
        detected.addAll(row(.30f, 7));
        detected.addAll(row(.40f, 7));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, List.of(
                number(53, top), number(60, .30f), number(67, .40f)));
        List<MeasureRegion> firstRow = corrected.stream()
                .filter(item -> item.top() == top).toList();

        assertEquals(7, firstRow.size());
        assertTrue(firstRow.stream().noneMatch(item -> item.right() - item.left() < .04f));
        assertEquals(.10f, firstRow.get(0).left(), .0001f);
        assertEquals(.244f, firstRow.get(1).left(), .0001f);
        assertEquals(.382f, firstRow.get(1).right(), .0001f);
    }

    @Test public void dominantSystemCountRemovesTinyStemMeasuresWithoutPrintedNumbers() {
        List<MeasureRegion> detected = regularPage(5, 3);
        float top = .70f;
        detected.add(new MeasureRegion(.10f, .16f, top, top + .06f));
        detected.add(new MeasureRegion(.164f, .36f, top, top + .06f));
        detected.add(new MeasureRegion(.364f, .62f, top, top + .06f));
        detected.add(new MeasureRegion(.624f, .896f, top, top + .06f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, List.of());

        assertEquals(3, corrected.stream().filter(item -> item.top() == top).count());
        assertEquals(18, corrected.size());
    }

    @Test public void tempoAndRestCountAreNotMistakenForSystemNumberAnchors() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 11));
        detected.addAll(row(.20f, 8));
        detected.addAll(row(.30f, 6));
        detected.addAll(row(.40f, 3));
        MeasureNumberReconciler.NumberToken tempo =
                new MeasureNumberReconciler.NumberToken(126, .13f, .09f, .19f, .12f);
        MeasureNumberReconciler.NumberToken restCount =
                new MeasureNumberReconciler.NumberToken(4, .18f, .11f, .21f, .13f);
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(tempo, restCount,
                number(15, .20f), number(21, .30f), number(24, .40f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers,
                List.of(restCount));

        assertEquals(26, corrected.size());
        assertEquals(14, corrected.stream().filter(item -> item.top() == .10f).count());
        assertEquals(6, corrected.stream().filter(item -> item.top() == .20f).count());
        assertEquals(3, corrected.stream().filter(item -> item.top() == .30f).count());
        assertEquals(1, MeasureNumberReconciler.firstMeasureNumber(corrected, numbers));
    }

    @Test public void dominantCountDoesNotCollapseACompletelyDifferentBusySystem() {
        List<MeasureRegion> detected = regularPage(5, 3);
        float top = .70f;
        detected.add(new MeasureRegion(.10f, .13f, top, top + .06f));
        detected.addAll(row(top, 7).stream()
                .map(item -> new MeasureRegion(item.left() + .035f, item.right(),
                        item.top(), item.bottom())).toList());

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, List.of());

        assertEquals(8, corrected.stream().filter(item -> item.top() == top).count());
    }

    @Test public void dominantSystemCountSplitsOneImplausiblyWideMergedMeasure() {
        List<MeasureRegion> detected = regularPage(5, 3);
        float top = .70f;
        detected.add(new MeasureRegion(.10f, .35f, top, top + .06f));
        detected.add(new MeasureRegion(.354f, .896f, top, top + .06f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, List.of());

        assertEquals(3, corrected.stream().filter(item -> item.top() == top).count());
        assertEquals(18, corrected.size());
    }

    @Test public void naturallyShortFinalSystemIsNotExpandedToTheDominantCount() {
        List<MeasureRegion> detected = regularPage(5, 3);
        float top = .70f;
        detected.add(new MeasureRegion(.10f, .34f, top, top + .06f));
        detected.add(new MeasureRegion(.344f, .58f, top, top + .06f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, List.of());

        assertEquals(2, corrected.stream().filter(item -> item.top() == top).count());
        assertEquals(17, corrected.size());
    }

    @Test public void missingHunterRowsAreRecoveredFromTheFullPrintedSequence() {
        List<MeasureRegion> detected = new ArrayList<>();
        detected.addAll(row(.10f, 1));
        detected.addAll(row(.19f, 1));
        detected.addAll(row(.36f, 1));
        detected.addAll(row(.45f, 1));
        detected.addAll(row(.54f, 6));
        detected.addAll(row(.63f, 3));
        detected.addAll(row(.71f, 2));
        detected.addAll(row(.88f, 1));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(2, .05f), number(87, .10f), number(28, .10f), number(31, .19f),
                number(34, .28f), number(37, .36f), number(39, .45f), number(42, .54f),
                number(47, .63f), number(49, .71f), number(51, .79f), number(53, .88f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(28, corrected.size());
        assertEquals(28, MeasureNumberReconciler.firstMeasureNumber(corrected, numbers));
        assertEquals(3, corrected.stream().filter(item -> item.top() == .10f).count());
        assertEquals(3, corrected.stream().filter(item -> Math.abs(item.top() - .275f) < .001f).count());
        assertEquals(3, corrected.stream().filter(item -> item.top() == .88f).count());
    }

    @Test public void sparseAndDenseAtonementSystemsKeepPrintedCounts() {
        List<MeasureRegion> detected = new ArrayList<>();
        int[] rawCounts = {1, 11, 2, 2, 5, 2, 1, 1, 1, 1};
        int[] printed = {1, 13, 27, 40, 49, 54, 56, 57, 58, 59};
        List<MeasureNumberReconciler.NumberToken> numbers = new ArrayList<>();
        for (int index = 0; index < rawCounts.length; index++) {
            float top = .08f + index * .085f;
            detected.addAll(row(top, rawCounts[index]));
            numbers.add(number(printed[index], top));
        }

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(59, corrected.size());
        for (int index = 0; index < rawCounts.length; index++) {
            float top = .08f + index * .085f;
            int expected = index + 1 < printed.length ? printed[index + 1] - printed[index] : 1;
            assertEquals(expected, corrected.stream().filter(item -> item.top() == top).count());
        }
    }

    @Test public void ambiguousSameRowOcrUsesDetectedCountAsTieBreaker() {
        List<MeasureRegion> detected = new ArrayList<>();
        for (int index = 0; index < 6; index++) detected.addAll(row(.10f + index * .10f, 4));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(6, .30f), number(9, .30f), number(13, .40f),
                number(16, .50f), number(19, .60f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(4, corrected.stream().filter(item -> item.top() == .30f).count());
        assertEquals(1, MeasureNumberReconciler.firstMeasureNumber(corrected, numbers));
    }

    @Test public void strayHumoresqueNineCannotExpandThreeMeasuresIntoEleven() {
        List<MeasureRegion> detected = new ArrayList<>();
        float[] tops = {.07f, .16f, .25f, .34f, .43f, .52f, .61f, .70f, .79f, .88f};
        for (float top : tops) detected.addAll(row(top, 4));
        List<MeasureNumberReconciler.NumberToken> numbers = List.of(
                number(4, .16f), number(8, .25f), number(12, .34f),
                number(9, .43f), number(20, .52f), number(24, .61f),
                number(28, .70f), number(32, .79f));

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers);

        assertEquals(4, corrected.stream().filter(item -> item.top() == .43f).count());
        assertEquals(40, corrected.size());
    }

    @Test public void finalArtemisFourMeasureRestExtendsInferredVisualCount() {
        List<MeasureRegion> detected = new ArrayList<>();
        List<MeasureNumberReconciler.NumberToken> numbers = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            float top = .10f + index * .10f;
            detected.addAll(row(top, index == 2 ? 6 : index == 4 ? 4 : 3));
            numbers.add(number(new int[]{44, 47, 50, 58, 61, 65}[index], top));
        }
        float finalTop = .70f;
        detected.add(region(.10f, .90f, finalTop));
        numbers.add(number(68, finalTop));
        MeasureNumberReconciler.NumberToken fourRest =
                new MeasureNumberReconciler.NumberToken(4, .86f, .66f, .87f, .69f);

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers,
                List.of(fourRest));

        assertEquals(7, corrected.stream().filter(item -> item.top() == finalTop).count());
        assertEquals(31, corrected.size());
    }

    @Test public void finalMultiMeasureRestInOrdinaryLastSlotDoesNotAddAVisualMeasure() {
        List<MeasureRegion> detected = new ArrayList<>();
        List<MeasureNumberReconciler.NumberToken> numbers = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            float top = .10f + index * .10f;
            detected.addAll(row(top, 3));
            numbers.add(number(10 + index * 3, top));
        }
        float finalTop = .70f;
        detected.add(region(.10f, .90f, finalTop));
        numbers.add(number(28, finalTop));
        MeasureNumberReconciler.NumberToken fourRest =
                new MeasureNumberReconciler.NumberToken(4, .76f, .66f, .78f, .69f);

        List<MeasureRegion> corrected = MeasureNumberReconciler.reconcile(detected, numbers,
                List.of(fourRest));

        assertEquals(6, corrected.stream().filter(item -> item.top() == finalTop).count());
    }

    private static List<MeasureRegion> regularPage(int rows, int measuresPerRow) {
        List<MeasureRegion> result = new ArrayList<>();
        for (int index = 0; index < rows; index++)
            result.addAll(row(.10f + index * .10f, measuresPerRow));
        return result;
    }

    private static List<MeasureRegion> row(float top, int count) {
        List<MeasureRegion> result = new ArrayList<>();
        float width = 0.8f / count;
        for (int index = 0; index < count; index++)
            result.add(new MeasureRegion(0.1f + width * index, 0.1f + width * (index + 1) - 0.004f,
                    top, top + 0.08f));
        return result;
    }

    private static List<MeasureRegion> unevenRow(float top) {
        return List.of(region(.10f, .196f, top), region(.204f, .336f, top),
                region(.344f, .576f, top), region(.584f, .716f, top),
                region(.724f, .90f, top));
    }

    private static MeasureRegion region(float left, float right, float top) {
        return new MeasureRegion(left, right, top, top + .08f);
    }

    private static MeasureNumberReconciler.NumberToken number(int value, float rowTop) {
        return new MeasureNumberReconciler.NumberToken(value, 0.09f, rowTop - 0.005f,
                0.11f, rowTop + 0.012f);
    }
}
