// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Uses the existing licensed public glyph template, with original OCR semantics. */
public class NativeSuddenLevelGlyphTest {
    @Test
    public void nativeGlyphReplacementRetainsSuddenQualification() throws Exception {
        var assets =
                new File(
                        "java/src/main/resources/io/github/luckolite/interpreter/glyphs/dynamic_templates");
        var template = ImageIO.read(new File(assets, "p.png"));
        int w = Math.round(24f * template.getWidth() / template.getHeight());
        var page = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        var g = page.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 320, 240);
        g.drawImage(template, 100, 120, w, 24, null);
        g.dispose();
        byte[] gray = new byte[320 * 240];
        for (int y = 0; y < 240; y++)
            for (int x = 0; x < 320; x++)
                gray[y * 320 + x] = (byte) ((page.getRGB(x, y) >>> 16) & 255);
        var literal =
                new PlayingTechniqueDetector.Word(
                        "subp", 100f / 320, 120f / 240, (100f + w) / 320, 144f / 240);
        var words =
                new NativeDynamicGlyphs()
                        .recognize(
                                gray,
                                320,
                                240,
                                List.of(new PlayingTechniqueDetector.Staff(50, 90, 10, 0, 1)),
                                List.of(literal));
        assertEquals(1, words.size());
        assertEquals("subp", words.get(0).text());
    }
}
