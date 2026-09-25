// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original connected roofs, long slurs and unrelated print-grain components. */
public class ConnectedFermataRoofTest {
 static final int W=1000,H=1000;byte[] g=new byte[W*H],labels=new byte[W*H];
 public ConnectedFermataRoofTest(){Arrays.fill(g,(byte)255);for(int y=498;y<=502;y++)for(int x=498;x<=502;x++)g[y*W+x]=0;}
 void arc(int left,int right,int center,int top,int depth){for(int x=left;x<=right;x++){int y=top+Math.round(depth*(x-center)*(x-center)/(float)((right-left)*(right-left)/4));g[y*W+x]=0;}}
 int marks(){return NoteArticulationDetector.detect(labels,g,W,H,List.of(new NoteArticulationDetector.Anchor(500,530,16,0)))[0];}
 @Test public void compactConnectedRoofPreservesFermataDot(){arc(488,512,500,482,8);assertEquals(0,marks()&NoteArticulation.STACCATO);}
 @Test public void isolatedStaccatoRemainsStaccato(){assertEquals(NoteArticulation.STACCATO,marks());}
 @Test public void fiveIndependentSpecksDoNotMakeAFermata(){for(int x:new int[]{490,495,500,505,510})g[488*W+x]=0;assertEquals(NoteArticulation.STACCATO,marks());}
 @Test public void endOfLongSlurDoesNotShelterStaccato(){arc(450,514,482,480,16);assertEquals(NoteArticulation.STACCATO,marks());}
 @Test public void offCenterRoofCannotOwnDot(){arc(476,500,488,482,8);assertEquals(NoteArticulation.STACCATO,marks());}
 @Test public void invertedRoofPreservesFermataDot(){arc(488,512,500,482,8);byte[] old=g.clone();for(int y=0;y<H;y++)System.arraycopy(old,y*W,g,(H-1-y)*W,W);int[] mark=NoteArticulationDetector.detect(labels,g,W,H,List.of(new NoteArticulationDetector.Anchor(500,469,16,0)));assertEquals(0,mark[0]&NoteArticulation.STACCATO);}
 @Test public void pixelsStayUnchanged(){arc(488,512,500,482,8);var before=g.clone();marks();assertArrayEquals(before,g);}
 @Test public void roofEndsBesideDotRemainFermata(){arc(488,512,500,486,12);assertEquals(0,marks()&NoteArticulation.STACCATO);}
 @Test public void grayGrainCannotOverrideShelter(){for(int y=498;y<=502;y++)for(int x=498;x<=502;x++)g[y*W+x]=(byte)151;for(int x:new int[]{490,495,500,505,510})g[488*W+x]=0;assertEquals(0,marks()&NoteArticulation.STACCATO);}
 @Test public void distantTypographyCannotOverrideShelter(){for(int x:new int[]{490,495,500,505,510})g[488*W+x]=0;assertEquals(0,NoteArticulationDetector.detect(labels,g,W,H,List.of(new NoteArticulationDetector.Anchor(500,545,16,0)))[0]&NoteArticulation.STACCATO);}
 @Test public void faintIsolatedDotKeepsExistingRecognition(){for(int y=498;y<=502;y++)for(int x=498;x<=502;x++)g[y*W+x]=(byte)145;assertEquals(NoteArticulation.STACCATO,marks());}
}
