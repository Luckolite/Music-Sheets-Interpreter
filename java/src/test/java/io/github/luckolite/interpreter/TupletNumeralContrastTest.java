// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class TupletNumeralContrastTest {
    private byte[] scene(int paper,int ink){byte[] g=new byte[80*70];Arrays.fill(g,(byte)paper);
        for(int y=20;y<=37;y++)for(int x=30;x<=41;x++)
            if(x>=38||y<=22||y>=35||y>=27&&y<=29)g[y*80+x]=(byte)ink;
        return g;}
    @Test public void darkPrintedDigitOnGrayPaper(){assertTrue(TupletNumeralInk.hasGlyphContrast(scene(181,75),80,70,30,20,41,37,14));}
    @Test public void fadedPrintedDigitStillHasContrast(){assertTrue(TupletNumeralInk.hasGlyphContrast(scene(245,192),80,70,30,20,41,37,14));}
    @Test public void paperStainDoesNotBecomeThree(){assertFalse(TupletNumeralInk.hasGlyphContrast(scene(157,140),80,70,30,20,41,37,20));}
    @Test public void faintPageGrainDoesNotBecomeThree(){assertFalse(TupletNumeralInk.hasGlyphContrast(scene(172,160),80,70,30,20,41,37,20));}
    @Test public void oneDarkDustPixelCannotCertifyStain(){byte[] g=scene(172,160);g[25*80+38]=0;assertFalse(TupletNumeralInk.hasGlyphContrast(g,80,70,30,20,41,37,20));}
}
