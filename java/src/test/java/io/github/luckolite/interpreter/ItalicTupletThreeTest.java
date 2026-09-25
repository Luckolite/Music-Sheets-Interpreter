// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original asymmetric two-bowl glyph, deliberately narrower at the lower bowl. */
public class ItalicTupletThreeTest {
    private static String[] glyph(){return new String[]{
        "...######...","..#########.",".........###","........####",
        "........####",".......####.","......####..",".....####...",
        "....##......","...###......","...###......","....##......",
        "....###.....","...####.....","...####.....","...####.....",
        "...####.....","..####......",".#####......","#####.......",
        "####........",".##........."};}
    private static boolean match(String[] rows)throws Exception {
        int w=12,h=rows.length;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(rows[y].charAt(x)=='#')gray[y*w+x]=0;
        var m=TripletRhythmDetector.class.getDeclaredMethod("looksLikeThree",byte[].class,int.class,int.class,int.class,int.class,int.class);m.setAccessible(true);
        return (boolean)m.invoke(null,gray,w,0,0,w,h);
    }
    @Test public void lowerOpeningUsesItsOwnBowlExtent()throws Exception {assertTrue(match(glyph()));}
    @Test public void roundedBowlMayContinueIntoPenultimateRasterRow()throws Exception {var rows=glyph();rows[20]="...#####....";assertTrue(match(rows));}
    @Test public void closedEightStillFails()throws Exception {var rows=glyph();for(int y=2;y<rows.length-2;y++)rows[y]="##"+rows[y].substring(2);assertFalse(match(rows));}
    @Test public void flatFootOfTwoStillFails()throws Exception {var rows=glyph();rows[20]="############";rows[21]="############";assertFalse(match(rows));}
    @Test public void solidUpperLeftOfFiveStillFails()throws Exception {var rows=glyph();for(int y=2;y<8;y++)rows[y]="###.........";assertFalse(match(rows));}
}
