// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic tapered hook; no score raster or private musical material. */
public class TupletBlankRowTest {
    @Test public void erasedRuleCannotGiveATaperedHookAnIndentedWaist() throws Exception {
        String[] rows = {"..######", "...#####", "....####", ".....###",
                "......##", "......##", "........", "........",
                ".....##.", ".....##.", "....###.", "....###.",
                "....##..", "....##..", "....#...", "....#..."};
        int width=rows[0].length();
        byte[] pixels = new byte[width * rows.length];
        Arrays.fill(pixels, (byte)255);
        for (int y=0;y<rows.length;y++) for (int x=0;x<width;x++)
            if (rows[y].charAt(x)=='#') pixels[y*width+x]=0;
        var matcher = TripletRhythmDetector.class.getDeclaredMethod("looksLikeThree",
                byte[].class,int.class,int.class,int.class,int.class,int.class);
        matcher.setAccessible(true);
        assertFalse((boolean)matcher.invoke(null,pixels,width,0,0,width,rows.length));
    }
}
