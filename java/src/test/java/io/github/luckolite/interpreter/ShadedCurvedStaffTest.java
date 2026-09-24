// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic curved rules over a dark photographic paper background. */
public class ShadedCurvedStaffTest {
    private static float bottom(int x){return 210f-45f*x*x/(1200f*1200f);}
    private StaffPitchTrack track(boolean rules,boolean shortFragments) {
        int w=1200,h=350;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)155);
        if(rules)for(int x=30;x<w-30;x++)for(int line=0;line<5;line++) {
            if(shortFragments&&x%144>25)continue;
            int y=Math.round(bottom(x)-line*10);gray[y*w+x]=50;gray[(y+1)*w+x]=50;
        }
        return StaffPitchTrack.detect(gray,w,h,155,195,10);
    }
    @Test public void followsShadedCurvedPrintedRules() {
        var track=track(true,false);assertNotNull(track);
        for(int x:new int[]{190,440,720,970}) {
            assertEquals(bottom(x)+.5f,track.at(x)[0],2f);
            assertEquals(10,track.at(x)[1],.6f);
        }
    }
    @Test public void plainShadowCannotCreateStaff(){assertNull(track(false,false));}
    @Test public void sparseLedgerFragmentsCannotCreateStaff(){assertNull(track(true,true));}
    private float[] edge(int lines,boolean oneSide) {
        int w=400,h=300;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)160);
        for(int line=0;line<lines;line++)for(int x=30;x<370;x++) {
            if(oneSide&&x>180)continue;
            int y=180-line*15+Math.round((x-200)*.2f);gray[y*w+x]=50;gray[(y+1)*w+x]=50;
        }
        return StaffPitchTrack.localCurledEdgeRules(gray,w,h,200,190,210,150,15);
    }
    @Test public void curledEdgeRecoversRulesBeyondTruncatedModel(){
        var p=edge(5,false);assertNotNull(p);assertEquals(180.5,p[0],1);assertEquals(15,p[1],.5);
    }
    @Test public void fourRulesDoNotEstablishCurledEdge(){assertNull(edge(4,false));}
    @Test public void onlyOneSideIsNotEnough(){assertNull(edge(5,true));}
    @Test public void sixCompetingRulesAreAmbiguous(){assertNull(edge(6,false));}
}
