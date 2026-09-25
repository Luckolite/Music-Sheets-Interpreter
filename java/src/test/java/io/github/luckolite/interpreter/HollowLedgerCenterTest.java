// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;import org.junit.Test;import static org.junit.Assert.*;
/** Original hollow/filled ellipses and ledger, no score-derived pixels. */
public class HollowLedgerCenterTest {
 final int W=160,H=160;byte[] gray=new byte[W*H];
 void page(int center,boolean hollow){Arrays.fill(gray,(byte)245);for(int y=center-10;y<=center+10;y++)for(int x=67;x<=93;x++){double d=(x-80)*(x-80)/169.+(y-center)*(y-center)/100.;if(d<=1)gray[y*W+x]=(byte)(hollow&&d<.5?245:20);}for(int x=58;x<=102;x++)gray[80*W+x]=20;}
 boolean check(){return HollowLedgerCenter.straddles(gray,W,H,80,80,20);}
 @Test public void hollowOvalHasEnclosedPocketsOnBothSides(){page(80,true);assertTrue(check());}
 @Test public void upperSpaceOvalHasNoPocketBelowLedger(){page(70,true);assertFalse(check());}
 @Test public void lowerSpaceOvalHasNoPocketAboveLedger(){page(90,true);assertFalse(check());}
 @Test public void filledOvalIsNotShiftedToLine(){page(80,false);assertFalse(check());}
 @Test public void openOutsidePaperCannotCountAsPocket(){Arrays.fill(gray,(byte)245);for(int x=58;x<=102;x++)gray[80*W+x]=20;assertFalse(check());}
 @Test public void malformedImageIsRejected(){assertFalse(HollowLedgerCenter.straddles(new byte[1],W,H,80,80,20));}
 @Test public void marginInputIsSafe(){assertFalse(HollowLedgerCenter.straddles(gray,W,H,2,2,20));}
 @Test public void imageIsUnchanged(){page(80,true);var before=gray.clone();check();assertArrayEquals(before,gray);}
 int decodedPitch()throws Exception{
  for(int y:new int[]{100,120})for(int x=58;x<=102;x++)gray[y*W+x]=20;
  var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
  var constructor=component.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);constructor.setAccessible(true);
  Object upperHalf=constructor.newInstance(150,70,90,70,81,80f,74f);
  var method=OmrScoreInterpreter.class.getDeclaredMethod("printedPitchStep",byte[].class,int.class,int.class,component,float.class,float.class);method.setAccessible(true);
  return (int)method.invoke(null,gray,W,H,upperHalf,220f,20f);
 }
 @Test public void decoderCorrectsUpperHalfMaskUsingActualHollowLedger()throws Exception{page(80,true);assertEquals(14,decodedPitch());}
 @Test public void decoderCannotPullNeighboringHollowSpaceOntoLedger()throws Exception{page(70,true);assertEquals(15,decodedPitch());}
}
