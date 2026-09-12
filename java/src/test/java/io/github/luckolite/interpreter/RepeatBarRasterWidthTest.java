// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original repeat bar with a flat-shaped island in its semantic mask. */
public class RepeatBarRasterWidthTest {
    private static final int W = 400, H = 240;

    private static Object call(String name, Object... args) throws Exception {
        for (var method : OmrScoreInterpreter.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Object construct(String name, Object... args) throws Exception {
        var ctor = Class.forName(OmrScoreInterpreter.class.getName() + "$" + name)
                .getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    @SuppressWarnings("unchecked")
    private static List<ScoreKeyChange> changes(float gap, boolean fullBar) throws Exception {
        byte[] labels = new byte[W * H], gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        int bottom = Math.round(80 + 4 * gap);
        for (int y = 80; y <= bottom; y++) {
            gray[y * W + 90] = 0;
            gray[y * W + 96] = 0;
            if (fullBar) for (int x = 100; x <= 108; x++) gray[y * W + x] = 0;
        }
        for (int line = 0; line < 5; line++) {
            int y = Math.round(80 + line * gap);
            for (int x = 20; x < 380; x++) gray[y * W + x] = 0;
        }
        for (int y = 111; y <= 132; y++) {
            int right = y < 123 ? 101 : 108;
            for (int x = 100; x <= right; x++) {
                labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
                gray[y * W + x] = 0;
            }
        }
        var components = (List<?>) call("findComponents", labels, W, H, (byte) 5);
        assertEquals(1, components.size());
        var candidate = construct("AccidentalCandidate", components.get(0), (byte) 5);
        assertEquals(true, call("isFlatGlyph", labels, W, H, candidate, gap));
        var staff = construct("Staff", 80f, 80 + 4 * gap, gap);
        var head = construct("Component", 20, 160, 166, 104, 108, 163f, 106f);
        return (List<ScoreKeyChange>) call("detectKeyChanges", labels, gray, W, H,
                List.of(new MeasureRegion(.25f, .90f, .25f, .65f)),
                List.of(staff), List.of(candidate), List.of(head));
    }

    @Test public void fractionalStaffGapDoesNotTurnARepeatBarIntoAFlat() throws Exception {
        assertTrue(changes(13.75f, true).isEmpty());
    }

    @Test public void nearbyIntegerGapAlsoRejectsTheBar() throws Exception {
        assertTrue(changes(14f, true).isEmpty());
    }

    @Test public void aPrintedFlatAfterADoubleBarStillChangesKey() throws Exception {
        assertEquals(List.of(new ScoreKeyChange(0, -1)), changes(13.75f, false));
    }
}
