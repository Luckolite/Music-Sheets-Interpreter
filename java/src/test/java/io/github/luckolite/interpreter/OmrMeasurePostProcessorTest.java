// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class OmrMeasurePostProcessorTest {
    @Test public void turnsOmrStaffAndBarLabelsIntoFourMeasures() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, true);
        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 500);
        assertEquals(4, measures.size());
        assertTrue(measures.get(0).left() < measures.get(0).right());
        assertTrue(measures.get(0).top() < measures.get(0).bottom());
    }

    @Test public void keepsSemanticStaffExtentAsOneMeasureWhenNoInternalBarlineExists() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, false);
        assertEquals(1, OmrMeasurePostProcessor.process(page, 600, 500).size());
    }

    @Test public void staffLabelNoiseBetweenLinesCannotMergeTheFiveSemanticPeaks() {
        int width = 600, height = 500, top = 100;
        byte[] page = page(width, height);
        staff(page, width, top, true);
        // A faint scan can make the model label notation through every interline space as staff.
        // The five real lines remain much stronger local peaks and must still define the system.
        for (int y = top; y <= top + 32; y++) for (int x = 90; x <= 120; x++)
            set(page, width, x, y, OmrMeasurePostProcessor.STAFF);

        assertEquals(4, OmrMeasurePostProcessor.process(page, width, height).size());
    }

    @Test public void pageWithoutSemanticStaffRemainsEmpty() {
        assertTrue(OmrMeasurePostProcessor.process(page(600, 500), 600, 500).isEmpty());
    }

    @Test public void genericSymbolMaskCanRestoreShortenedBarlines() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, false);
        for (int x : new int[]{60, 180, 300, 420, 540})
            for (int y = 101; y <= 131; y++)
                set(page, 600, x, y, OmrMeasurePostProcessor.SYMBOL);
        assertEquals(4, OmrMeasurePostProcessor.process(page, 600, 500).size());
    }

    @Test public void keepsSeparateSystemsInReadingOrder() {
        byte[] page = page(600, 700);
        staff(page, 600, 100, true);
        staff(page, 600, 240, true);
        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 700);
        assertEquals(8, measures.size());
        assertTrue(measures.get(3).top() < measures.get(4).top());
    }

    @Test public void alignedGrandStaffIsCountedOnce() {
        byte[] page = page(600, 700);
        staff(page, 600, 100, true);
        staff(page, 600, 180, true);
        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 700);
        assertEquals(4, measures.size());
        assertTrue(measures.get(0).bottom() > 0.28f);
    }

    @Test public void compactAlignedViolinSystemsStaySeparate() {
        byte[] page = page(600, 900);
        for (int top = 80; top <= 724; top += 92) staff(page, 600, top, false);

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 900);

        assertEquals(8, measures.size());
        for (int index = 0; index + 1 < measures.size(); index++)
            assertTrue(measures.get(index).top() < measures.get(index + 1).top());
    }

    @Test public void alignedStavesContributeComplementaryBarlines() {
        byte[] page = page(600, 700);
        staffWithBars(page, 600, 100, new int[]{60, 180, 420, 540});
        staffWithBars(page, 600, 180, new int[]{60, 180, 300, 540});

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 700);

        assertEquals(4, measures.size());
        assertTrue(measures.get(1).right() < measures.get(2).right());
    }

    @Test public void printedConnectorMergesWideDuetStavesWithDifferentBarlineCounts() {
        int width = 600, height = 500;
        byte[] labels = page(width, height), gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        staffWithBars(labels, width, 100, new int[]{60, 180, 300, 420, 540});
        staffWithBars(labels, width, 200, new int[]{60, 300, 540});
        rawStaffAndBars(gray, width, 100, new int[]{60, 180, 300, 420, 540});
        rawStaffAndBars(gray, width, 200, new int[]{60, 300, 540});
        // The left bracket/shared opening barline joins the two printed parts even though one
        // staff's segmentation omitted two interior boundaries.
        for (int y = 97; y <= 235; y++) gray[y * width + 60] = 0;

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(
                labels, gray, width, height);

        assertEquals(4, measures.size());
        assertTrue(measures.get(0).bottom() > .45f);
    }

    @Test public void equallySpacedUnconnectedRowsRemainSequentialSystems() {
        int width = 600, height = 500;
        byte[] labels = page(width, height), gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        staffWithBars(labels, width, 100, new int[]{60, 180, 300, 420, 540});
        staffWithBars(labels, width, 200, new int[]{60, 300, 540});
        rawStaffAndBars(gray, width, 100, new int[]{60, 180, 300, 420, 540});
        rawStaffAndBars(gray, width, 200, new int[]{60, 300, 540});

        assertEquals(6, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void shadedPaperDoesNotConnectSeparateSystemsButRealInkStillDoes() {
        int width = 600, height = 500;
        byte[] labels = page(width, height), gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 185);
        staffWithBars(labels, width, 100, new int[]{60, 180, 300, 420, 540});
        staffWithBars(labels, width, 200, new int[]{60, 300, 540});
        rawStaffAndBars(gray, width, 100, new int[]{60, 180, 300, 420, 540});
        rawStaffAndBars(gray, width, 200, new int[]{60, 300, 540});
        assertEquals(6, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
        for (int y = 97; y <= 235; y++) gray[y * width + 60] = 0;
        assertEquals(4, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void semanticNoteheadRejectsAStemAsABarline() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, true);
        for (int y = 97; y <= 135; y++) set(page, 600, 250, y, OmrMeasurePostProcessor.STEM_OR_REST);
        for (int y = 114; y <= 120; y++) for (int x = 243; x <= 257; x++)
            set(page, 600, x, y, OmrMeasurePostProcessor.NOTEHEAD);
        assertEquals(4, OmrMeasurePostProcessor.process(page, 600, 500).size());
    }

    @Test public void rawStaffSpanRejectsHeadlessStemAsBarline() {
        int width = 600, height = 500;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 255);
        staff(labels, width, 100, true);
        for (int x : new int[]{60, 180, 300, 420, 540})
            for (int y = 97; y <= 135; y++) gray[y * width + x] = 0;
        for (int y = 97; y <= 135; y++)
            set(labels, width, 250, y, OmrMeasurePostProcessor.STEM_OR_REST);
        // The semantic model missed this notehead, but raw ink shows that its stem does not
        // actually cross both outer staff lines.
        for (int y = 112; y <= 135; y++) gray[y * width + 250] = 0;

        assertEquals(4, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void faintScanStillValidatesSemanticBarlines() {
        int width = 600, height = 500, top = 100;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 255);
        staff(labels, width, top, true);
        rawStaff(gray, width, top);
        for (int x : new int[]{60, 180, 300, 420, 540})
            for (int y = top - 3; y <= top + 35; y++) gray[y * width + x] = (byte) 195;

        assertEquals(4, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void upperPositionHeadsKeepTheirStemsFromBecomingBarlines() {
        int width = 900, height = 360, top = 150;
        byte[] labels = page(width, height);
        for (int line = 0; line < 5; line++)
            for (int x = 50; x <= 850; x++)
                set(labels, width, x, top + line * 10, OmrMeasurePostProcessor.STAFF);
        // A dense run of high violin notes used to yield more than 32 fake boundaries. Because
        // their heads were outside the one-gap attachment search, the whole staff was discarded.
        for (int x = 90; x <= 810; x += 20) {
            for (int y = 90; y <= top + 42; y++)
                set(labels, width, x, y, OmrMeasurePostProcessor.STEM_OR_REST);
            for (int y = 86; y <= 94; y++) for (int dx = -7; dx <= 1; dx++)
                set(labels, width, x + dx, y, OmrMeasurePostProcessor.NOTEHEAD);
        }

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(labels, width, height);

        assertEquals(1, measures.size());
    }

    @Test public void shortenedBarlineNearButNotAttachedToANoteIsRetained() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, false);
        for (int x : new int[]{60, 180, 420, 540})
            for (int y = 97; y <= 135; y++) set(page, 600, x, y, OmrMeasurePostProcessor.STEM_OR_REST);
        for (int y = 98; y <= 113; y++) set(page, 600, 300, y, OmrMeasurePostProcessor.STEM_OR_REST);
        for (int y = 114; y <= 120; y++) for (int x = 308; x <= 320; x++)
            set(page, 600, x, y, OmrMeasurePostProcessor.NOTEHEAD);

        assertEquals(4, OmrMeasurePostProcessor.process(page, 600, 500).size());
    }

    @Test public void firstMeasureBeginsAfterRecognizedClefAndKey() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, true);
        for (int y = 88; y <= 143; y++) for (int x = 85; x <= 120; x++)
            set(page, 600, x, y, OmrMeasurePostProcessor.CLEF_OR_KEY);
        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 500);
        assertEquals(4, measures.size());
        assertTrue(measures.get(0).left() > 120f / 600f);
        assertTrue(measures.get(0).right() < 180f / 600f);
    }

    @Test public void distantClefClassificationCannotChopTheFirstMeasure() {
        byte[] page = page(600, 500);
        staff(page, 600, 100, true);
        for (int y = 88; y <= 143; y++) for (int x = 85; x <= 120; x++)
            set(page, 600, x, y, OmrMeasurePostProcessor.CLEF_OR_KEY);
        for (int y = 106; y <= 126; y++) for (int x = 156; x <= 164; x++)
            set(page, 600, x, y, OmrMeasurePostProcessor.CLEF_OR_KEY);

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(page, 600, 500);

        assertEquals(4, measures.size());
        assertTrue(measures.get(0).left() < 150f / 600f);
    }

    @Test public void followsSlightlyRotatedStaffGeometry() {
        int width = 600, height = 500, top = 100;
        byte[] page = page(width, height);
        for (int x = 60; x <= 540; x++) {
            int shift = Math.round((x - width / 2f) * 0.03f);
            for (int line = 0; line < 5; line++)
                set(page, width, x, top + line * 8 + shift, OmrMeasurePostProcessor.STAFF);
        }
        for (int x : new int[]{60, 180, 300, 420, 540}) {
            int shift = Math.round((x - width / 2f) * 0.03f);
            for (int y = top - 3 + shift; y <= top + 35 + shift; y++)
                set(page, width, x, y, OmrMeasurePostProcessor.STEM_OR_REST);
        }
        assertEquals(4, OmrMeasurePostProcessor.process(page, width, height).size());
    }

    @Test public void rawFiveLineFallbackRecoversStaffOmittedBySemanticMask() {
        int width = 600, height = 500;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        rawStaff(gray, width, 100);
        rawStaff(gray, width, 260);
        staff(labels, width, 260, true);
        for (int x : new int[]{60, 180, 300, 420, 540}) {
            for (int y = 97; y <= 135; y++) {
                set(labels, width, x, y, OmrMeasurePostProcessor.STEM_OR_REST);
                gray[y * width + x] = 0;
            }
            for (int y = 257; y <= 295; y++) gray[y * width + x] = 0;
        }

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(labels, gray, width, height);

        assertEquals(8, measures.size());
        assertTrue(measures.get(3).top() < measures.get(4).top());
    }

    @Test public void rawBeamStaffBetweenConsecutiveSemanticSystemsIsRejected() {
        int width = 600, height = 620;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        staff(labels, width, 100, true);
        staff(labels, width, 260, true);
        staff(labels, width, 420, true);
        // Five long, regularly spaced beam bands can satisfy the raw staff detector, but this
        // candidate sits halfway between two already complete consecutive systems.
        rawStaff(gray, width, 180);

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(
                labels, gray, width, height);

        // Supplying grayscale requires raw confirmation for internal barlines, so each of the
        // three semantic systems remains one region. The false middle pseudo-staff adds none.
        assertEquals(3, measures.size());
        assertEquals(0, measures.stream().filter(item -> item.top() > .25f
                && item.top() < .38f).count());
    }

    @Test public void rawInkAloneDoesNotInventBarlines() {
        int width = 600, height = 500;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        staff(labels, width, 100, false);
        rawStaff(gray, width, 100);
        for (int x : new int[]{60, 180, 300, 420, 540})
            for (int y = 97; y <= 135; y++) gray[y * width + x] = 0;

        assertEquals(1, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void rawInkDoesNotResplitAnAlreadySegmentedSemanticStaff() {
        int width = 600, height = 500;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        staffWithBars(labels, width, 100, new int[]{60, 300, 540});
        rawStaff(gray, width, 100);
        for (int x : new int[]{60, 180, 300, 420, 540})
            for (int y = 97; y <= 135; y++) gray[y * width + x] = 0;

        assertEquals(2, OmrMeasurePostProcessor.process(labels, gray, width, height).size());
    }

    @Test public void rawInkLeavesFinalLongPageSystemForNumberReconciliation() {
        int width = 600, height = 700;
        byte[] labels = page(width, height);
        byte[] gray = page(width, height);
        java.util.Arrays.fill(gray, (byte) 0xff);
        for (int index = 0; index < 5; index++) {
            int top = 80 + index * 110;
            staff(labels, width, top, index < 4);
            rawStaff(gray, width, top);
            for (int x : new int[]{60, 180, 300, 420, 540})
                for (int y = top - 3; y <= top + 35; y++) gray[y * width + x] = 0;
        }

        List<MeasureRegion> measures = OmrMeasurePostProcessor.process(labels, gray, width, height);

        assertEquals(17, measures.size());
        assertEquals(1, measures.stream().filter(item -> item.top() > .70f).count());
    }

    private static byte[] page(int width, int height) { return new byte[width * height]; }

    private static void staff(byte[] page, int width, int top, boolean bars) {
        staffWithBars(page, width, top, bars ? new int[]{60, 180, 300, 420, 540} : new int[0]);
    }

    private static void staffWithBars(byte[] page, int width, int top, int[] bars) {
        for (int line = 0; line < 5; line++) {
            int y = top + line * 8;
            for (int x = 60; x <= 540; x++) set(page, width, x, y, OmrMeasurePostProcessor.STAFF);
        }
        for (int x : bars)
            for (int y = top - 3; y <= top + 35; y++)
                set(page, width, x, y, OmrMeasurePostProcessor.STEM_OR_REST);
    }

    private static void rawStaff(byte[] gray, int width, int top) {
        for (int line = 0; line < 5; line++)
            for (int x = 60; x <= 540; x++) gray[(top + line * 8) * width + x] = 0;
    }

    private static void rawStaffAndBars(byte[] gray, int width, int top, int[] bars) {
        rawStaff(gray, width, top);
        for (int x : bars) for (int y = top - 3; y <= top + 35; y++)
            gray[y * width + x] = 0;
    }

    private static void set(byte[] page, int width, int x, int y, byte value) {
        page[y * width + x] = value;
    }
}
