// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated shaded-paper and notation samples. */
public class PrintedNoteContrastTest {
    private boolean texture(int background,int ink,boolean hollow,boolean speck) {
        int w=100,h=100;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)background);
        for(int y=40;y<=54;y++)for(int x=40;x<=60;x++)
            if(!hollow||y<43||y>51||x<43||x>57)gray[y*w+x]=(byte)ink;
        if(speck)gray[45*w+50]=0;
        return PrintedNoteContrast.paperTexture(gray,w,h,40,40,60,54);
    }
    @Test public void shadedPaperNoiseIsRejected(){assertTrue(texture(175,166,false,false));}
    @Test public void deepShadowNoiseIsRejected(){assertTrue(texture(130,119,false,false));}
    @Test public void deepShadowPrintedHeadIsPreserved(){assertFalse(texture(130,35,false,false));}
    @Test public void solidBlackPrintedRegionIsNotPaperTexture(){assertFalse(texture(0,0,false,false));}
    @Test public void isolatedDustDoesNotValidateTexture(){assertTrue(texture(175,166,false,true));}
    @Test public void printedHeadOnShadowIsPreserved(){assertFalse(texture(175,25,false,false));}
    @Test public void hollowHeadOnShadowIsPreserved(){assertFalse(texture(175,25,true,false));}
    @Test public void fadedHeadOnWhiteIsPreserved(){assertFalse(texture(255,210,false,false));}
    @Test public void lowerContrastPrintedInkIsPreserved(){assertFalse(texture(185,150,false,false));}
    @Test public void missingPixelsAreNotEvidence(){assertFalse(PrintedNoteContrast.paperTexture(null,100,100,40,40,60,54));}
    @Test public void semanticHeadInsidePrintedOutlineIsPreserved(){
        int w=100;byte[] gray=new byte[w*w];Arrays.fill(gray,(byte)210);
        for(int y=38;y<=56;y++)for(int x=38;x<=62;x++)
            if(x==38||x==62||y==38||y==56)gray[y*w+x]=90;
        for(int y=40;y<=54;y++)for(int x=40;x<=60;x++)gray[y*w+x]=(byte)198;
        assertFalse(PrintedNoteContrast.paperTexture(gray,w,w,40,40,60,54));
    }
    @Test public void oneSidedNearbySlurDoesNotValidateBlankPaper(){
        int w=100;byte[] gray=new byte[w*w];Arrays.fill(gray,(byte)145);
        for(int x=38;x<=62;x++)gray[38*w+x]=35;
        assertTrue(PrintedNoteContrast.paperTexture(gray,w,w,40,40,60,54));
    }
}
