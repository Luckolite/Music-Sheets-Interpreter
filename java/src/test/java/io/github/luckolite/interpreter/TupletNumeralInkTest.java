// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tiny numeral geometry with pale printing and intersecting staff rules. */
public class TupletNumeralInkTest {
    private final String[] glyph={"..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "....#####...",
        "......####..", ".......####.", "##.....####.", "###....####.",
        "###....####.", "###....####.", ".###....###.", "..########..",
        "..########..", "....####...."};
    private byte[] image(int paper,int ink,boolean rule,boolean eight) {
        byte[] gray=new byte[400*240];Arrays.fill(gray,(byte)paper);
        for(int y=0;y<glyph.length;y++)for(int x=0;x<12;x++)
            if(glyph[y].charAt(x)=='#'||eight&&x<2&&y>1&&y<glyph.length-2)gray[(145+y)*400+119+x]=(byte)ink;
        if(rule)for(int x=60;x<195;x++)gray[150*400+x]=(byte)ink;
        return gray;
    }
    private List<ScoreNoteEvent> detect(byte[] gray,boolean quarters) {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(float x:new float[]{.25f,.3125f,.375f})notes.add(new ScoreNoteEvent(0,x,2,0,1,.4f,false,0,quarters?0:1,2,quarters?1:0));
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),gray,400,240);
    }
    @Test public void paleThreeUsesLocalPaperContrast(){for(var n:detect(image(245,180,false,false),false))assertEquals(3,n.tupletDivisor());}
    @Test public void thinIntersectingRuleDoesNotJoinTheNumeralToTheWholeStaff(){for(var n:detect(image(245,100,true,false),false))assertEquals(3,n.tupletDivisor());}
    @Test public void paleIntersectingRuleAlsoWorks(){for(var n:detect(image(245,180,true,false),false))assertEquals(3,n.tupletDivisor());}
    @Test public void gentlySlopedBlurredRuleAlsoWorks(){var gray=image(245,100,false,false);for(int x=75;x<=175;x++){int y=150+Math.round((x-125)*.05f);for(int d=-1;d<=1;d++)gray[(y+d)*400+x]=100;}for(var n:detect(gray,false))assertEquals(3,n.tupletDivisor());}
    @Test public void darkPaperDoesNotFillTheGlyphCounters(){for(var n:detect(image(180,110,false,false),false))assertEquals(3,n.tupletDivisor());}
    @Test public void paleEightCannotBecomeAThree(){for(var n:detect(image(245,180,false,true),false))assertEquals(1,n.tupletDivisor());}
    @Test public void crossingLineCannotOpenAClosedEight(){for(var n:detect(image(245,100,true,true),false))assertEquals(1,n.tupletDivisor());}
    @Test public void quarterNotesStillRequireBothBracketArms(){for(var n:detect(image(245,180,false,false),true))assertEquals(1,n.tupletDivisor());}
    @Test public void imageIsUnchanged(){var gray=image(245,180,true,false);var copy=gray.clone();detect(gray,false);assertArrayEquals(copy,gray);}
}
