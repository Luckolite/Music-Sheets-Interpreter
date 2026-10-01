// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original Bravura (OFL) numeral and Cambria Italic graphical renders at a held-out size.
 * Numeral size 44, suffix size 18, two-pixel separation, upper suffix offset 8 percent;
 * lower suffixes align to the numeral bottom. No font files or score pixels are included. */
public class MusicNumeralOctaveWordTest {
    private record Glyph(int width, int height, String encoded) {
        byte[] pixels() throws IOException {
            try (var in =
                    new GZIPInputStream(
                            new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
                return in.readAllBytes();
            }
        }
    }

    private static final Glyph[] GLYPHS = {
        new Glyph(
                35,
                21,
                "H4sIAAAAAAAC/3XSPUibURQG4DdG1BitWkgCVid/0IpE6BDxb0iXLnWolsaiIoI0IEEHJ1sVKUIlEDVb1ExFhyAUbIYWxYKIg7VLNxWhthDEIEkGY2ixr+feZPxyhnvPd87zHS6XS6pIB1+0PbC5ho6ZL1YdKBkP+50wefOITaD4h+yZFmCNtCLDuB0R/p2rqegP+aRzVgK81nYR6CT9uCAD1gQ9tp0bH0LSmAcwo0kIqCQ/4Yh8NckIvlKWn9IYEPJMkwmggTzGZ149vGTnEykFyu9kHRaCsCQpux53hXVOvGfcpEY/f6r+XVCk4AP/vQQeJ6VgWThtuZVhH8mY5Z0isVJl4GoHuv6oQoPP/Y08xBbpNUf1EaaRi8Cd/nZXjct6XTyWehvEd1XZb8sJ00H2nkYab9S2UtUU3cCjBJN9gMNTqE19xuhm0x3S+cV9uzZ+I7Ik8w9l/92sSJ8RcQI9OonXCnEaESswm82WhXQbkTpgMZutC5kyIh5gIJuNAhXnRuSkHGZ1XO6aUbRn/J6i1Sh6sxYaLEDrl3yvMh3obS2ztQ9u/88D7gG11Xcl3wIAAA=="),
        new Glyph(
                35,
                21,
                "H4sIAAAAAAAC/3XR3SuDURwH8O+GbWxes5HyUl7CrLkjbxeuKAlL8564QNIUV4qSlLWaNVfebl0spXDhQpQkxB/AprxEGoWEIX7OOdvK9PhdnPN9zvk853n6HSJer86GwhhtUfsR/VdzSVD1L9qMkPX+I5YA5TGbfXpgXlK4VUCLSFagVJKMAxgVaRaIkyTNjFSJZAGyJUkHI1hk4UkXPO5vTXIin6LPRiD/UZLcRHGDomKg7Or3Rj1yg3EEgbJ/hb6c3BcIO4UBIdsNFR8Kl5gfTUBSU7gwWb4QciDzitspYTvntKMTxsaX1PDRnQ4uchR4TZrq52l2/h5bv8zjxMSJDRdEdvUDmVvN7k04jUCFOPUulREjTys4JDIPEqVpr4k03WpgzP9lByPlPBxhjbwJt3QNB3uK6MoErH6ywMgwD14skGWCaFl+T+SBtQlo9pMuIPZMpMjJU/0b0VABy06Z+yQaYfx3aTMMii0/zh6o3GZTSQ3RWxa74PUUKHrmZ9vkMGwE+lEZ38/Gd2X6zVVdood3xl5r0GiL21a/gy3rzHlh4379TEZC4yXRD5Xnlv3fAgAA"),
        new Glyph(
                53,
                21,
                "H4sIAAAAAAAC/42TXUiTYRTHj9qaziRHa9lmNrRSREEJu7AiJPDKr0JDsd3IgkAHNYJaWRFaUZGjLgK76MssisCIldSobkIc7qIP+1BGs4uyWBlO1kTmTv/zvhp689q5eM7/fPye9zzP+77MqgUO9D06+A4iesYTmIGfHelpZG0LrCDYIFQnfIrJZtIRWbSZz2uEsSQgC2jeSsaYO5KKJpozyibe7zA0oThzMntlfbdTYX7mK12tkG/mEcOpP1KqaGkY7iN3w/gJGmVuXO2LOqlbCrFyte8FtJusGzL1uZVXwuoM+VkfOEjrInyXQnyfnjKWt3LkXWQWxhRHkEfBhXNPJruYn9Et5rOGOG/djFRXxizWNioOpwNyQA/RlkWH9ZGf+bQ+wlxXweGk40hV78Rym3K+Ppcn9SM4ROcXQZ0GbFtTDmVu5wD1MH9La0d01PaJW8Fk4u0kcmh7pVlnLnWFVKha+rNwXaP0hAfoAfP+FK9aSlgB2SEG4I0l8s50L5WKuY35C91gvk6v+Zd+3+SxyzSkQn6Z7qEyXerVOEerEK6algpdm7suB62N8CVjgbeXrL8V6DCa0mMQFzb6JR6WTQJLfESb0LNnQRwTqFebUTa+BzHlG1cSQUk81oY60JI6xRxaT8ldkvAKNKINlaKlBt4OXyaJvRC52syY7Htz7mi74b8vh/BoQx60LJNr3EZk+8gcr0WiaFqDuONqsghkP/KKL5IFJ4u3IM4OaT2m8N8/5+BEPdWdc8uQVT/4/62/tjDNWOwcXKLtL/QqIrJZBAAA"),
        new Glyph(
                53,
                21,
                "H4sIAAAAAAAC/42TX0hTcRTHz1ZzNhdorbVmzWGWIhP0wR5GEb5IwdKtWia2l7WXUKFWUIsIIissamLQQw/9/0MSGFEJ2Z+XiI320B/74x/SimWpGE3Mkm2n7+9uXiLGZefhnu8593zOPff87mVOWWR39909byCmjwcjs/CJ/qvbWdkieoKFoNrg5xmsBg2RWZn5uFQw5iRkGc1Z5YgiM1EqVTVDvppDdEd+KTIz9lTdE+gAFZbka4trz40rz5ZwkVEwhjiClTTE2VgLVYznAfJBv6A1WTHXyBJ9LJ7Ug2AfncwKOmj9wM1g8nE6SQutqzVqjFX+4Yy1LiqTdbIQkAfiOXxBpTgzzdOMlGmXLMNiujvSdLnn4zztQLj4dwZmNqdL1vtRlDcDcWpVWMR9okkkAxRWjcl6NWq2/XtwArou1FGVbbJpYfXk2/W6RoQdtrEt+o1TcuNbEFO9oxI0JBL3JVnjdfd1U8A9epgGmBuaGgYfUafUDiW5wIeLSH1GJO4JqF+CSk3v0GNFjG8SFmpZEmXW7xQ3qlBSB++BrxaJHRDFEvNT7Wd+SFeYT+jiHKUO5DReXEZE38vpV9sM/y0HIihBvYTNHNPGmJ01zLfVE2L2dtwIomT+D4i1RNb3zPF6JGypjbfpEsx1dijjIea9NohO1eANf6NZQJ4Dz/g0mfFmcS/i5ekvYpOoN7UyD9ADZrsDmy3ZwOXyP+fj5FZytgfEkI7v6eUbW5g/0SXmi/SS/2iLvn5xGv7/DXrqyxcUVLSG5ARdYO6i18w+WhYLuc5aF7k/M/8FJsQBNlkEAAA="),
        new Glyph(
                17,
                21,
                "H4sIAAAAAAAC/y2PPUvCURTGf2qY9L6oEDRVQ4X83ZTe5qaGJNCwhpaChpamoKAhSASTNjPXPkA0NESDRFN9gIggClpabBEh6nTuuT7Lc57ffe6BI+LUPltJD8Uz64/idZ4kttMoB4S2LV9C75N6Zwbq6i8xWLOXEsypHQEHBmowolZQsGRgFybVNhTQ0OE74avHDoRP5GcVplsKPvscIZOF+Q/7uk9XlV/LzXQ3h+5dbOUgme8xMtHRK2bV36SZMFIWOdXmgxbfpxzIiQSwaJu+xhQEIv1w6E+uKlgQGYeSBxcK9kTyUPBgE4ZfRZ4HibilchsheueG61GiW/VaMUzqxlfbleXUQDxbvPqz+A88mI/+ZQEAAA=="),
        new Glyph(
                29,
                21,
                "H4sIAAAAAAAC//v/HwLOFKzfVHgZyPja1n/mF5D+e3NxBEyOhwEITgBZLUCaWURBhJWBQQoid08cJCf1D8jUYIABgwdguTfqYF42kHkRJsXV+A0s990Kwt8HZFcySKsIsCu5TXsNMfNvIIMYSE7kD5CjzHDnPzLIYdB9zQ2UTAGyTzOYocgtYZB7uhekczuQU8LQhSJZpXDjfzZQTgDou39yDLZuYqxihkX34fL/pIGSsUDGMSAtaADyM+t+mORJkKkbwKZyzPrz/6sPkCv8AypZDuRwfwcyulVPgvhXQIrPQCXVgOwwJGd8B0kuhbDBClcCGZ93PwcL3AEJbIVINgOZHJ///78vz8DUBxLYApK8CZE0BDL9gHQskDYFCcQAGUoQuQcgdQuhVgcB6RdsQEY/RLIfyGR5D2TYMDAoXP///48/UEAH5JNlRZFSIMnYiiP/exmkgDb/SQLyZcAhpAWP25T//0IYAjorQYb7vPyPCbb7a3EK6uaegHIBQBpN4WECAAA=")
    };

    private int match(int index) throws IOException {
        var g = GLYPHS[index];
        return OctaveWordShapes.match(g.pixels(), g.width, 0, 0, g.width - 1, g.height - 1);
    }

    @Test
    public void raisedSuffixRecognizesUpperOctave() throws Exception {
        assertEquals(1, match(0));
    }

    @Test
    public void baselineSuffixRecognizesLowerOctave() throws Exception {
        assertEquals(-1, match(1));
    }

    @Test
    public void raisedSuffixRecognizesTwoUpperOctaves() throws Exception {
        assertEquals(2, match(2));
    }

    @Test
    public void baselineSuffixRecognizesTwoLowerOctaves() throws Exception {
        assertEquals(-2, match(3));
    }

    @Test
    public void bareMusicNumeralsNeedSpanEvidence() throws Exception {
        assertEquals(0, match(4));
        assertEquals(0, match(5));
    }

    @Test
    public void explicitUpperSuffixOwnsFollowingStaffBetweenRows() throws Exception {
        int w = 500, h = 300;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        var g = GLYPHS[0];
        var ink = g.pixels();
        for (int y = 0; y < g.height; y++)
            System.arraycopy(ink, y * g.width, gray, (110 + y) * w + 70, g.width);
        for (int x = 70 + g.width + 6; x < 300; x++)
            if ((x - 70 - g.width - 6) % 14 < 7) gray[115 * w + x] = gray[116 * w + x] = 0;
        var upper = new PlayingTechniqueDetector.Staff(20, 84, 16, 0, 2);
        var lower = new PlayingTechniqueDetector.Staff(180, 244, 16, 1, 2);
        var a = new ScoreNoteEvent(0, .4f, 2, 0, 2, 60f / h, false, 0, 0, 2, 1);
        var b = new ScoreNoteEvent(0, .4f, 2, 1, 2, 220f / h, false, 0, 0, 2, 1);
        var c = new ScoreNoteEvent(0, .8f, 2, 1, 2, 220f / h, false, 0, 0, 2, 1);
        var result =
                OctaveMarkDetector.apply(
                        List.of(),
                        List.of(upper, lower),
                        List.of(new MeasureRegion(0, 1, 0, 1)),
                        List.of(a, b, c),
                        gray,
                        w,
                        h);
        assertEquals(0, result.get(0).octaveShift());
        assertEquals(1, result.get(1).octaveShift());
        assertEquals(0, result.get(2).octaveShift());
    }
}
