// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original numeral and attack coordinates exercise page-width independence. */
public class CompactTripletSpacingTest {
    private static final String[] THREE={
        "..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "......####..",
        ".......####.", "##.....####.", "###....####.", "###....####.",
        "###....####.", ".###....###.", "..########..", "....####...."};
    private List<ScoreNoteEvent> run(int w,int spacing,boolean glyph) {
        int h=240,cx=400+spacing;byte[] g=new byte[w*h];Arrays.fill(g,(byte)255);
        if(glyph)for(int y=0;y<THREE.length;y++)for(int x=0;x<THREE[y].length();x++)
            if(THREE[y].charAt(x)=='#')g[(145+y)*w+cx-6+x]=0;
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int i=0;i<3;i++)notes.add(new ScoreNoteEvent(0,(400+i*spacing)/(float)w,2,0,1,.4f,
                false,0,2,2,0,1,0,0,30,false,0,false,1));
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.2f,.6f)),g,w,h);
    }
    @Test public void wideMeasureKeepsThreeDistinctCompactAttacks(){assertTrue(run(1600,28,true).stream().allMatch(n->n.tupletDivisor()==3));}
    @Test public void sameRasterGroupInANarrowerMeasureAgrees(){assertTrue(run(800,28,true).stream().allMatch(n->n.tupletDivisor()==3));}
    @Test public void numeralIsStillRequired(){assertTrue(run(1600,28,false).stream().allMatch(n->n.tupletDivisor()==1));}
    @Test public void mergedChordColumnsDoNotBecomeTriplets(){assertTrue(run(1600,18,true).stream().allMatch(n->n.tupletDivisor()==1));}
    @Test public void copiedNotesPreserveOctaveDisplacement(){assertTrue(run(1600,28,true).stream().allMatch(n->n.octaveShift()==1));}
    @Test public void compactSixteenthsHaveTheirActualTripletLength(){var notes=run(1600,28,true);for(var n:notes)assertEquals(1./6,ScoreNoteTiming.writtenDurationBeats(n),1e-9);}
}
