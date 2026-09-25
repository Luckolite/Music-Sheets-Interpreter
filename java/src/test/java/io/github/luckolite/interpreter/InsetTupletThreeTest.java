// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original asymmetric two-bowl digit extends the existing synthetic italic fixture. */
public class InsetTupletThreeTest {
    private static String[] glyph(){return new String[]{
        "...######...","..#########.",".........###","........####",
        "........####",".......####.","......####..",".....####...",
        "....##......",".########...",".########...",".########...",
        ".########...","...####.....","...####.....","...####.....",
        "...####.....","..####......",".#####......","#####.......",
        "####........",".##........."};}
    private static boolean match(String[] rows)throws Exception {
        int w=12,h=rows.length;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(rows[y].charAt(x)=='#')gray[y*w+x]=0;
        var m=TripletRhythmDetector.class.getDeclaredMethod("looksLikeThree",byte[].class,int.class,int.class,int.class,int.class,int.class);m.setAccessible(true);
        return (boolean)m.invoke(null,gray,w,0,0,w,h);
    }
    @Test public void broadWaistAndInsetOpenLowerBowlStillMakeThree()throws Exception{assertTrue(match(glyph()));}
    @Test public void uprightHookOfLetterFDoesNotSupplyUpperCurvedCap()throws Exception{var r=glyph();for(int y=0;y<3;y++)r[y]=".........###";assertFalse(match(r));}
    @Test public void leftwardWaistTongueIsRequiredForInsetBowl()throws Exception{var r=glyph();for(int y=9;y<=12;y++)r[y]="...######...";assertFalse(match(r));}
    @Test public void lowerBowlMustRemainOpen()throws Exception{var r=glyph();for(int y=13;y<=18;y++)r[y]="#"+r[y].substring(1);assertFalse(match(r));}
    @Test public void flatFootOfTwoCannotSupplyCurvedLowerBowl()throws Exception{var r=glyph();r[20]="############";r[21]="############";assertFalse(match(r));}
    @Test public void upperLeftStemOfFiveStillFails()throws Exception{var r=glyph();for(int y=2;y<8;y++)r[y]="###.........";assertFalse(match(r));}
    @Test public void closedEightCannotSupplyUpperOpening()throws Exception{var r=glyph();for(int y=2;y<r.length-2;y++)r[y]="##"+r[y].substring(2);assertFalse(match(r));}
}
