// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdjacentSextupletTest {
    List<ScoreNoteEvent> read(boolean second)throws Exception {
        int w=800,h=240;byte[] g=new byte[w*h];Arrays.fill(g,(byte)255);
        var draw=SextupletRecognitionTest.class.getDeclaredMethod("glyph",boolean.class,boolean.class);draw.setAccessible(true);
        byte[] glyph=(byte[])draw.invoke(null,false,true);
        for(int left:second?new int[]{146,326}:new int[]{146})for(int y=0;y<28;y++)for(int x=0;x<18;x++)g[(140+y)*w+left+x]=glyph[y*18+x];
        List<ScoreNoteEvent> notes=new ArrayList<>();
        for(int i=0;i<12;i++)notes.add(new ScoreNoteEvent(0,(80+i*30)/800f,2,0,1,.4f,false,0,1,2,0,1));
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),g,w,h);
    }
    @Test public void twoPrintedSixesDescribeTwoAdjacentGroups()throws Exception{assertTrue(read(true).stream().allMatch(n->n.tupletDivisor()==6));}
    @Test public void loneSixDoesNotTruncateTwelveAttacks()throws Exception{assertTrue(read(false).stream().allMatch(n->n.tupletDivisor()==1));}
    @Test public void pairedSixesFillFourQuarterBeats()throws Exception{assertEquals(4,read(true).stream().mapToDouble(ScoreNoteTiming::writtenDurationBeats).sum(),1e-9);}
}
