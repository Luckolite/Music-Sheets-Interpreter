// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independently rendered Noto Serif digits (SIL OFL); no score-derived pixels.
 * Font license: training/fonts/NOTO-SERIF-OFL.txt in the public interpreter. */
public class RasterRestCountTest {
    static final String[] TWO={"..######..",".###..###.","###....###","###....###",".#.....###",".......###",".......##.",".......##.","......##..",".....##...","....##....","...##.....","..##......",".##......#",".##......#","##########","##########"};
    static final String[] THREE={"..#######..",".#########.","####...####","###....####",".##....####","......####.",".....####..","...#####...","....######.","......#####",".......####",".......####",".#.....####","##.....####","###....###.","##########.","..######..."};
    static final int W=220,H=220;
    private MeasureNumberReconciler.NumberToken read(String[] rows,boolean mirror) {
        byte[] gray=new byte[W*H];java.util.Arrays.fill(gray,(byte)255);
        int left=105-rows[0].length()/2;
        for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++)
            if(rows[y].charAt(x)=='#')gray[(65+y)*W+left+(mirror?rows[y].length()-1-x:x)]=0;
        for(int y=110;y<=174;y+=16)for(int x=50;x<=160;x++)gray[y*W+x]=0;
        for(int y=137;y<=147;y++)for(int x=80;x<=130;x++)gray[y*W+x]=0;
        var region=new MeasureRegion(50f/W,160f/W,100f/H,190f/H);
        return MultiMeasureRestDetector.standaloneCount(gray,W,H,new MultiMeasureRestDetector.RestBarCandidate(0,region));
    }
    @Test public void roundedThreeRetainsBothRasterizedOpenBowls(){assertEquals(3,read(THREE,false).value());}
    @Test public void printedTwoRemainsDistinctFromThree(){assertEquals(2,read(TWO,false).value());}
    @Test public void reflectedThreeDoesNotSupplyARightSpine(){assertNull(read(THREE,true));}
    @Test public void reflectedTwoDoesNotSupplyTheDescendingDiagonal(){assertNull(read(TWO,true));}
}
