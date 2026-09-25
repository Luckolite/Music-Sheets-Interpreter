// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlyphInkContrastTest {
    private float[] shape(float paper,float ink){float[] values=new float[100];Arrays.fill(values,paper);
        for(int i=0;i<35;i++)values[i]=ink;values[35]=(paper+ink)*.5f;return values;}
    @Test public void blackPrintOnWhitePaperUnchanged(){float[] m=GlyphInkContrast.mask(shape(255,0));assertEquals(1,m[0],0);assertEquals(.5,m[35],.001);assertEquals(0,m[99],0);}
    @Test public void grayPaperDoesNotFillGlyphBackground(){float[] m=GlyphInkContrast.mask(shape(185,45));assertEquals(1,m[0],0);assertEquals(.5,m[35],.001);assertEquals(0,m[99],0);}
    @Test public void fadedInkRetainsSoftEdges(){float[] m=GlyphInkContrast.mask(shape(246,186));assertEquals(1,m[0],0);assertEquals(.5,m[35],.001);}
    @Test public void paperTextureDoesNotBecomeCharacter(){float[] m=GlyphInkContrast.mask(shape(171,155));for(float value:m)assertEquals(0,value,0);}
    @Test public void emptyPatchIsEmpty(){float[] values=new float[200];Arrays.fill(values,190);for(float value:GlyphInkContrast.mask(values))assertEquals(0,value,0);}
}
