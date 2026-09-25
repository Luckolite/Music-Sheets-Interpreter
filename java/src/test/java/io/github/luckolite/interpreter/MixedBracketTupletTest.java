// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original mixed-value bracket glyph; no score pixels. */
public class MixedBracketTupletTest {
    private static byte[] image(boolean left,boolean right,boolean numeral) {
        byte[] gray=new byte[400*240];Arrays.fill(gray,(byte)255);
        String[] glyph={"..#######...", ".##########.", "###......###", "####.....###",
            "####.....###", "####.....###", ".##.....####", ".......####.",
            "......####..", "....#####...", "....#####...", "....#####...",
            "......####..", ".......####.", "##.....####.", "###....####.",
            "###....####.", "###....####.", ".###....###.", "..########..",
            "..########..", "....####...."};
        if(numeral)for(int y=0;y<glyph.length;y++)for(int x=0;x<12;x++)if(glyph[y].charAt(x)=='#')gray[(145+y)*400+119+x]=0;
        if(left)for(int x=96;x<=116;x++)gray[148*400+x]=0;
        if(right)for(int x=133;x<=154;x++)gray[148*400+x]=0;
        return gray;
    }
    private static ScoreNoteEvent note(float x,float value) {return new ScoreNoteEvent(0,x,2,0,1,.4f,false,0,value<1?1:0,2,value<1?0:value);}
    private static List<ScoreNoteEvent> run(List<ScoreNoteEvent> notes,boolean left,boolean right,boolean numeral) {
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),image(left,right,numeral),400,240);
    }
    @Test public void quarterPlusEighthUsesThreeInTwo(){var out=run(List.of(note(.25f,1),note(.375f,.5f)),true,true,true);assertEquals(2.0/3,ScoreNoteTiming.writtenDurationBeats(out.get(0)),.0001);assertEquals(1.0/3,ScoreNoteTiming.writtenDurationBeats(out.get(1)),.0001);}
    @Test public void reversedEighthThenQuarterAlsoWorks(){for(var n:run(List.of(note(.25f,.5f),note(.375f,1)),true,true,true))assertEquals(3,n.tupletDivisor());}
    @Test public void halfPlusQuarterAlsoWorks(){for(var n:run(List.of(note(.25f,2),note(.375f,1)),true,true,true))assertEquals(3,n.tupletDivisor());}
    @Test public void missingLeftArmRejectsFingering(){for(var n:run(List.of(note(.25f,1),note(.375f,.5f)),false,true,true))assertEquals(1,n.tupletDivisor());}
    @Test public void missingRightArmRejectsFingering(){for(var n:run(List.of(note(.25f,1),note(.375f,.5f)),true,false,true))assertEquals(1,n.tupletDivisor());}
    @Test public void bracketWithoutThreeDoesNotImplyTuplet(){for(var n:run(List.of(note(.25f,1),note(.375f,.5f)),true,true,false))assertEquals(1,n.tupletDivisor());}
    @Test public void equalTwoNoteValuesAreNotThreeUnits(){for(var n:run(List.of(note(.25f,.5f),note(.375f,.5f)),true,true,true))assertEquals(1,n.tupletDivisor());}
    @Test public void graceIsNotAnOrdinaryTupletSlot(){for(var n:run(List.of(note(.25f,1),note(.375f,.5f).withArticulations(NoteOrnament.GRACE)),true,true,true))assertEquals(1,n.tupletDivisor());}
    @Test public void octaveMetadataSurvives(){var n=note(.25f,1).withOctaveShift(1);var out=run(List.of(n,note(.375f,.5f)),true,true,true);assertEquals(1,out.get(0).octaveShift());assertEquals(3,out.get(0).tupletDivisor());}
}
