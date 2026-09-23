// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import static org.junit.Assert.*;

/** Original synthetic staff signatures; no commercial score image is used. */
public class MeterFontMatcherTest {
    private static final File FONT = new File("java/assets/Bravura.otf");

    @Test public void readsPrintedStackedDigitsAfterStaffRemoval() throws Exception {
        var matcher = new MeterFontMatcher(FONT);
        assertEquals("2/4", matcher.read(signature("2", "4", 32), 32, 73, 36, 3, 16.5f).text());
        assertEquals("12/8", matcher.read(signature("12", "8", 48), 48, 73, 36, 3, 16.5f).text());
    }

    @Test public void narrowBarlineCropIsNotAOneOneMeter() throws Exception {
        var matcher = new MeterFontMatcher(FONT);
        int[] pixels = new int[16*73];
        java.util.Arrays.fill(pixels, 0xffffffff);
        for (int y=0;y<73;y++) pixels[y*16+7]=0xff000000;
        assertEquals("", matcher.read(pixels, 16, 73, 36, 3, 16.5f).text());
    }

    private static int[] signature(String numerator, String denominator, int width) throws Exception {
        BufferedImage image = new BufferedImage(width,73,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0,0,width,73);
        Font font=Font.createFont(Font.TRUETYPE_FONT,FONT).deriveFont(62f);
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        print(graphics, glyphs(numerator), width, 3, 36);
        print(graphics, glyphs(denominator), width, 36, 73);
        graphics.dispose();
        return image.getRGB(0,0,width,73,null,0,width);
    }

    private static String glyphs(String digits) {
        return digits.codePoints().map(ch -> 0xe080 + ch - '0')
                .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
    }

    private static void print(Graphics2D graphics, String digits, int width, int top, int bottom) {
        var bounds=graphics.getFont().createGlyphVector(graphics.getFontRenderContext(),digits)
                .getPixelBounds(null,0,0);
        int left=(width-bounds.width)/2;
        int vertical=top+(bottom-top-bounds.height)/2;
        graphics.drawString(digits,left-bounds.x,vertical-bounds.y);
    }
}
