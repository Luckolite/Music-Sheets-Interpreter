// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original faint shaft with a lighter paper tail beyond the genuine flag endpoint. */
public class BoundedPaleFlagTest extends PaleFlagRootTest {
    void bounded(boolean root, boolean hook) {
        setup(root, hook, true);
        rect(109, 111, 97, 150, 210);
        rect(109, 111, 48, 79, 235);
    }

    @Test
    public void lighterPaperTailCannotHideRootedFlag() throws Exception {
        bounded(true, true);
        assertEquals(1, beams());
    }

    @Test
    public void independentlyProvedEndpointStopsAtFlagNotPaperTail() throws Exception {
        bounded(true, true);
        int[] endpoint = endpoint();
        assertNotNull(endpoint);
        assertEquals(80, endpoint[1]);
        assertEquals(-1, endpoint[2]);
    }

    int[] endpoint() throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object head = ctor.newInstance(180, 90, 110, 144, 156, 100f, 150f);
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        Object staff = st.newInstance(70f, 134f, 16f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "paleStemToSupportedBeam",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        sc,
                        boolean.class);
        method.setAccessible(true);
        return (int[]) method.invoke(null, labels, g, W, H, head, staff, false);
    }

    @Test
    public void noRootDoesNotBorrowDetachedHook() throws Exception {
        bounded(false, true);
        assertEquals(0, beams());
    }

    @Test
    public void noHookCannotBecomeFlag() throws Exception {
        bounded(false, false);
        assertEquals(0, beams());
    }

    @Test
    public void broadShadowCannotSupplyIndependentShaft() throws Exception {
        bounded(true, true);
        rect(100, 121, 108, 135, 210);
        assertEquals(0, beams());
    }

    @Test
    public void brokenShaftDoesNotReachFlag() throws Exception {
        bounded(true, true);
        rect(108, 112, 125, 134, 255);
        assertEquals(0, beams());
    }

    @Test
    public void pixelsRemainUnchanged() throws Exception {
        bounded(true, true);
        var before = g.clone();
        var beforeLabels = labels.clone();
        beams();
        assertArrayEquals(before, g);
        assertArrayEquals(beforeLabels, labels);
    }
}
