// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated staff geometry; no score scans or model predictions. */
public class FaintStraightStaffTest {
    private static final int WIDTH=1200, HEIGHT=300;
    private static byte[][] staff(float gap,float bottom,int ink) {
        byte[] labels=new byte[WIDTH*HEIGHT],gray=new byte[WIDTH*HEIGHT];
        Arrays.fill(gray,(byte)245);
        for(int x=25;x<WIDTH-25;x++)for(int line=0;line<5;line++) {
            int y=Math.round(bottom-line*gap);
            gray[y*WIDTH+x]=(byte)ink;labels[y*WIDTH+x]=4;
        }
        return new byte[][]{labels,gray};
    }
    @Test public void fadedRulesCorrectAnExpandedSemanticScale() {
        var image=staff(10,140,186);
        var pitch=StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,142,10.7f);
        assertNotNull(pitch);assertEquals(140,pitch[0],.01);assertEquals(10,pitch[1],.01);
        assertEquals(12,Math.round((pitch[0]-80)*2/pitch[1]));
    }
    @Test public void correctionScalesWithImageResolution() {
        var image=staff(18,220,195);
        var pitch=StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,223,19.4f);
        assertNotNull(pitch);assertEquals(220,pitch[0],.01);assertEquals(18,pitch[1],.01);
    }
    @Test public void aLocallyObscuredGroupDoesNotLoseTheBroadReference() {
        var image=staff(10,140,186);
        for(int y=90;y<150;y++)for(int x=920;x<1010;x++)image[1][y*WIDTH+x]=(byte)245;
        var pitch=StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,142,10.7f);
        assertNotNull(pitch);assertEquals(140,pitch[0],.01);assertEquals(10,pitch[1],.01);
    }
    @Test public void anAccurateSeedIsUnchanged() {
        var image=staff(10,140,186);
        assertNull(StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,140,10));
    }
    @Test public void unlabelledParallelInkIsNotAStaffCalibration() {
        var image=staff(10,140,186);Arrays.fill(image[0],(byte)0);
        assertNull(StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,142,10.7f));
    }
    @Test public void fourRulesAndAnUnlabelledBeamAreInsufficient() {
        var image=staff(10,140,186);
        for(int x=0;x<WIDTH;x++) {
            image[1][100*WIDTH+x]=(byte)245;image[0][100*WIDTH+x]=0;
            image[1][150*WIDTH+x]=20;
        }
        assertNull(StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,142,10.7f));
    }
    @Test public void aCurveCannotBecomeAConstantReference() {
        var image=staff(10,140,186);Arrays.fill(image[0],(byte)0);Arrays.fill(image[1],(byte)245);
        for(int x=25;x<WIDTH-25;x++)for(int line=0;line<5;line++) {
            int y=Math.round(140-line*10+14*x*x/(float)(WIDTH*WIDTH));
            image[1][y*WIDTH+x]=(byte)186;image[0][y*WIDTH+x]=4;
        }
        assertNull(StaffPitchTrack.straightPitch(image[0],image[1],WIDTH,HEIGHT,142,10.7f));
    }
    @Test public void aLedgerNoteUsesPrintedScaleWhenItsLocalStaffIsObscured() throws Exception {
        var image=staff(10,140,186);Arrays.fill(image[0],(byte)0);
        for(int x=25;x<WIDTH-25;x++)for(int line=0;line<5;line++) {
            int center=Math.round(142-line*10.75f);
            for(int y=center-2;y<=center+2;y++)image[0][y*WIDTH+x]=4;
        }
        for(int y=90;y<150;y++)for(int x=920;x<1010;x++)image[1][y*WIDTH+x]=(byte)245;
        var find=OmrScoreInterpreter.class.getDeclaredMethod("findStaffs",byte[].class,byte[].class,
                int.class,int.class,java.util.List.class);find.setAccessible(true);
        var staffs=(java.util.List<?>)find.invoke(null,image[0],image[1],WIDTH,HEIGHT,
                java.util.List.of(new MeasureRegion(.02f,.98f,.2f,.6f)));
        assertEquals(1,staffs.size());Object staff=staffs.get(0);
        var headType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=headType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object head=constructor.newInstance(80,959,971,77,83,965f,80f);
        var local=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,
                int.class,int.class,staff.getClass(),headType);local.setAccessible(true);
        float[] pitch=(float[])local.invoke(null,image[0],image[1],WIDTH,HEIGHT,staff,head);
        assertEquals(140,pitch[0],.01);assertEquals(10,pitch[1],.01);
        assertEquals(12,Math.round((pitch[0]-80)*2/pitch[1]));
    }
}
