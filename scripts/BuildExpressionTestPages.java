// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Reproduce Android-compatible raster fixtures from the original public JVM tests.
import io.github.luckolite.interpreter.PrintedExpressionGlyphsTest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.zip.GZIPOutputStream;

public final class BuildExpressionTestPages {
    private static java.lang.reflect.Method page, draw, equation, equals;

    private static BufferedImage page() throws Exception {
        return (BufferedImage) page.invoke(null);
    }

    private static int draw(BufferedImage image, String glyph, int x, int y, int h)
            throws Exception {
        return (int) draw.invoke(null, image, glyph, x, y, h);
    }

    private static void save(Path output, String name, BufferedImage image) throws Exception {
        try (var out =
                new GZIPOutputStream(Files.newOutputStream(output.resolve(name + ".gray.gz")))) {
            for (int y = 0; y < image.getHeight(); y++)
                for (int x = 0; x < image.getWidth(); x++) out.write(image.getRGB(x, y) & 255);
        }
    }

    public static void main(String[] args) throws Exception {
        page = PrintedExpressionGlyphsTest.class.getDeclaredMethod("page");
        page.setAccessible(true);
        draw =
                PrintedExpressionGlyphsTest.class.getDeclaredMethod(
                        "draw", BufferedImage.class, String.class, int.class, int.class, int.class);
        draw.setAccessible(true);
        equation =
                PrintedExpressionGlyphsTest.class.getDeclaredMethod(
                        "equation", String.class, String.class, int.class, boolean.class);
        equation.setAccessible(true);
        equals =
                PrintedExpressionGlyphsTest.class.getDeclaredMethod(
                        "equals", BufferedImage.class, int.class, int.class);
        equals.setAccessible(true);
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        for (int h : new int[] {28, 36, 44})
            save(
                    output,
                    "quarter-half-" + h,
                    (BufferedImage) equation.invoke(null, "quarter", "half", h, false));
        for (var pair :
                new String[][] {
                    {"eighth", "quarter", "dotted"},
                    {"sixteenth", "eighth", "plain"},
                    {"half-down", "quarter-down", "plain"},
                    {"whole", "quarter", "plain"}
                })
            save(
                    output,
                    pair[0] + "-" + pair[1],
                    (BufferedImage)
                            equation.invoke(null, pair[0], pair[1], 36, pair[2].equals("dotted")));
        for (String glyph : new String[] {"breath-comma", "breath-tick", "upbow"}) {
            var image = page();
            draw(image, glyph, 158, 145, 22);
            save(output, glyph, image);
        }
        var image = page();
        int edge = draw(image, "breath-comma", 158, 145, 22);
        var g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SERIF, Font.PLAIN, 22));
        g.drawString("word", edge + 3, 163);
        g.dispose();
        save(output, "punctuation", image);
        image = page();
        edge = draw(image, "quarter", 132, 110, 36);
        equals.invoke(null, image, edge + 8, 130);
        g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SERIF, Font.PLAIN, 24));
        g.drawString("120", edge + 30, 145);
        g.dispose();
        save(output, "numeric-tempo", image);
        image = (BufferedImage) equation.invoke(null, "quarter", "half", 36, false);
        g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 180, 600, 80);
        g.dispose();
        save(output, "no-rails", image);
        for (String text : new String[] {"1", "7", "I", "l", "/", ";", "'"}) {
            image = page();
            g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SERIF, Font.PLAIN, 28));
            g.drawString(text, 158, 168);
            g.dispose();
            save(output, "text-" + Integer.toHexString(text.charAt(0)), image);
        }
    }
}
