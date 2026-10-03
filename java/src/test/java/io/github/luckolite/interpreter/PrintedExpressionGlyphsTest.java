// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

/** Original synthetic pages; glyph sprites rendered from SIL-OFL Bravura, no score content. */
public final class PrintedExpressionGlyphsTest {
    private static final int W = 600, H = 380;

    private static BufferedImage page() {
        var image = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        var g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.BLACK);
        for (int y = 180; y <= 244; y += 16) g.drawLine(20, y, 580, y);
        g.dispose();
        return image;
    }

    private static BufferedImage glyph(String name) throws Exception {
        return ImageIO.read(
                Path.of("java/src/test/resources/expression-glyphs", name + ".png").toFile());
    }

    private static int draw(BufferedImage page, String name, int x, int y, int h) throws Exception {
        var image = glyph(name);
        int w = Math.round(image.getWidth() * (float) h / image.getHeight());
        var g = page.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, x, y, w, h, null);
        g.dispose();
        return x + w;
    }

    private static void equals(BufferedImage page, int x, int y) {
        var g = page.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(x, y, 12, 2);
        g.fillRect(x, y + 7, 12, 2);
        g.dispose();
    }

    private static ScorePageInterpretation score() {
        var notes = new ArrayList<ScoreNoteEvent>();
        for (float position : new float[] {.2f, .43f, .67f, .85f})
            notes.add(new ScoreNoteEvent(0, position, 0, 0, 1, 244f / H, false, 0, 0, 0, 1, 1, 0));
        return new ScorePageInterpretation(
                List.of(new MeasureRegion(20f / W, 580f / W, 150f / H, 280f / H)),
                notes,
                1,
                List.of(),
                List.of(),
                List.of(new ScoreMeterChange(0, 4, 4)),
                List.of(),
                List.of(),
                List.of());
    }

    private static ScorePageInterpretation detect(BufferedImage image, boolean rails) {
        if (!rails) {
            var g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 180, W, 80);
            g.dispose();
        }
        byte[] gray = new byte[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) gray[y * W + x] = (byte) (image.getRGB(x, y) & 255);
        return ScoreExpressionDetector.apply(
                score(),
                List.of(),
                List.of(new PlayingTechniqueDetector.Staff(180, 244, 16, 0, 1)),
                gray,
                W,
                H,
                GlyphResources.expressions());
    }

    private static BufferedImage equation(String left, String right, int h, boolean dot)
            throws Exception {
        var image = page();
        int x = 132, y = 110;
        int edge =
                draw(
                        image,
                        left,
                        x,
                        left.equals("whole") ? y + h - 12 : y,
                        left.equals("whole") ? 12 : h);
        if (dot) {
            var g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.fillOval(edge + 4, y + h - 9, 4, 4);
            g.dispose();
            edge += 10;
        }
        equals(image, edge + 8, y + h - 16);
        draw(image, right, edge + 30, y, h);
        return image;
    }

    @Test
    public void bothPrintedNoteShapesDetermineRelativePulse() throws Exception {
        for (int height : new int[] {28, 36, 44}) {
            var result = detect(equation("quarter", "half", height, false), true);
            assertEquals("height=" + height, 1, result.expressiveEvents().size());
            var event = result.expressiveEvents().get(0);
            assertEquals(Kind.METRIC_MODULATION, event.kind());
            assertEquals(
                    2, MetricModulationText.decode(event.qualifierText()).orElseThrow().ratio(), 0);
            assertEquals(new ScoreAnchor(0, 0), event.start().orElseThrow());
            assertEquals(score().notes(), result.notes());
        }
    }

    @Test
    public void flagsAndDotsArePrintedEvidence() throws Exception {
        var event = detect(equation("eighth", "quarter", 36, true), true).expressiveEvents().get(0);
        var pulses = MetricModulationText.decode(event.qualifierText()).orElseThrow();
        assertEquals(.75, pulses.leftQuarterBeats(), 0);
        assertEquals(1, pulses.rightQuarterBeats(), 0);
        var sixteenth =
                detect(equation("sixteenth", "eighth", 36, false), true).expressiveEvents().get(0);
        assertEquals(
                2, MetricModulationText.decode(sixteenth.qualifierText()).orElseThrow().ratio(), 0);
    }

    @Test
    public void stemDownAndWholeHaveActualPulseValues() throws Exception {
        var event =
                detect(equation("half-down", "quarter-down", 36, false), true)
                        .expressiveEvents()
                        .get(0);
        assertEquals(
                .5, MetricModulationText.decode(event.qualifierText()).orElseThrow().ratio(), 0);
        var whole = detect(equation("whole", "quarter", 36, false), true).expressiveEvents().get(0);
        assertEquals(
                .25, MetricModulationText.decode(whole.qualifierText()).orElseThrow().ratio(), 0);
    }

    @Test
    public void commaAndTickOwnPreviousWrittenRelease() throws Exception {
        for (String name : List.of("breath-comma", "breath-tick")) {
            var image = page();
            draw(image, name, 158, 145, 22);
            var result = detect(image, true);
            assertEquals(name, 1, result.expressiveEvents().size());
            var event = result.expressiveEvents().get(0);
            assertEquals(Kind.BREATH, event.kind());
            assertEquals(new ScoreAnchor(0, 1), event.start().orElseThrow());
        }
    }

    @Test
    public void punctuationBowMarkAndNumericTempoDoNotBecomePauseOrModulation() throws Exception {
        var punctuation = page();
        int edge = draw(punctuation, "breath-comma", 158, 145, 22);
        var g = punctuation.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SERIF, Font.PLAIN, 22));
        g.drawString("word", edge + 3, 163);
        g.dispose();
        assertTrue(detect(punctuation, true).expressiveEvents().isEmpty());
        var bow = page();
        draw(bow, "upbow", 158, 145, 22);
        assertTrue(detect(bow, true).expressiveEvents().isEmpty());
        var numeric = page();
        edge = draw(numeric, "quarter", 132, 110, 36);
        equals(numeric, edge + 8, 130);
        g = numeric.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SERIF, Font.PLAIN, 24));
        g.drawString("120", edge + 30, 145);
        g.dispose();
        assertTrue(detect(numeric, true).expressiveEvents().isEmpty());
        assertTrue(
                detect(equation("quarter", "half", 36, false), false).expressiveEvents().isEmpty());
    }

    @Test
    public void isolatedDigitsLettersAndSlashAreNotBreathSigns() {
        for (String text : List.of("1", "7", "I", "l", "/", ";", "'")) {
            var image = page();
            var g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SERIF, Font.PLAIN, 28));
            g.drawString(text, 158, 168);
            g.dispose();
            assertTrue(text, detect(image, true).expressiveEvents().isEmpty());
        }
    }
}
