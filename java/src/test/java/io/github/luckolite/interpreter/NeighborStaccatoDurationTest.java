// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raster notation with a descending interval and a separate round mark. */
public final class NeighborStaccatoDurationTest {
    @Test
    public void staccatoAboveNextLowerHeadDoesNotLengthenPriorHead() throws Exception {
        assertEquals(0, dots(184, 92));
    }

    @Test
    public void genuineDotBeforeNextHeadStillLengthensPriorHead() throws Exception {
        assertEquals(1, dots(173, 92));
    }

    @Test
    public void adjacentStepInAnotherVoiceDoesNotOwnDurationDot() throws Exception {
        assertEquals(1, dots(184, 92, 108.5f));
    }

    private static int dots(int dotX, int dotY) throws Exception {
        return dots(dotX, dotY, 117f);
    }

    private static int dots(int dotX, int dotY, float nextY) throws Exception {
        Class<?> component = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        Constructor<?> constructor = component.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object head = constructor.newInstance(220, 139, 160, 93, 107, 150f, 100f);
        Object next =
                constructor.newInstance(
                        220, 173, 195, Math.round(nextY - 7), Math.round(nextY + 7), 184f, nextY);
        Object dot =
                constructor.newInstance(
                        13, dotX - 2, dotX + 2, dotY - 2, dotY + 2, (float) dotX, (float) dotY);
        Method count =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "countAugmentationDots",
                        List.class,
                        component,
                        float.class,
                        byte[].class,
                        int.class,
                        int.class,
                        boolean.class,
                        List.class,
                        List.class);
        count.setAccessible(true);
        return (int)
                count.invoke(
                        null,
                        List.of(dot),
                        head,
                        17f,
                        null,
                        420,
                        260,
                        false,
                        List.of(),
                        List.of(head, next));
    }
}
