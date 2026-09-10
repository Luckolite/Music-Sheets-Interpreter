// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated rules, independent of score scans and song identities. */
public class MildStaffTiltTest {
    private static final int W=2000,H=400;
    private static float bottom(int x,float slope) {return 240+slope*(x-W*.5f);}
    private static byte[] rules(float slope) {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)240);
        for(int x=40;x<W-40;x++)for(int line=0;line<5;line++) {
            int y=Math.round(bottom(x,slope)-line*14);
            g[y*W+x]=50;g[(y+1)*W+x]=50;
        }
        return g;
    }
    private static void follows(float slope,float seed) {
        var track=StaffPitchTrack.detect(rules(slope),W,H,seed-56,seed,14);
        assertNotNull("A mild tilt still moves a note into another staff space",track);
        for(int x:new int[]{200,700,1200,1800}) {
            assertEquals(bottom(x,slope)+.5f,track.at(x)[0],1f);
            assertEquals(14,track.at(x)[1],.4f);
        }
    }
    @Test public void followsMildDownhillRulesFromAnOffsetSeed() {follows(.0065f,236);}
    @Test public void followsMildUphillRulesFromAnOffsetSeed() {follows(-.0065f,244);}
    @Test public void flatRulesKeepTheOriginalReference() {
        assertNull(StaffPitchTrack.detect(rules(0),W,H,184.5f,240.5f,14));
    }
    @Test public void fourSeparatedRuleGroupsCannotEstablishAMildTilt() {
        byte[] g=rules(.0065f);
        for(int x=0;x<W;x++) {
            boolean keep=false;
            for(int strip:new int[]{0,2,4,6})if(Math.abs(x-Math.round(W*(.15f+strip*.12f)))<72)keep=true;
            if(!keep)for(int y=0;y<H;y++)g[y*W+x]=(byte)240;
        }
        assertNull(StaffPitchTrack.detect(g,W,H,180,236,14));
    }
    @Test public void alternatingRuleOffsetsAreNotASmoothMildTilt() {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)240);
        for(int strip=0;strip<7;strip++) {
            int center=Math.round(W*(.15f+strip*.12f));
            for(int x=center-72;x<=center+72;x++)for(int line=0;line<5;line++) {
                int y=236+(strip%2)*8-line*14;g[y*W+x]=50;g[(y+1)*W+x]=50;
            }
        }
        assertNull(StaffPitchTrack.detect(g,W,H,180,236,14));
    }
    @Test public void edgeSpacePitchUsesTheMildTrack() throws Exception {
        byte[] g=rules(.0065f),labels=new byte[W*H];
        for(int i=0;i<g.length;i++)if((g[i]&255)<170)labels[i]=4;
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object staff=constructor.newInstance(180f,236f,14f);
        var field=type.getDeclaredField("pitchTrack");field.setAccessible(true);
        field.set(staff,StaffPitchTrack.detect(g,W,H,180,236,14));
        var headType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var hc=headType.getDeclaredConstructors()[0];hc.setAccessible(true);
        float cy=bottom(1800,.0065f)+.5f-21;
        Object head=hc.newInstance(100,1793,1807,Math.round(cy)-5,Math.round(cy)+5,1800f,cy);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,type,headType);
        method.setAccessible(true);
        float[] pitch=(float[])method.invoke(null,labels,g,W,H,staff,head);
        assertEquals(3,Math.round((pitch[0]-cy)*2/pitch[1]));
    }
    @Test public void mildCurvatureCanHaveASmallInitialReverseSlope() {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)240);
        int[] offsets={0,-1,1,3,6,8,9};
        for(int x=40;x<W-40;x++) {
            float position=(x-W*.15f)/(W*.12f);
            int i=Math.max(0,Math.min(5,(int)Math.floor(position)));
            float base=240+offsets[i]+(position-i)*(offsets[i+1]-offsets[i]);
            for(int line=0;line<5;line++) {
                int y=Math.round(base-line*14);g[y*W+x]=50;g[(y+1)*W+x]=50;
            }
        }
        var track=StaffPitchTrack.detect(g,W,H,184,240,14);
        assertNotNull(track);
        assertEquals(249.5f,track.at(1740)[0],1f);
    }
}
