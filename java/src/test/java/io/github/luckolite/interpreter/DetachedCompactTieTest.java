// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic dotted-note clearance, not a crop of a published score. */
public class DetachedCompactTieTest {
    private boolean detect(int mode, int side) throws Exception {
        int w=mode==5?600:220,h=160;float gap=14.5f,cy=80;
        byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)(mode>=6?160:255));
        for(int x=0;x<w;x++){int y=Math.round(cy+side*gap);gray[y*w+x]=0;labels[y*w+x]=4;}
        int end=mode==5?407:107;
        for(int x=77;x<=end;x++) {
            float t=(x-77)/(float)(end-77);
            if(mode==3&&t>.5f)continue;
            float curve=mode==1?0:mode==2?7*t:7*4*t*(1-t);
            int y=Math.round(cy+side*(7+curve));
            gray[y*w+x]=(byte)(mode==4?210:mode==6?145:mode==7?135:40);
            if(labels[y*w+x]!=4)labels[y*w+x]=5;
        }
        var m=OmrScoreInterpreter.class.getDeclaredMethod("hasPrintedTieArc",byte[].class,
                byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);
        m.setAccessible(true);return (boolean)m.invoke(null,labels,gray,w,h,60,end+7,cy,gap);
    }
    @Test public void upperTieClearsDot()throws Exception{assertTrue(detect(0,-1));}
    @Test public void lowerTieClearsDot()throws Exception{assertTrue(detect(0,1));}
    @Test public void straightStrokeIsNotTie()throws Exception{assertFalse(detect(1,-1));}
    @Test public void slopingStrokeIsNotTie()throws Exception{assertFalse(detect(2,-1));}
    @Test public void halfCurveIsNotTie()throws Exception{assertFalse(detect(3,-1));}
    @Test public void paleNoiseIsNotTie()throws Exception{assertFalse(detect(4,-1));}
    @Test public void broadStaffCoveredCurveNeedsIndependentEvidence()throws Exception{assertFalse(detect(5,-1));}
    @Test public void shadedPaperRippleIsNotPrintedTie()throws Exception{assertFalse(detect(6,-1));}
    @Test public void contrastedPaleTieOnShadeIsPreserved()throws Exception{assertTrue(detect(7,-1));}
}
