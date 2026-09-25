// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original synthetic crossed-S polygon used by the existing segno ownership controls. */
public class NavigationSegnoGlyphsTest {
    static final int W=240,H=260;
    final byte[] gray=new byte[W*H];
    final List<PlayingTechniqueDetector.Staff> staffs=List.of(new PlayingTechniqueDetector.Staff(160,224,16,0,1));
    public NavigationSegnoGlyphsTest(){Arrays.fill(gray,(byte)255);}
    void line(int x1,int y1,int x2,int y2){int n=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));for(int i=0;i<=n;i++){int x=Math.round(x1+(x2-x1)*i/(float)n),y=Math.round(y1+(y2-y1)*i/(float)n);for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)gray[(y+dy)*W+x+dx]=0;}}
    void dot(int x,int y){for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)if(dx*dx+dy*dy<=5)gray[(y+dy)*W+x+dx]=0;}
    void body(){line(116,94,119,87);line(119,87,112,84);line(112,84,104,90);line(104,90,107,101);line(107,101,121,111);line(121,111,124,122);line(124,122,117,128);line(117,128,110,125);line(110,125,112,119);line(99,128,128,84);}
    @Test public void fullGlyphFoundWithoutSemanticHeadPrediction(){body();dot(99,108);dot(128,102);assertEquals(1,NavigationSegnoGlyphs.detect(gray,W,H,staffs).size());}
    @Test public void twoThresholdsDoNotDuplicateTheSameSign(){body();dot(99,108);dot(128,102);assertEquals(1,NavigationSegnoGlyphs.detect(gray,W,H,staffs).size());}
    @Test public void incompleteCrossedSDoesNotMakeADestination(){body();dot(128,102);assertEquals(List.of(),NavigationSegnoGlyphs.detect(gray,W,H,staffs));}
    @Test public void glyphOutsideStaffDirectionBandIsIgnored(){body();dot(99,108);dot(128,102);assertEquals(List.of(),NavigationSegnoGlyphs.detect(gray,W,H,List.of(new PlayingTechniqueDetector.Staff(50,114,16,0,1))));}
    @Test public void emptyAndInvalidInputAreSafe(){assertEquals(List.of(),NavigationSegnoGlyphs.detect(gray,W,H,staffs));assertEquals(List.of(),NavigationSegnoGlyphs.detect(null,W,H,staffs));}
    @Test public void rawInkIsNotMutated(){body();dot(99,108);dot(128,102);byte[] original=gray.clone();NavigationSegnoGlyphs.detect(gray,W,H,staffs);assertArrayEquals(original,gray);}
}
