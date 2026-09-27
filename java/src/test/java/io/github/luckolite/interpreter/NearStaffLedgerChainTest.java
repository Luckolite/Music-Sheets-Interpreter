// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original neighboring staves and unrelated chord-ledger ink. */
public class NearStaffLedgerChainTest {
    private Object[] compact(boolean connected) throws Exception {
        int w = 260, h = 280;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y : new int[] {130, 146}) for (int x = 88; x <= 123; x++) gray[y * w + x] = 0;
        for (int y = connected ? 75 : 120; y <= 154; y++) gray[y * w + 115] = 0;
        var ct =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        ct.setAccessible(true);
        Object head = ct.newInstance(210, 94, 116, 147, 161, 105f, 154f);
        var st =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Staff")
                        .getDeclaredConstructors()[0];
        st.setAccessible(true);
        Object upper = st.newInstance(50f, 114f, 16f), lower = st.newInstance(174f, 238f, 16f);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "printedLedgerOwner",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        head.getClass());
        m.setAccessible(true);
        byte[] saved = gray.clone();
        Object owner = m.invoke(null, gray, w, h, List.of(upper, lower), head);
        assertArrayEquals(saved, gray);
        return new Object[] {owner, upper};
    }

    @Test
    public void compactChordLedgerChainFollowsStemBackToUpperStaff() throws Exception {
        var pair = compact(true);
        assertSame(pair[1], pair[0]);
    }

    @Test
    public void detachedCompactLedgerChainCannotStealNearbyNote() throws Exception {
        assertNull(compact(false)[0]);
    }

    @Test
    public void unrelatedLedgersDoNotStealNearbySpaceNote() throws Exception {
        int w = 260, h = 260;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y : new int[] {112, 128, 144}) for (int x = 86; x <= 124; x++) gray[y * w + x] = 0;
        for (int y = 90; y <= 140; y++) gray[y * w + 94] = 0;
        var ct =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        ct.setAccessible(true);
        Object head = ct.newInstance(200, 94, 116, 83, 97, 105f, 90f);
        var st =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Staff")
                        .getDeclaredConstructors()[0];
        st.setAccessible(true);
        Object upper = st.newInstance(0f, 64f, 16f), lower = st.newInstance(160f, 224f, 16f);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "printedLedgerOwner",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        head.getClass());
        m.setAccessible(true);
        assertNull(m.invoke(null, gray, w, h, List.of(upper, lower), head));
    }
}
