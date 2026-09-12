// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original straight/curved rule patterns with independently drawn beam ink. */
public class AmbiguousStaffTrackTest {
    private static final int W=1200,H=300;
    private static void ambiguousOuterRule(boolean above) throws Exception {
        int w=120,h=120;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int row:new int[]{40,50,60,70,80,above?30:90})for(int x=0;x<w;x++)gray[row*w+x]=40;
        var lines=new RawStaffLineDetector.StaffLines(new int[]{40,50,60,70,80},10f);
        java.lang.reflect.Method method;
        try {
            method=StaffPitchTrack.class.getDeclaredMethod("completeRules",byte[].class,int.class,int.class,RawStaffLineDetector.StaffLines.class,boolean.class);
        } catch(NoSuchMethodException oldImplementation) {
            method=StaffPitchTrack.class.getDeclaredMethod("completeRules",byte[].class,int.class,int.class,RawStaffLineDetector.StaffLines.class);
        }
        method.setAccessible(true);
        boolean ambiguous=(boolean)(method.getParameterCount()==5?method.invoke(null,gray,w,h,lines,true):method.invoke(null,gray,w,h,lines));
        assertFalse("A sixth equally spaced printed rule leaves the five-rule phase ambiguous",ambiguous);
        if(method.getParameterCount()==5)assertTrue("A group remaining in the known staff phase keeps its prior support",(boolean)method.invoke(null,gray,w,h,lines,false));
    }
    @Test public void extraLowerRuleInvalidatesAPhaseAmbiguousSample() throws Exception {ambiguousOuterRule(false);}
    @Test public void extraUpperRuleInvalidatesAPhaseAmbiguousSample() throws Exception {ambiguousOuterRule(true);}
    private static byte[] tiltedWithBeams(boolean above) {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int x=30;x<W-30;x++) {
            int tilt=Math.round(x*.0065f);
            for(int line=0;line<5;line++)for(int y=100+line*10+tilt;y<=101+line*10+tilt;y++)gray[y*W+x]=60;
            if(x>650)for(int y=(above?90:150)+tilt;y<=(above?92:152)+tilt;y++)gray[y*W+x]=0;
            if(x>900)for(int y=(above?80:160)+tilt;y<=(above?82:162)+tilt;y++)gray[y*W+x]=0;
        }
        return gray;
    }
    private static void phasePreserved(boolean above) {
        var track=StaffPitchTrack.detect(tiltedWithBeams(above),W,H,104,144,10);
        if(track!=null)for(int x:new int[]{250,650,1050})assertEquals("Beam must not replace an outer staff rule",140.5f+x*.0065f,track.at(x)[0],2f);
    }
    @Test public void lowerBeamExtensionsCannotShiftTheFiveRulePhase() {phasePreserved(false);}
    @Test public void upperBeamExtensionsCannotShiftTheFiveRulePhase() {phasePreserved(true);}
    @Test public void anIsolatedFiveRuleCurveStillTracks() {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int x=30;x<W-30;x++)for(int line=0;line<5;line++) {
            int row=Math.round(190-35*x*x/(float)(W*W))-line*10;
            gray[row*W+x]=50;gray[(row+1)*W+x]=50;
        }
        var track=StaffPitchTrack.detect(gray,W,H,140,180,10);assertNotNull(track);
        assertEquals(190.5f-35*1000*1000/(float)(W*W),track.at(1000)[0],2f);
    }
}
