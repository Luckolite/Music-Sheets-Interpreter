// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.IOException;
import java.math.BigInteger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Exact written tuplet timing when an exporter rounds to a coarse division grid. */
public final class MusicXmlWrittenDuration {
    private MusicXmlWrittenDuration() {}

    public static long durationTicks(Element element, long divisions, long resolution)
            throws IOException {
        try {
            if (divisions <= 0 || resolution <= 0)
                throw new IOException("Invalid timing divisions");
            long units = Long.parseLong(required(element, "duration"));
            if (units <= 0) throw new IOException("Invalid metric duration");
            Element modification = direct(element, "time-modification");
            if (element.getTagName().equals("note") && modification != null) {
                long actual = Long.parseLong(required(modification, "actual-notes"));
                long normal = Long.parseLong(required(modification, "normal-notes"));
                if (actual <= 0 || normal <= 0) throw new IOException("Invalid tuplet ratio");
                long[] type =
                        switch (required(element, "type")) {
                            case "maxima" -> new long[] {32, 1};
                            case "long" -> new long[] {16, 1};
                            case "breve" -> new long[] {8, 1};
                            case "whole" -> new long[] {4, 1};
                            case "half" -> new long[] {2, 1};
                            case "quarter" -> new long[] {1, 1};
                            case "eighth" -> new long[] {1, 2};
                            case "16th" -> new long[] {1, 4};
                            case "32nd" -> new long[] {1, 8};
                            case "64th" -> new long[] {1, 16};
                            case "128th" -> new long[] {1, 32};
                            case "256th" -> new long[] {1, 64};
                            case "512th" -> new long[] {1, 128};
                            case "1024th" -> new long[] {1, 256};
                            default -> throw new IOException("Unknown written tuplet type");
                        };
                int dots = 0;
                for (Node node = element.getFirstChild();
                        node != null;
                        node = node.getNextSibling())
                    if (node instanceof Element child && child.getTagName().equals("dot")) dots++;
                if (dots > 8) throw new IOException("Too many augmentation dots");
                BigInteger numerator =
                        BigInteger.valueOf(type[0])
                                .multiply(BigInteger.valueOf(normal))
                                .multiply(BigInteger.valueOf((1L << (dots + 1)) - 1));
                BigInteger denominator =
                        BigInteger.valueOf(type[1])
                                .multiply(BigInteger.valueOf(actual))
                                .multiply(BigInteger.valueOf(1L << dots));
                BigInteger exportNumerator = numerator.multiply(BigInteger.valueOf(divisions));
                if (exportNumerator.remainder(denominator).signum() != 0) {
                    // Recovery requires the serialized value to be exactly the nearest
                    // exporter unit. Contradictory durations are never normalized.
                    BigInteger nearest =
                            exportNumerator
                                    .shiftLeft(1)
                                    .add(denominator)
                                    .divide(denominator.shiftLeft(1));
                    if (!nearest.equals(BigInteger.valueOf(units)))
                        throw new IOException("Tuplet duration contradicts its written ratio");
                    BigInteger[] ticks =
                            numerator
                                    .multiply(BigInteger.valueOf(resolution))
                                    .divideAndRemainder(denominator);
                    if (ticks[1].signum() != 0 || ticks[0].signum() <= 0)
                        throw new IOException(
                                "Written tuplet exceeds the requested timing precision");
                    // longValueExact is unavailable on older Android releases.
                    // The positive value fits a signed long exactly up to 63 bits.
                    if (ticks[0].bitLength() > 63)
                        throw new IOException("Written tuplet duration exceeds a signed long");
                    return ticks[0].longValue();
                }
            }
            // Explicit intended durations stay unchanged when no rounding is proved.
            long scaled = Math.multiplyExact(units, resolution);
            if (scaled % divisions != 0) throw new IOException("Rhythmic precision was changed");
            return scaled / divisions;
        } catch (IOException failure) {
            throw failure;
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IOException("Invalid metric duration", failure);
        }
    }

    private static String required(Element element, String name) throws IOException {
        Element child = direct(element, name);
        if (child == null) throw new IOException("Missing timing field " + name);
        return child.getTextContent();
    }

    private static Element direct(Element element, String name) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element child && child.getTagName().equals(name)) return child;
        return null;
    }
}
