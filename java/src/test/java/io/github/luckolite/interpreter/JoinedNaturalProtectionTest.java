// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic natural and nearby rule fragments, not copied notation pixels. */
public class JoinedNaturalProtectionTest {
    private boolean protectedNatural(boolean complete, boolean contained, int unionArea, byte label)
            throws Exception {
        var f = new OverlappingAccidentalBoxesTest();
        f.natural(60, 30);
        if (!complete)
            for (int y = 59; y <= 62; y++) for (int x = 63; x < 70; x++) f.labels[y * f.W + x] = 0;
        Object original = f.original();
        var cc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        cc.setAccessible(true);
        Object union = cc.newInstance(unionArea, contained ? 58 : 65, 78, 29, 75, 66f, 51f);
        var ac = original.getClass().getDeclaredConstructors()[0];
        ac.setAccessible(true);
        Object candidate = ac.newInstance(union, label);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "containsCompleteNatural",
                        byte[].class,
                        int.class,
                        int.class,
                        original.getClass(),
                        List.class,
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, f.labels, f.W, f.H, candidate, List.of(original), 18f);
    }

    @Test
    public void completeNaturalSurvivesSmallMixedLabelUnion() throws Exception {
        assertTrue(protectedNatural(true, true, 400, (byte) 0));
    }

    @Test
    public void partialNaturalCannotVetoRecoveredFlat() throws Exception {
        assertFalse(protectedNatural(false, true, 400, (byte) 0));
    }

    @Test
    public void NeighboringNaturalOutsideUnionCannotVetoFlat() throws Exception {
        assertFalse(protectedNatural(true, false, 400, (byte) 0));
    }

    @Test
    public void smallNaturalInsideLargeCompositeIsNotSufficient() throws Exception {
        assertFalse(protectedNatural(true, true, 1000, (byte) 0));
    }

    @Test
    public void actualSemanticFlatIsNotARepairedUnion() throws Exception {
        assertFalse(protectedNatural(true, true, 400, (byte) 3));
    }

    private boolean printedProtection(boolean sourceNatural) throws Exception {
        var f = new OverlappingAccidentalBoxesTest();
        f.natural(60, 30);
        Object original = f.original();
        byte[] gray = new byte[f.labels.length];
        for (int i = 0; i < gray.length; i++) gray[i] = f.labels[i] == 0 ? (byte) 255 : 0;
        if (!sourceNatural)
            for (int y = 59; y <= 62; y++)
                for (int x = 63; x < 70; x++) gray[y * f.W + x] = (byte) 255;
        var cc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        cc.setAccessible(true);
        Object union = cc.newInstance(400, 58, 78, 29, 75, 66f, 51f);
        Object head = cc.newInstance(180, 85, 105, 44, 60, 95f, 52f);
        var ac = original.getClass().getDeclaredConstructors()[0];
        ac.setAccessible(true);
        Object candidate = ac.newInstance(union, (byte) 0);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "containsPrintedNatural",
                        byte[].class,
                        int.class,
                        int.class,
                        original.getClass(),
                        List.class,
                        List.class,
                        float.class);
        m.setAccessible(true);
        return (boolean)
                m.invoke(null, gray, f.W, f.H, candidate, List.of(original), List.of(head), 18f);
    }

    @Test
    public void printedNaturalProtectsItsUnionIndependentlyOfMask() throws Exception {
        assertTrue(printedProtection(true));
    }

    @Test
    public void missingPrintedConnectorCannotProtectAUnion() throws Exception {
        assertFalse(printedProtection(false));
    }
}
