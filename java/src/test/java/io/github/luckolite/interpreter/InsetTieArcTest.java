// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original returning curves separated from each notehead by normal engraving clearance. */
public final class InsetTieArcTest {
    private boolean detect(int mode,boolean below,int insetLeft,int insetRight)throws Exception {
        int w=150,h=140,left=40,right=82;float cy=80,gap=14;int side=below?1:-1;
        byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        int first=left+insetLeft,last=right-insetRight;
        for(int x=first;x<=last;x++) {
            float t=(x-first)/(float)(last-first);
            if(mode==4&&t>.55)continue;
            float bend=mode==0?0:mode==2?5*t:mode==3?(t>.5?5:0):5*4*t*(1-t);
            int y=Math.round(cy+side*(7+bend));
            for(int yy=y-1;yy<=y+1;yy++){gray[yy*w+x]=0;labels[yy*w+x]=5;}
        }
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasPrintedTieArc",byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);
        method.setAccessible(true);return (boolean)method.invoke(null,labels,gray,w,h,left,right,cy,gap);
    }
    @Test public void detachedArcAboveRetainsBothEndpoints()throws Exception {assertTrue(detect(1,false,6,6));}
    @Test public void detachedArcBelowRetainsBothEndpoints()throws Exception {assertTrue(detect(1,true,6,6));}
    @Test public void asymmetricClearanceCanStillJoinHeads()throws Exception {assertTrue(detect(1,false,3,6));}
    @Test public void attachedArcKeepsExistingBehavior()throws Exception {assertTrue(detect(1,false,0,0));}
    @Test public void detachedStraightStrokeIsNotTie()throws Exception {assertFalse(detect(0,false,6,6));}
    @Test public void detachedSlopeIsNotTie()throws Exception {assertFalse(detect(2,false,6,6));}
    @Test public void detachedSteppedBeamIsNotTie()throws Exception {assertFalse(detect(3,false,6,6));}
    @Test public void onlyHalfAnArcIsNotTie()throws Exception {assertFalse(detect(4,false,6,6));}
    @Test public void farAwayCurveCannotBeStretchedToTheHeads()throws Exception {assertFalse(detect(1,false,13,13));}
}
