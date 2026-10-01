// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import static org.junit.Assert.*;

/** Original procedural MusicXML timing examples, without score assets. */
public class MusicXmlWrittenDurationTest {
    private static Element element(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getDocumentElement();
    }

    private static String note(String type, int units, int actual, int normal, String extra) {
        return "<note><duration>"
                + units
                + "</duration><type>"
                + type
                + "</type>"
                + extra
                + "<time-modification><actual-notes>"
                + actual
                + "</actual-notes><normal-notes>"
                + normal
                + "</normal-notes></time-modification></note>";
    }

    private static long ticks(String xml, long divisions, long resolution) throws Exception {
        return MusicXmlWrittenDuration.durationTicks(element(xml), divisions, resolution);
    }

    private static void rejects(String xml, long divisions, long resolution) throws Exception {
        try {
            ticks(xml, divisions, resolution);
            fail("Invalid or contradictory timing accepted");
        } catch (IOException expected) {
        }
    }

    @Test
    public void sevenRoundedSixteenthsFillExactlyOneQuarter() throws Exception {
        long sum = 0;
        for (int i = 0; i < 7; i++) sum += ticks(note("16th", 23, 7, 4, ""), 160, 10080);
        assertEquals(10080, sum);
    }

    @Test
    public void nearestRoundingCanGoDown() throws Exception {
        assertEquals(720, ticks(note("16th", 11, 7, 2, ""), 160, 10080));
    }

    @Test
    public void dottedWrittenUnitIsIncluded() throws Exception {
        assertEquals(2160, ticks(note("16th", 34, 7, 4, "<dot/>"), 160, 10080));
    }

    @Test
    public void twoWrittenDotsAreIncluded() throws Exception {
        assertEquals(2520, ticks(note("16th", 40, 7, 4, "<dot/><dot/>"), 160, 10080));
    }

    @Test
    public void differingNormalTypeDoesNotChangeCumulativeRatio() throws Exception {
        String xml =
                note("quarter", 107, 3, 2, "")
                        .replace(
                                "</time-modification>",
                                "<normal-type>eighth</normal-type></time-modification>");
        assertEquals(6720, ticks(xml, 160, 10080));
    }

    @Test
    public void exactExportGridKeepsExplicitIntendedDuration() throws Exception {
        assertEquals(2880, ticks(note("16th", 2, 7, 4, ""), 7, 10080));
    }

    @Test
    public void ordinaryIntendedDurationIsPreserved() throws Exception {
        assertEquals(
                1449, ticks("<note><duration>23</duration><type>16th</type></note>", 160, 10080));
    }

    @Test
    public void independentVoiceBackupKeepsItsClock() throws Exception {
        assertEquals(30240, ticks("<backup><duration>480</duration></backup>", 160, 10080));
    }

    @Test
    public void forwardKeepsItsClock() throws Exception {
        assertEquals(15120, ticks("<forward><duration>240</duration></forward>", 160, 10080));
    }

    @Test
    public void nestedRatioIsNotTimingEvidence() throws Exception {
        assertEquals(
                1449,
                ticks(
                        "<note><duration>23</duration><type>16th</type><notations><time-modification><actual-notes>7</actual-notes><normal-notes>4</normal-notes></time-modification></notations></note>",
                        160,
                        10080));
    }

    @Test
    public void unrelatedDotsAreNotAugmentation() throws Exception {
        assertEquals(
                1440, ticks(note("16th", 23, 7, 4, "<notations><dot/></notations>"), 160, 10080));
    }

    @Test
    public void changedDurationIsRejected() throws Exception {
        rejects(note("16th", 24, 7, 4, ""), 160, 10080);
    }

    @Test
    public void changedRatioIsRejected() throws Exception {
        rejects(note("16th", 23, 7, 5, ""), 160, 10080);
    }

    @Test
    public void changedWrittenTypeIsRejected() throws Exception {
        rejects(note("eighth", 23, 7, 4, ""), 160, 10080);
    }

    @Test
    public void changedDotCountIsRejected() throws Exception {
        rejects(note("16th", 23, 7, 4, "<dot/>"), 160, 10080);
    }

    @Test
    public void missingTypeIsRejected() throws Exception {
        rejects(note("16th", 23, 7, 4, "").replace("<type>16th</type>", ""), 160, 10080);
    }

    @Test
    public void missingRatioFieldIsRejected() throws Exception {
        rejects(
                note("16th", 23, 7, 4, "").replace("<normal-notes>4</normal-notes>", ""),
                160,
                10080);
    }

    @Test
    public void zeroRatioIsRejected() throws Exception {
        rejects(note("16th", 23, 0, 4, ""), 160, 10080);
    }

    @Test
    public void negativeRatioIsRejected() throws Exception {
        rejects(note("16th", 23, 7, -4, ""), 160, 10080);
    }

    @Test
    public void zeroDurationIsRejected() throws Exception {
        rejects(note("16th", 0, 7, 4, ""), 160, 10080);
    }

    @Test
    public void negativeDurationIsRejected() throws Exception {
        rejects(note("16th", -23, 7, 4, ""), 160, 10080);
    }

    @Test
    public void invalidDivisionsAreRejected() throws Exception {
        rejects(note("16th", 23, 7, 4, ""), 0, 10080);
    }

    @Test
    public void invalidResolutionIsRejected() throws Exception {
        rejects(note("16th", 23, 7, 4, ""), 160, 0);
    }

    @Test
    public void insufficientTargetPrecisionIsRejected() throws Exception {
        rejects(note("16th", 23, 7, 4, ""), 160, 100);
    }

    @Test
    public void rawPrecisionLossIsRejected() throws Exception {
        rejects("<note><duration>1</duration></note>", 3, 100);
    }

    @Test
    public void overflowingRawDurationIsRejected() throws Exception {
        rejects("<note><duration>9223372036854775807</duration></note>", 1, 10080);
    }

    @Test
    public void overflowingWrittenDurationIsRejected() throws Exception {
        rejects(note("maxima", 2926, 7, 4, ""), 160, Long.MAX_VALUE - Long.MAX_VALUE % 7);
    }

    @Test
    public void largePositiveWrittenDurationRetainsEveryBit() throws Exception {
        assertEquals(
                6148914691236517204L,
                ticks(note("quarter", 107, 3, 2, ""), 160, Long.MAX_VALUE - 1));
    }

    @Test
    public void unknownTypeIsRejected() throws Exception {
        rejects(note("invalid", 23, 7, 4, ""), 160, 10080);
    }
}
