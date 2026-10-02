// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original licensed/rendered control words surrounded by procedural curved brackets. */
public class ParenthesizedOctaveContinuationTest {
    private record Glyph(int width, int height, String encoded) {
        byte[] pixels() throws Exception {
            try (var in =
                    new java.util.zip.GZIPInputStream(
                            new java.io.ByteArrayInputStream(
                                    Base64.getDecoder().decode(encoded)))) {
                return in.readAllBytes();
            }
        }
    }

    private static final Glyph G0 =
            new Glyph(
                    37,
                    19,
                    "H4sIAAAAAAAC/32STUiUURSGnxlnvhmEkVDLcBYV5Q/EVFQGbiSslS3KMiiEICqJMBICV0KMMG4UISFRcBGBQhLRRqhoEUITDUFSFkmtBn+YTVEUiM58b+fOtJ25m/eecx4O7z33SHYWz+4KNVxcUqUzHqDvVwrvuQrvR7uafGkilnSF3LUdTZ+qOGjXtwG8H9oK0ZhfCAIfpDiRbel7PDifPg4DBl2BAya1sKRcI0xLLQS3tbqHW1IUFqxqbLNJCL5IA9AvzXNMOoe3rkIV3h+rdkBwQ1nYX5BmoMty1TN6BZelz3DS2RsyH9c1TeS1BS9xTZaP5NUJT6VRSDkoGzFqaGci7YKPsFf+ha9aCxD9K7VCpjiClEEMl8axDrs1+FiahYT0BGoLLp/psYdRv1qENqHu3qRdhp25323QY8HWzUD3z6NGdZZahQk9cmpmErnzN2AiM+t3E15TtsaoF0WouqakaedhqhdOXPUfwhlLJS0zWCw2L///rDuxljklo/tG8m5IY5Z5Z9Dtsp8bL019xaD7ZaFDsGjyDLxsWWgE5kxOQbL8Lm2e5vC3jT6461fYuPyD9lg4fulNpa38B53Toji/AgAA");
    private static final Glyph G5 =
            new Glyph(
                    29,
                    19,
                    "H4sIAAAAAAAC/02RP0hCURTGvyf6jP4NWglJNBgtYUFIFEFD2VQUQkE41BJSkBAVTS4GL5cIGoraGiKKICIKipYSTGxqCCHaHoqbIYSEvDyd98/nGe73HX6Xe+45h4gjOdth98y/U31UJnGs6r6ASEmC+FDHqmEIeda0ALFIFTs6FQuuA0OqLgI9LC7AejgBQFJNAOhlsQNZk50yw4fqxgBbgWTA92ewO74In2Zj7JbpBM5ng6Ub4Qc2NC87mcba/a8Gy7oQ3QJe9ExSK+yY9XJdiFe74da//jbHH0VbTmfFPtsRpYAlbRIrQuh7kOm4xsqj4gXRGnCtTiIER57kVqaPnCozTSyKBw0/ekdTLHGG26yr7gyfT8C00eQeS4ZhlKjkQC2CRF7gnuEnZwdElxbDGVE/kGR4A4iy2cwtMKyZXeCcZQKI16YeBhKa+Q1i4KsQATarJis311agHI60OLwLKWuVV/oKOf4BLM4QdScCAAA=");

    private List<ScoreNoteEvent> run(
            String glyphName, boolean dash, boolean brackets, boolean stemEdges) throws Exception {
        int w = 700, h = 320;
        byte[] g = new byte[w * h];
        Arrays.fill(g, (byte) 255);
        var field = ParenthesizedOctaveContinuationTest.class.getDeclaredField(glyphName);
        field.setAccessible(true);
        Object glyph = field.get(null);
        Method width = glyph.getClass().getDeclaredMethod("width"),
                height = glyph.getClass().getDeclaredMethod("height"),
                pixels = glyph.getClass().getDeclaredMethod("pixels");
        width.setAccessible(true);
        height.setAccessible(true);
        pixels.setAccessible(true);
        int gw = (int) width.invoke(glyph), gh = (int) height.invoke(glyph);
        byte[] raster = (byte[]) pixels.invoke(glyph);
        for (int y = 0; y < gh; y++)
            for (int x = 0; x < gw; x++) g[(58 + y) * w + 100 + x] = raster[y * gw + x];
        if (brackets)
            for (int y = 0; y < 27; y++) {
                int offset = stemEdges ? 0 : Math.round(7 * (float) Math.pow((y - 13) / 13f, 2));
                for (int dx = 0; dx < 2; dx++) {
                    g[(54 + y) * w + 87 + offset + dx] = 0;
                    g[(54 + y) * w + gw + 111 - offset + dx] = 0;
                }
            }
        if (dash)
            for (int x = gw + 123; x <= 560; x++)
                if ((x - gw - 123) % 14 < 7) {
                    g[68 * w + x] = 0;
                    g[69 * w + x] = 0;
                }
        var measures = List.of(new MeasureRegion(0, 1, 0, 1));
        var staff = new PlayingTechniqueDetector.Staff(140, 204, 16, 0, 1);
        var n = new ScoreNoteEvent(0, 300f / w, 2, 0, 1, 180f / h, false, 0, 2, 2, 0);
        return OctaveMarkDetector.apply(List.of(), List.of(staff), measures, List.of(n), g, w, h);
    }

    @Test
    public void parenthesizedUpperDirectionKeepsItsCompleteDashSpan() throws Exception {
        assertEquals(1, run("G0", true, true, false).get(0).octaveShift());
    }

    @Test
    public void parenthesesWithoutADashDoNotTransposeDistantNotes() throws Exception {
        assertEquals(0, run("G0", false, true, false).get(0).octaveShift());
    }

    @Test
    public void parenthesizedOrdinaryNumeralDoesNotTranspose() throws Exception {
        assertEquals(0, run("G5", true, true, false).get(0).octaveShift());
    }

    @Test
    public void extraVerticalStrokesDoNotSuppressAProvedDirection() throws Exception {
        assertEquals(1, run("G0", true, true, true).get(0).octaveShift());
    }

    @Test
    public void originalUnparenthesizedDirectionsRemainRecognized() throws Exception {
        assertEquals(1, run("G0", true, false, false).get(0).octaveShift());
    }
}
