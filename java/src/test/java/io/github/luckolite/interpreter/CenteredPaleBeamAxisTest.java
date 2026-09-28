// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** Original two-beam raster: preserve the independently traced shaft axis. */
public class CenteredPaleBeamAxisTest {
    private static final int W = 300, H = 240;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void rect(int left, int right, int top, int bottom, int ink) {
        for (int y = top; y <= bottom; y++)
            for (int x = left; x <= right; x++) gray[y * W + x] = (byte) ink;
    }

    private void setup() {
        Arrays.fill(gray, (byte) 250);
        rect(109, 113, 90, 150, 235);
        rect(111, 111, 89, 89, 235);
        rect(112, 190, 90, 96, 35);
        rect(112, 190, 102, 108, 35);
    }

    private Object detect(String method, boolean endpoint) throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        Object head = ctor.newInstance(180, 90, 112, 144, 156, 100f, 150f);
        Object staff = st.newInstance(40f, 104f, 16f);
        if (endpoint) {
            var m =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            method,
                            byte[].class,
                            byte[].class,
                            int.class,
                            int.class,
                            hc,
                            sc,
                            boolean.class);
            m.setAccessible(true);
            return m.invoke(null, labels, gray, W, H, head, staff, false);
        }
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        method, byte[].class, byte[].class, int.class, int.class, hc, sc);
        m.setAccessible(true);
        return m.invoke(null, labels, gray, W, H, head, staff);
    }

    @Test
    public void strongestTraceIsNotReplacedByFirstPassingFringe() throws Exception {
        setup();
        assertArrayEquals(new int[] {111, 89, -1}, (int[]) detect("paleStemToSupportedBeam", true));
        assertEquals(2, detect("detectBeamCount", false));
    }

    @Test
    public void disconnectedShaftStillFails() throws Exception {
        setup();
        rect(108, 114, 120, 131, 250);
        assertNull(detect("paleStemToSupportedBeam", true));
        assertEquals(0, detect("detectBeamCount", false));
    }

    @Test
    public void oneBeamDoesNotProveVeryPaleShaft() throws Exception {
        setup();
        rect(112, 190, 102, 108, 250);
        assertNull(detect("paleStemToSupportedBeam", true));
    }

    @Test
    public void broadShadowDoesNotProveShaft() throws Exception {
        setup();
        rect(100, 122, 112, 137, 225);
        assertNull(detect("paleStemToSupportedBeam", true));
    }

    @Test
    public void preservesBothSourceArrays() throws Exception {
        setup();
        var original = gray.clone();
        var originalLabels = labels.clone();
        detect("paleStemToSupportedBeam", true);
        detect("detectBeamCount", false);
        assertArrayEquals(original, gray);
        assertArrayEquals(originalLabels, labels);
    }
}
