// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Random;

/** Original rendered direction words with scaled, shaded and threshold-noise controls. */
public class OctaveWordPackedParityTest {
    private static final String[] GLYPHS = {
        "A/wAAAAAA/wAAAAAB/4AAAAABw4AAAAADw8AAAAADgcAAAAADgcAAAAADgcAAAAAHgcAAAAADw4AAAABDw4cBgD7D4w+DwH/B+B+DwP/B/D+BwePB/B+Bw8PD/AeAw4PD/AeAh4PMPgeBBwPMHgOBDwP8HgPBDgO4DgPADgO4DwPAHgewDwPAHAcwDwHEHA8wDwHAHA8wDgHQPA84DgHgPB88HgHgHB+8PAHgHn///AHAHw+P8AGAD48P8AGAD48",
        "AAAAAAAAAAAAAAAAAQfAAAACAg/AAAAABhzgAAABBBhgAAABDBhgAAABGBhgAAABGBhgAAABOBxGDB+BOA5PDD+BMA8fDHODcA8PDGODcB8HBOODcCODCMODYGODCcMD4OGDAcMD4MHDAYcH4MHDAYcH4MHDg4cHwMGDw44GwOODg48OwGOBgZcOwH8BAecMQD4BAeYcQAAAAAAYQAAAAAAQQAAAAAAgAAAAAAAgAAAAAABAAAAAAAAAAAAAAACA",
        "AAAAAA/gAAAAAA/AAfgAAAfAA/wAAAPAB/wAAAOABw4AAAeADw8AAAeADgcAAAeADgcAAAeADgcAAAeADgcAAAcADw4IBAc8DwwcDw//B4x+Dw//B/D+Bw+PB/B+Bx+HB/AeAx8HD/AeAh4DMPAeBB4HMHgOBBwH8HgPBBwH4DwPADwH4DwPADwPwDwHEDgPwDwHADgPwDgHADgewDgHwHgc4HgHgHA88PAHgHB4//AHAH/wP+AGAD/gP8AGAD/A",
        "AAAAAHgAAAAAAHgAAQfAADgCAg/AADAABhzgADABBBhgADABDBhgAHABGBhgAHABGBhgAHABOBxGDG8BOA5PDH+BMA8fDPODcA8PDOGDcB8HBOGDcCODCOGDYGODCMGD4OGDAMGD4MHDAcOH4MHDAcOH4MHDgcMHwMGDwYcGwOODgYYOwGOBgY4OwH8BAfgMQD4BAPgcQAAAAAAYQAAAAAAQQAAAAAAgAAAAAAAgAAAAAABAAAAAAAAAAAAAAACA",
        "DwD4AAAADwD4AAAABwDwAAAAAwEAAAAAAwEAAAAABwEAAAAABwEAAAAABwEAAAAABgOAAAAABgPAQAABBgPhxjgfBgPh7jg/DgPh7zh/DgDw7zhnDgDw5zjnDgBw5xjDDABw5zjDDABw5znDDAAwxzmDDAAxxjmGHAAxxjGGHAAxzjGGHAAxjjOGGAAhjjMOGABhjjMOGABhjHMOGABBjHMOOBjBjHMeOBzDDHOffB+DHHPO/B8DHHHO/B8DHHHO",
        "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"
    };
    private static final float[] ASPECTS = {2.076923f, 2.222222f, 1.928571f, 2.222222f, 3.230769f, 3.055556f, 3.000000f, 3.055556f, 2.095238f, 2.370370f, 2.000000f, 2.285714f, 3.238095f, 3.296296f, 3.090909f, 3.178571f};

    static String decisions() throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        int positive = 0;
        for (int n = 0; n < 224; n++) {
            int height, width;
            byte[] pixels;
            if (n < 96) {
                int index = n % GLYPHS.length;
                height = 32 + (n / 16 % 3) * 9;
                width = Math.round(ASPECTS[index] * height);
                byte[] bits = Base64.getDecoder().decode(GLYPHS[index]);
                pixels = new byte[width * height];
                Arrays.fill(pixels, (byte) (n % 5 == 0 ? 185 : 255));
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int at = Math.min(31, y * 32 / height) * 48
                                + Math.min(47, x * 48 / width);
                        if ((bits[at / 8] & (1 << (7 - at % 8))) != 0)
                            pixels[y * width + x] = (byte) (n < 48 ? 0 : 145);
                    }
                }
            } else {
                height = 20 + n % 43;
                width = 31 + n % 91;
                pixels = new byte[width * height];
                Random random = new Random(907201L + n);
                int[] shades = {0, 164, 165, 180, 255};
                for (int i = 0; i < pixels.length; i++)
                    pixels[i] = (byte) shades[random.nextInt(shades.length)];
            }
            byte[] before = pixels.clone();
            int result = OctaveWordShapes.match(pixels, width, 0, 0, width - 1, height - 1);
            hash.update((byte) result);
            if (result != 0) positive++;
            org.junit.Assert.assertArrayEquals(before, pixels);
        }
        return positive + " " + HexFormat.of().formatHex(hash.digest());
    }

    @org.junit.Test
    public void wordDirectionAndRejectionDecisionsRemainExact() throws Exception {
        org.junit.Assert.assertEquals("96 1e8dc389716b6fdab9edb70a074a28eebcef73b8a11de42d222417e54b2038b8", decisions());
    }

    public static void main(String[] args) throws Exception {
        System.out.println(decisions());
    }
}
