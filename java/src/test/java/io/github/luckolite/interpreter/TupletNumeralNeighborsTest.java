// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original procedural letters/digits, not score or OCR fixtures. */
public class TupletNumeralNeighborsTest {
    private static final int W=160,H=100;
    private final byte[] g=new byte[W*H];
    public TupletNumeralNeighborsTest(){Arrays.fill(g,(byte)255);box(70,40,74,59);}
    private void box(int l,int t,int r,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=0;}
    private boolean text(){return TupletNumeralNeighbors.joinedText(g,W,H,70,40,74,59);}
    @Test public void solitaryNumeralHasNoTextNeighbor(){assertFalse(text());}
    @Test public void alignedDigitToLeftIsText(){box(58,40,61,59);assertTrue(text());}
    @Test public void alignedDigitToRightIsText(){box(82,40,85,59);assertTrue(text());}
    @Test public void separatelySpacedHorizontalFingerNumberIsNotText(){box(86,40,89,59);assertFalse(text());}
    @Test public void anotherAttackFingerNumberFarAwayIsNotText(){box(104,40,108,59);assertFalse(text());}
    @Test public void stackedFingerNumberIsNotHorizontalText(){box(58,64,61,83);assertFalse(text());}
    @Test public void bracketArmIsNotNeighboringDigit(){box(50,49,66,50);assertFalse(text());}
    @Test public void longNoteStemIsNotNeighboringDigit(){box(60,15,62,82);assertFalse(text());}
    @Test public void smallDotIsNotAnotherDigit(){box(64,48,66,50);assertFalse(text());}
    @Test public void helperPreservesRaster(){box(58,40,61,59);byte[] old=g.clone();text();assertArrayEquals(old,g);}
    @Test public void invalidBoundsDoNotInspectPixels(){assertFalse(TupletNumeralNeighbors.joinedText(g,W,H,-1,40,74,59));assertFalse(TupletNumeralNeighbors.joinedText(g,W,H,70,40,170,59));}
    private static final String[] THREE={
        "..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "......####..",
        ".......####.", "##.....####.", "###....####.", "###....####.",
        "###....####.", ".###....###.", "..########..", "....####...."};
    private static List<ScoreNoteEvent> group(boolean neighbor) {
        int w=400,h=240;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int start:neighbor?new int[]{100,116}:new int[]{116})
            for(int y=0;y<THREE.length;y++)for(int x=0;x<THREE[y].length();x++)if(THREE[y].charAt(x)=='#')gray[(145+y)*w+start+x]=0;
        var notes=List.of(new ScoreNoteEvent(0,.25f,2,0,1,.4f,false,0,2),new ScoreNoteEvent(0,.3125f,2,0,1,.4f,false,0,2),new ScoreNoteEvent(0,.375f,2,0,1,.4f,false,0,2));
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),gray,w,h);
    }
    @Test public void repeatedTempoDigitsDoNotRegroupNotes(){assertTrue(group(true).stream().allMatch(n->n.tupletDivisor()==1));}
    @Test public void isolatedPrintedThreeStillRegroupsNotes(){assertTrue(group(false).stream().allMatch(n->n.tupletDivisor()==3));}
}
