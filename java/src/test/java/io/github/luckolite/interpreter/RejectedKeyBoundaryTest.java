// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original flat run with an independently rejected head-shaped ink fragment. */
public final class RejectedKeyBoundaryTest {
 static final int W=400,H=240;static final List<MeasureRegion>M=List.of(new MeasureRegion(.20f,.90f,.25f,.55f));
 static byte[] page(int extraY){byte[] a=new byte[W*H];for(int y=80;y<=120;y+=10)for(int x=20;x<380;x++)a[y*W+x]=4;for(int x:new int[]{80,84})for(int y=78;y<=122;y++)a[y*W+x]=1;int[] xs={92,108,124,140},ys={76,83,73,80};for(int i=0;i<4;i++)flat(a,xs[i],ys[i]);head(a,190,110);if(extraY>=0)head(a,138,extraY);return a;}
 static void head(byte[]a,int cx,int cy){for(int y=cy-2;y<=cy+2;y++)for(int x=cx-3;x<=cx+3;x++)a[y*W+x]=2;}
 static void flat(byte[]a,int l,int t){for(int y=t;y<=t+20;y++)a[y*W+l]=3;for(int y=t+10;y<=t+17;y++){int reach=y<=t+13?y-(t+9):t+18-y;for(int x=l+1;x<=l+Math.max(2,reach);x++)a[y*W+x]=3;}}
 static byte[] raw(byte[]a){byte[]g=new byte[a.length];for(int i=0;i<a.length;i++)g[i]=a[i]==0?(byte)255:0;return g;}
 static OmrScoreInterpreter.Analysis analyze(int y){byte[]a=page(y);return OmrScoreInterpreter.analyze(a,raw(a),W,H,M);}
 @Test public void rejectedHighHeadDoesNotTruncateKey(){var a=analyze(50);assertEquals(1,a.notes().size());assertEquals(List.of(new ScoreKeyChange(0,-4)),a.keyChanges());}
 @Test public void rejectedLowHeadDoesNotTruncateKey(){var a=analyze(150);assertEquals(1,a.notes().size());assertEquals(List.of(new ScoreKeyChange(0,-4)),a.keyChanges());}
 @Test public void normalFourFlatSignatureRemains(){assertEquals(List.of(new ScoreKeyChange(0,-4)),analyze(-1).keyChanges());}
 @Test public void aRealFirstNoteStillEndsTheSignature(){var a=analyze(110);assertEquals(2,a.notes().size());assertEquals(List.of(new ScoreKeyChange(0,-3)),a.keyChanges());}
 @Test public void inputArraysAreUnchanged(){byte[]a=page(50),g=raw(a),saved=a.clone(),rawSaved=g.clone();OmrScoreInterpreter.analyze(a,g,W,H,M);assertArrayEquals(saved,a);assertArrayEquals(rawSaved,g);}
}
