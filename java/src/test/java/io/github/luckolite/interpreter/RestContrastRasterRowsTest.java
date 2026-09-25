// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rectangular ink rows test the raster support boundary, not a score glyph. */
public class RestContrastRasterRowsTest {
    static final int W=70,H=50;
    final byte[] gray=new byte[W*H];
    final boolean[] lines=new boolean[23];
    RestContrastRasterRowsTest(int paper,int ink){
        Arrays.fill(gray,(byte)paper);
        for(int y=0;y<23;y++)for(int x=34;x<=37;x++)gray[y*W+x]=(byte)ink;
        for(int y=6;y<=14;y++)lines[y]=true;
    }
    public RestContrastRasterRowsTest(){this(240,90);}
    boolean read(float gap){return CompactQuarterRestContour.hasContrastedInk(gray,W,30,40,0,22,lines,0,gap);}
    @Test public void fourteenRowsMeetTheNearestFractionalPixelMinimum(){assertTrue(read(14.36f));}
    @Test public void theHalfPixelBoundaryRemainsStrict(){assertTrue(read(14.49f));assertFalse(read(14.51f));}
    @Test public void oneFewerRowDoesNotReceiveAnExtraAllowance(){lines[15]=true;assertFalse(read(14.36f));}
    @Test public void anIntegerGapKeepsItsExactMinimum(){assertTrue(read(14));assertFalse(read(15));}
    @Test public void hiddenStaffRowsDoNotCountAsSupport(){Arrays.fill(lines,true);assertFalse(read(14.36f));}
    @Test public void supportFractionIsNotRelaxed(){for(int y:new int[]{0,1,2,3})for(int x=30;x<=40;x++)gray[y*W+x]=(byte)240;assertFalse(read(14.36f));}
    @Test public void darkPaperStillNeedsRealLocalContrast(){assertFalse(new RestContrastRasterRowsTest(189,168).read(14.36f));}
    @Test public void sourcePixelsAndLineEvidenceAreUnchanged(){var before=gray.clone();var mask=lines.clone();read(14.36f);assertArrayEquals(before,gray);assertArrayEquals(mask,lines);}
}
