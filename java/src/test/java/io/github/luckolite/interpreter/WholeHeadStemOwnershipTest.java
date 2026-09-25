// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original ellipses and disconnected/attached shafts, never source score pixels. */
public class WholeHeadStemOwnershipTest {
 final int W=280,H=240;byte[] labels=new byte[W*H],gray=new byte[W*H];
 void rect(int l,int t,int r,int b,int label,int shade){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){labels[y*W+x]=(byte)label;gray[y*W+x]=(byte)shade;}}
 void oval(int cx,int cy,int rx,int ry,boolean hollow){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++){double d=(x-cx)*(x-cx)/(double)(rx*rx)+(y-cy)*(y-cy)/(double)(ry*ry);if(d<=1){labels[y*W+x]=2;gray[y*W+x]=(byte)(hollow&&d<.45?245:20);}}}
 void page(boolean attached,int shade){Arrays.fill(gray,(byte)245);for(int y=100;y<=164;y+=16)rect(15,y,260,y,4,30);oval(130,100,16,8,true);if(attached)rect(144,45,145,101,1,shade);else{rect(140,30,141,90,1,20);oval(130,78,11,8,false);}}
 List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.02f,.98f,.05f,.9f)));}
 List<ScoreNoteEvent> lower(){return notes().stream().filter(n->Math.abs(n.pageY()*H-100)<4).toList();}
 @Test public void nearbyUpperShaftCannotTurnWholeIntoHalfOrUnison(){page(false,20);var n=lower();assertEquals(1,n.size());assertEquals(4,n.get(0).unbeamedDurationBeats(),0);}
 @Test public void attachedWideHalfRetainsItsActualStem(){page(true,20);var n=lower();assertEquals(1,n.size());assertEquals(2,n.get(0).unbeamedDurationBeats(),0);}
 @Test public void faintAttachedStemStillRetainsHalfDuration(){page(true,190);var n=lower();assertEquals(1,n.size());assertEquals(2,n.get(0).unbeamedDurationBeats(),0);}
 @Test public void callerPixelsAreNotRewritten(){page(false,20);var a=labels.clone();var b=gray.clone();notes();assertArrayEquals(a,labels);assertArrayEquals(b,gray);}
}
