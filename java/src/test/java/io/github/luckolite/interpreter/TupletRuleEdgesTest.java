// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural numerals and variable-height horizontal rule bands. */
public class TupletRuleEdgesTest {
    private static final int W=400,H=240;
    private static final String[] THREE={"..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "....#####...",
        "......####..", ".......####.", "##.....####.", "###....####.",
        "###....####.", "###....####.", ".###....###.", "..########..",
        "..########..", "....####...."};
    private static byte[] image(boolean closed,int ink) {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)245);
        for(int y=0;y<THREE.length;y++)for(int x=0;x<12;x++)
            if(THREE[y].charAt(x)=='#'||closed&&x<2&&y>1&&y<THREE.length-2)g[(145+y)*W+119+x]=(byte)ink;
        return g;
    }
    private static void box(byte[] g,int l,int t,int r,int b,int ink){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=(byte)ink;}
    private static void rule(byte[] g,int y,int ink){box(g,60,y,195,y,ink);box(g,60,y-1,126,y-1,ink);box(g,126,y+1,195,y+1,ink);}
    private static List<ScoreNoteEvent> detect(byte[] g) {
        var notes=List.of(new ScoreNoteEvent(0,.25f,2,0,1,.4f,false,0,1),new ScoreNoteEvent(0,.3125f,2,0,1,.4f,false,0,1),new ScoreNoteEvent(0,.375f,2,0,1,.4f,false,0,1));
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),g,W,H);
    }
    private static void assertDivisor(int divisor,byte[] g){for(var n:detect(g))assertEquals(divisor,n.tupletDivisor());}
    private static TupletNumeralInk.Window window(byte[] g){return TupletNumeralInk.ruleEdgesWindow(g,W,H,100,150,96,96,12,165);}
    private static int at(TupletNumeralInk.Window w,int x,int y){return w.pixels()[(y-w.top())*w.width()+x-w.left()]&255;}
    @Test public void adjacentRuleEdgesCannotRetainTheFullCrossbar(){var g=image(false,100);rule(g,150,100);assertDivisor(3,g);}
    @Test public void paleVariableBandRetainsTheOriginalNumeral(){var g=image(false,185);rule(g,150,185);assertDivisor(3,g);}
    @Test public void thinRuleUnderTheFootRetainsTheTerminalInk(){var g=image(false,100);rule(g,165,100);assertEquals(0,at(window(g),123,165));assertEquals(0,at(window(g),123,166));}
    @Test public void legacyMaskRemainsSeparateFromTheAdditiveRetry(){var g=image(false,100);rule(g,165,100);var old=TupletNumeralInk.window(g,W,H,100,150,96,96,12,165);assertEquals(255,at(old,123,165));assertEquals(0,at(window(g),123,165));}
    @Test public void closedEightCannotBecomeAnOpenThree(){var g=image(true,100);rule(g,150,100);assertDivisor(1,g);}
    @Test public void thickBeamIsNotErasedAsAStaffRule(){var g=image(false,100);box(g,60,147,195,153,100);assertEquals(0,at(window(g),90,150));}
    @Test public void shortBracketDoesNotSupplyFullRuleCoverage(){var g=image(false,100);box(g,88,150,107,151,100);assertEquals(0,at(window(g),95,150));}
    @Test public void verticalGlyphContinuationSurvivesMasking(){var g=image(false,100);rule(g,150,100);box(g,99,141,101,160,100);assertEquals(0,at(window(g),100,150));}
    @Test public void blankPaperDoesNotChange(){byte[] g=new byte[W*H];Arrays.fill(g,(byte)245);var v=window(g);for(byte p:v.pixels())assertEquals(255,p&255);}
    @Test public void sourceRasterRemainsUntouched(){var g=image(false,100);rule(g,150,100);byte[] before=g.clone();detect(g);assertArrayEquals(before,g);}
}
