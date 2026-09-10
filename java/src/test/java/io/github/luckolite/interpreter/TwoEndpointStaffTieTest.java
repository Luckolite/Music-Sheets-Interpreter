// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic returning curves whose two ends merge with one staff rule. */
public class TwoEndpointStaffTieTest {
    private boolean thinCurve(boolean below)throws Exception {
        int w=140,h=140;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int x=0;x<w;x++)for(int y=74;y<=75;y++) {
            int yy=below?160-y:y;gray[yy*w+x]=0;labels[yy*w+x]=4;
        }
        for(int x=45;x<=85;x++) {
            float t=(x-45)/40f;int top=Math.round(75-4*4*t*(1-t));
            for(int y=top;y<=top+1;y++) {
                int yy=below?160-y:y;gray[yy*w+x]=0;if(labels[yy*w+x]!=4)labels[yy*w+x]=5;
            }
        }
        var m=OmrScoreInterpreter.class.getDeclaredMethod("hasContinuousTieArc",byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);
        m.setAccessible(true);return (boolean)m.invoke(null,labels,gray,w,h,45,85,below?79.8f:80.2f,13.8f);
    }
    @Test public void twoPixelCurveRetainsBothRuleCoveredEndsAbove()throws Exception {assertTrue(thinCurve(false));}
    @Test public void twoPixelCurveRetainsBothRuleCoveredEndsBelow()throws Exception {assertTrue(thinCurve(true));}
    private boolean detect(int mode,boolean below)throws Exception {
        int w=140,h=140,left=45,right=85;float cy=80,gap=14;int side=below?1:-1;
        byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        int rule=Math.round(cy+side*6);
        for(int x=0;x<w;x++)for(int y=rule-1;y<=rule+1;y++){gray[y*w+x]=0;labels[y*w+x]=4;}
        for(int x=left;x<=right;x++) {
            float t=(x-left)/(float)(right-left);
            if(mode==0||mode==4&&t>.5)continue;
            float bend=mode==2?5*t:mode==3?(t>.5?5:0):5*4*t*(1-t);
            int yy=Math.round(cy+side*(6+bend));
            for(int y=yy-1;y<=yy+1;y++) {gray[y*w+x]=0;if(labels[y*w+x]!=4)labels[y*w+x]=5;}
        }
        var m=OmrScoreInterpreter.class.getDeclaredMethod("hasContinuousTieArc",byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);
        m.setAccessible(true);return (boolean)m.invoke(null,labels,gray,w,h,left,right,cy,gap);
    }
    @Test public void bothTieEndsMayTouchRuleAbove()throws Exception {assertTrue(detect(1,false));}
    @Test public void bothTieEndsMayTouchRuleBelow()throws Exception {assertTrue(detect(1,true));}
    @Test public void straightRuleIsNotTie()throws Exception {assertFalse(detect(0,false));}
    @Test public void slopingBeamIsNotTie()throws Exception {assertFalse(detect(2,false));}
    @Test public void steppedBeamIsNotTie()throws Exception {assertFalse(detect(3,false));}
    @Test public void halfCurveIsNotTie()throws Exception {assertFalse(detect(4,false));}
}
