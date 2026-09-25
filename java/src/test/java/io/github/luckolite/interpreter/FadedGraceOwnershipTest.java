// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original miniature groups with light shafts and independently dark ornaments. */
public class FadedGraceOwnershipTest {
 static final int W=300,H=240;byte[] gray=new byte[W*H],labels=new byte[W*H];
 public FadedGraceOwnershipTest(){Arrays.fill(gray,(byte)255);}
 void rect(int l,int r,int t,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)v;}
 void line(int ax,int ay,int bx,int by,int r,int v){int steps=Math.max(Math.abs(bx-ax),Math.abs(by-ay));for(int i=0;i<=steps;i++){int x=Math.round(ax+(bx-ax)*i/(float)steps),y=Math.round(ay+(by-ay)*i/(float)steps);rect(x-r,x+r,y-r,y+r,v);}}
 Object head(int cx,int cy,int halfW,int halfH,int area)throws Exception {var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(area,cx-halfW,cx+halfW,cy-halfH,cy+halfH,(float)cx,(float)cy);}
 ScoreNoteEvent event(int x,int beams,int staff){return new ScoreNoteEvent(0,x/(float)W,3,staff,1,.5f,false,0,beams,2,beams==0?1:0);}
 List<ScoreNoteEvent> mark(List<Object> heads,List<ScoreNoteEvent> src,String method)throws Exception {var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var dc=Class.forName(OmrScoreInterpreter.class.getName()+"$DetectedNote");var ctor=dc.getDeclaredConstructor(ScoreNoteEvent.class,hc,float.class);ctor.setAccessible(true);var detected=new ArrayList<>();for(int i=0;i<heads.size();i++)detected.add(ctor.newInstance(src.get(i),heads.get(i),16f));var events=new ArrayList<>(src);var m=OmrScoreInterpreter.class.getDeclaredMethod(method,byte[].class,int.class,int.class,List.class,List.class);m.setAccessible(true);m.invoke(null,gray,W,H,detected,events);return events;}
 List<ScoreNoteEvent> paired(int beams,boolean stemA,boolean stemB,int principalArea,int principalStaff)throws Exception {
  if(stemA)rect(106,107,110,150,180);if(stemB)rect(130,131,113,157,180);
  for(int x=106;x<=131;x++)for(int b=0;b<beams;b++){int y=110+Math.round((x-106)*.125f)+b*9;rect(x,x,y,y+3,30);}
  return mark(List.of(head(100,150,6,4,85),head(124,157,6,4,85),head(162,150,10,7,principalArea)),List.of(event(100,0,0),event(124,2,0),event(162,0,principalStaff)),"markPairedGraces");
 }
 boolean grace(ScoreNoteEvent n){return(n.articulations()&NoteOrnament.GRACE)!=0;}
 @Test public void paleAttachedPairUsesIndependentDoubleBeam()throws Exception {var e=paired(2,true,true,240,0);assertTrue(grace(e.get(0)));assertTrue(grace(e.get(1)));assertEquals(2,e.get(0).beamCount());assertEquals(0,e.get(0).unbeamedDurationBeats(),0);assertFalse(grace(e.get(2)));}
 @Test public void oneBeamDoesNotBecomeDoubleGrace()throws Exception {assertFalse(grace(paired(1,true,true,240,0).get(0)));}
 @Test public void missingOwnShaftIsRejected()throws Exception {assertFalse(grace(paired(2,false,true,240,0).get(0)));}
 @Test public void equallySmallPrincipalDoesNotProveGrace()throws Exception {assertFalse(grace(paired(2,true,true,90,0).get(0)));}
 @Test public void principalOnDifferentStaffCannotOwnGroup()throws Exception {assertFalse(grace(paired(2,true,true,240,1).get(0)));}
 @Test public void rawPixelsRemainUnchanged()throws Exception {paired(2,true,true,240,0);byte[] before=gray.clone();paired(2,true,true,240,0);assertArrayEquals(before,gray);}
 List<ScoreNoteEvent> solitary(boolean slash,boolean flag,int distance,boolean shaft)throws Exception {
  if(shaft)rect(100,101,85,120,180);if(flag)line(100,85,109,101,1,30);if(slash)line(94,106,111,89,1,30);
  return mark(List.of(head(96,120,5,4,65),head(96+distance,115,10,7,240)),List.of(event(96,0,0),event(96+distance,0,0)),"markSlashedGracePrefixes");
 }
 @Test public void paleShortSlashedFlagIsGraceAndEighth()throws Exception {var e=solitary(true,true,36,true);assertTrue(grace(e.get(0)));assertEquals(1,e.get(0).beamCount());assertFalse(grace(e.get(1)));}
 @Test public void slightlyWidePrefixNeedsSlashProof()throws Exception {assertTrue(grace(solitary(true,true,48,true).get(0)));}
 @Test public void missingSlashDoesNotGainGraceTiming()throws Exception {assertFalse(grace(solitary(false,true,48,true).get(0)));}
 @Test public void slashWithoutFlagIsRejected()throws Exception {assertFalse(grace(solitary(true,false,36,true).get(0)));}
 @Test public void detachedSlashFlagWithoutShaftIsRejected()throws Exception {assertFalse(grace(solitary(true,true,36,false).get(0)));}
 @Test public void distantPrincipalIsRejected()throws Exception {assertFalse(grace(solitary(true,true,70,true).get(0)));}
}
