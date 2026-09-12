// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original glyph geometry; never includes score crops or learned masks. */
public class MixedSharpSlotTest {
    static final int W=600,H=260;
    static void box(byte[][] a,int l,int t,int r,int b,int label) {
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){a[0][y*W+x]=(byte)label;a[1][y*W+x]=0;}
    }
    static byte[][] base(boolean doubleBar) {
        byte[][] a={new byte[W*H],new byte[W*H]};Arrays.fill(a[1],(byte)255);
        for(int y=100;y<=164;y+=16)box(a,20,y,580,y,4);
        box(a,122,95,122,169,1);if(doubleBar)box(a,128,95,128,169,1);
        box(a,222,121,235,130,2);box(a,235,81,236,125,1);
        return a;
    }
    static void sharp(byte[][] a) {
        box(a,151,76,153,121,5);box(a,160,72,162,117,5);
        box(a,148,86,165,90,2);box(a,148,102,165,106,2);
    }
    static byte[] normalize(byte[][] a) {
        return OmrScoreInterpreter.normalizeHeaderSymbols(a[0],a[1],W,H,List.of(new MeasureRegion(130f/W,560f/W,60f/H,210f/H)));
    }
    @Test public void doubleBarSharpLobesAreNotPlayedNotes() {
        byte[][] a=base(true);sharp(a);byte[] fixed=normalize(a);
        for(int y=72;y<=121;y++)for(int x=148;x<=165;x++)assertNotEquals(2,fixed[y*W+x]);
        assertEquals(2,fixed[125*W+226]);
    }
    @Test public void identicalGlyphWithoutDoubleBarIsLeftAlone() {
        byte[][] a=base(false);sharp(a);assertArrayEquals(a[0],normalize(a));
    }
    @Test public void ordinaryFirstPlayedChordIsRetained() {
        byte[][] a=base(true);box(a,149,103,163,113,2);box(a,149,119,163,129,2);box(a,162,72,164,126,1);
        assertArrayEquals(a[0],normalize(a));
    }
    @Test public void oneNaturalDoesNotBecomeASharp() {
        byte[][] a=base(true);box(a,151,76,153,111,5);box(a,160,88,162,123,5);box(a,153,88,159,92,5);box(a,153,106,159,110,5);
        assertArrayEquals(a[0],normalize(a));
    }
    @Test public void disconnectedNearbyInkKeepsItsOriginalLabel() {
        byte[][] a=base(true);sharp(a);box(a,149,137,150,139,1);
        byte[] fixed=normalize(a);
        assertEquals(3,fixed[88*W+151]);
        assertEquals(1,fixed[138*W+149]);
    }

    @Test public void sharpWithoutASeparatedPlayedHeadIsLeftAlone() {
        byte[][] a=base(true);sharp(a);
        for(int y=81;y<=130;y++)for(int x=222;x<=236;x++) {
            if(a[0][y*W+x]!=4){a[0][y*W+x]=0;a[1][y*W+x]=(byte)255;}
        }
        assertArrayEquals(a[0],normalize(a));
    }

}
