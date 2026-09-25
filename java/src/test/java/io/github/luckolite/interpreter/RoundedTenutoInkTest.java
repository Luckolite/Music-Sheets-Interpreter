// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original procedural short horizontal strokes with sparse raster edge pixels. */
public class RoundedTenutoInkTest {
 final int W=1000,H=400;byte[] gray=new byte[W*H],labels=new byte[W*H];
 public RoundedTenutoInkTest(){Arrays.fill(gray,(byte)255);}
 void rect(int l,int r,int t,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){gray[y*W+x]=20;labels[y*W+x]=5;}}
 int marks(){return NoteArticulationDetector.detect(labels,gray,W,H,List.of(new NoteArticulationDetector.Anchor(120,120,14,0)))[0];}
 void dash(){rect(109,131,97,99);rect(116,118,96,96);rect(128,128,100,100);}
 @Test public void sparseOuterRowsRetainDenseTenutoCore(){dash();assertEquals(NoteArticulation.TENUTO,marks());}
 @Test public void fractionalWidthAllowsOnlyOneRasterPixel(){rect(108,131,97,99);assertEquals(NoteArticulation.TENUTO,marks());}
 @Test public void slopingSparseStrokeIsNotDenseHorizontalDash(){for(int x=109;x<=131;x++)rect(x,x,94+(x-109)/4,94+(x-109)/4);assertEquals(0,marks());}
 @Test public void shortDotIsNotPromotedToTenuto(){rect(118,122,98,101);assertEquals(0,marks()&NoteArticulation.TENUTO);}
 @Test public void longLineCannotUseRasterTolerance(){rect(103,137,97,99);assertEquals(0,marks());}
 @Test public void broadFilledGlyphCannotBeTenuto(){rect(109,131,94,102);assertEquals(0,marks()&NoteArticulation.TENUTO);}
 @Test public void hollowLetterCannotUseDenseCore(){rect(109,131,94,95);rect(109,110,94,101);rect(130,131,94,101);rect(109,131,100,101);assertEquals(0,marks()&NoteArticulation.TENUTO);}
 @Test public void semanticStaffFragmentRetainsItsVeto(){dash();for(int i=0;i<labels.length;i++)if(labels[i]!=0)labels[i]=4;assertEquals(0,marks());}
 @Test public void actualNoteheadRetainsItsVeto(){dash();for(int i=0;i<labels.length;i++)if(labels[i]!=0)labels[i]=2;assertEquals(0,marks());}
 @Test public void distantOffAxisStrokeIsNotOwned(){rect(159,181,97,99);assertEquals(0,marks());}
 @Test public void farBelowLyricExtenderCannotUseRoundedRecovery(){rect(109,131,180,182);rect(116,118,179,179);rect(128,128,183,183);assertEquals(0,marks());}
 @Test public void pixelsRemainUnchanged(){dash();var before=gray.clone();var mask=labels.clone();marks();assertArrayEquals(before,gray);assertArrayEquals(mask,labels);}
}
