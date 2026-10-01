// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural pages using existing licensed Bravura templates; no score-derived input. */
public class FaintDynamicGlyphTemplateTest {
    private static final int W = 400, H = 280;
    private static final List<PlayingTechniqueDetector.Staff> STAFFS =
            List.of(new PlayingTechniqueDetector.Staff(80, 128, 12, 0, 1));

    private static byte[] blank(int paper) {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) paper);
        return p;
    }

    private static byte[] page(String mark, int paper, int ink, boolean darkHook) throws Exception {
        var original = GlyphResources.image("dynamic_templates/" + mark + ".png");
        int h = 28, w = Math.round(h * original.getWidth() / (float) original.getHeight());
        var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(original, 0, 0, w, h, null);
        graphics.dispose();
        byte[] p = blank(paper);
        int last = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int gray = (image.getRGB(x, y) >>> 16) & 255;
                int at = (180 + y) * W + 100 + x;
                p[at] = (byte) Math.round(ink + (paper - ink) * gray / 255f);
                if (gray < 50) last = at;
            }
        if (darkHook && last >= 0) {
            p[last] = 0;
            p[last - 1] = 0;
            p[last - 2] = 0;
        }
        return p;
    }

    private static List<PlayingTechniqueDetector.Word> words(byte[] p) throws Exception {
        return new NativeDynamicGlyphs().recognize(p, W, H, STAFFS, List.of());
    }

    private static void has(String mark, byte[] p) throws Exception {
        var found = words(p);
        assertEquals(1, found.size());
        assertEquals(mark, found.get(0).text());
    }

    @Test
    public void faintForteKeepsItsPrintedLevel() throws Exception {
        has("f", page("f", 255, 170, false));
    }

    @Test
    public void faintMezzoPianoKeepsBothLetters() throws Exception {
        has("mp", page("mp", 255, 170, false));
    }

    @Test
    public void faintMezzoForteKeepsBothLetters() throws Exception {
        has("mf", page("mf", 255, 170, false));
    }

    @Test
    public void tinyDarkHookDoesNotDiscardMostlyFadedForte() throws Exception {
        has("f", page("f", 255, 170, true));
    }

    @Test
    public void paleConnectedStrokeStillNeedsAndPassesFontProof() throws Exception {
        has("f", page("f", 255, 195, false));
    }

    @Test
    public void ordinaryDarkForteRemains() throws Exception {
        has("f", page("f", 255, 0, false));
    }

    @Test
    public void ordinaryDarkMezzoPianoRemains() throws Exception {
        has("mp", page("mp", 255, 0, false));
    }

    @Test
    public void uniformGrayRectangleNeverSuppliesAFont() throws Exception {
        byte[] p = blank(255);
        for (int y = 180; y < 208; y++) for (int x = 100; x < 120; x++) p[y * W + x] = (byte) 175;
        assertTrue(words(p).isEmpty());
    }

    @Test
    public void lowContrastPaperTextureCannotBecomeAFont() throws Exception {
        assertTrue(words(page("f", 175, 160, false)).isEmpty());
    }

    @Test
    public void fadedStaffRulesStayExcluded() throws Exception {
        byte[] p = blank(255);
        for (int y = 80; y <= 128; y += 12)
            for (int x = 70; x < 330; x++) p[y * W + x] = (byte) 175;
        assertTrue(words(p).isEmpty());
    }

    @Test
    public void fadedHairpinIsNotAStandaloneLevel() throws Exception {
        byte[] p = blank(255);
        for (int x = 70; x < 330; x++) {
            int rise = (x - 70) / 20;
            p[(170 + rise) * W + x] = (byte) 175;
            p[(196 - rise) * W + x] = (byte) 175;
        }
        assertTrue(words(p).isEmpty());
    }

    @Test
    public void grayPaperPatchDoesNotInventALevel() throws Exception {
        assertTrue(words(blank(180)).isEmpty());
    }

    @Test
    public void glyphProposalsLeaveCallerPixelsUntouched() throws Exception {
        byte[] p = page("f", 255, 170, true), original = p.clone();
        words(p);
        assertArrayEquals(original, p);
    }
}
