// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original note ellipses, shortened stems, staff rules and ornamental prefixes. */
public class GracePrefixGeometryTest {
 static final int W=700,H=320;
 static final List<MeasureRegion> M=List.of(new MeasureRegion(0,1,.05f,.95f));
 static class Page {
  byte[] l=new byte[W*H],g=new byte[W*H];
  Page(){this(false);}
  Page(boolean fractional){Arrays.fill(g,(byte)255);for(int y:fractional?new int[]{112,129,146,162,179}:new int[]{112,128,144,160,176})for(int x=15;x<685;x++){pixel(x,y,4);pixel(x,y+1,4);}}
  void pixel(int x,int y,int label){l[y*W+x]=(byte)label;g[y*W+x]=0;}
  void head(float cx,int cy,float rx,int ry,boolean open){for(int y=cy-ry;y<=cy+ry;y++)for(int x=(int)Math.ceil(cx-rx);x<=cx+rx;x++)if(Math.pow((x-cx)/rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){pixel(x,y,2);if(open&&Math.pow((x-cx)/(rx-3),2)+Math.pow((y-cy)/(double)(ry-3),2)<1)g[y*W+x]=(byte)255;}}
  void stem(int x,int y,int length,boolean semantic){for(int row=y-length;row<=y;row++)pixel(x,row,semantic?1:0);}
  void main(int x,int y){head(x,y,11,7,true);stem(x+11,y,48,true);}
  void small(int x,int y,boolean semantic){head(x,y,7,5,false);stem(x+7,y,28,semantic);}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(l,g,W,H,M).notes();}
  ScoreNoteEvent at(float x){return notes().stream().filter(n->Math.abs(n.positionInMeasure()*W-x)<3).findFirst().orElse(null);}
 }
 static Page possibleDot(boolean stem){var p=new Page();p.main(70,144);p.head(120,144,6,5,false);if(stem)p.stem(126,144,28,false);p.main(164,128);return p;}
 static Page run(int length,boolean principal){var p=new Page();for(int i=0;i<length;i++)p.small(100+i*25,144,true);if(principal)p.main(100+length*25,136);return p;}
 @Test public void anUnlabelledShortStemProtectsTheGraceFromDotDemotion(){var p=possibleDot(true);assertNotNull(p.at(120));assertEquals(3,p.notes().size());}
 @Test public void protectedGraceRetainsItsPitch(){var p=possibleDot(true);assertNotNull(p.at(120));assertEquals(4,p.at(120).staffStep());}
 @Test public void protectedHeadBecomesAnOrnament(){var p=possibleDot(true);assertNotNull(p.at(120));assertTrue((p.at(120).articulations()&NoteOrnament.GRACE)!=0);}
 @Test public void anActualStemlessDotIsStillDemoted(){assertNull(possibleDot(false).at(120));}
 @Test public void aShortInkSmudgeDoesNotProtectTheDot(){var p=possibleDot(false);p.stem(126,144,9,false);assertNull(p.at(120));}
 @Test public void fiveHeadPrefixIsNotTruncated(){var ns=run(5,true).notes();assertEquals(5,ns.stream().filter(n->(n.articulations()&NoteOrnament.GRACE)!=0).count());}
 @Test public void eightHeadPrefixIsNotTruncated(){var ns=run(8,true).notes();assertEquals(8,ns.stream().filter(n->(n.articulations()&NoteOrnament.GRACE)!=0).count());}
 @Test public void uniformlySmallNotesWithoutPrincipalRemainMetrical(){var ns=run(8,false).notes();assertEquals(8,ns.size());assertTrue(ns.stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));}
 @Test public void fiveGracesShareOneBudget(){var ns=run(5,true).notes();for(int i=0;i<5;i++)assertEquals(.05,ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(i),ns,3),.0001);assertEquals(1.75,ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(5),ns,3),.0001);}
 @Test public void distantLargeHeadDoesNotTurnAnEntirePhraseIntoGrace(){var p=run(5,false);p.main(360,136);assertTrue(p.notes().stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));}
 @Test public void pixelRoundedGraceWidthPreservesItsLedgerPitch(){var p=new Page(true);for(int y:new int[]{196,213})for(int x=120;x<=141;x++)p.pixel(x,y,4);p.head(130.5f,213,7.5f,5,false);p.stem(138,213,31,true);p.main(175,188);var n=p.at(130.5f);assertNotNull(n);assertEquals(-4,n.staffStep());assertTrue((n.articulations()&NoteOrnament.GRACE)!=0);}
 @Test public void shortStemRecognitionDoesNotModifyTheInput(){var p=possibleDot(true);var l=p.l.clone();var g=p.g.clone();p.notes();assertArrayEquals(l,p.l);assertArrayEquals(g,p.g);}
}
