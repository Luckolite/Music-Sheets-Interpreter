// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff geometry, with no private score images or model output. */
public class WhiteStaffEdgeTest {
    private static final int W=420,H=280;
    private static byte[][] rules(float slope,int count) {
        byte[] labels=new byte[W*H],gray=new byte[W*H];Arrays.fill(gray,(byte)245);
        for(int line=0;line<count;line++)for(int x=20;x<W-20;x++) {
            int y=144-line*16+Math.round((x-210)*slope);
            gray[y*W+x]=60;labels[y*W+x]=4;
        }
        return new byte[][]{labels,gray};
    }
    private static float[] pitch(byte[][] ink,float seed) throws Exception {
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=st.getDeclaredConstructors()[0];sc.setAccessible(true);
        Object staff=sc.newInstance(seed-64,seed,16f);
        var ht=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var hc=ht.getDeclaredConstructors()[0];hc.setAccessible(true);
        Object head=hc.newInstance(100,203,217,115,125,210f,120f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,st,ht);
        method.setAccessible(true);
        return (float[])method.invoke(null,ink[0],ink[1],W,H,staff,head);
    }
    private static void correct(byte[][] ink,float seed) throws Exception {
        float[] result=pitch(ink,seed);
        assertEquals(144,result[0],1f);assertEquals(16,result[1],.5f);
        assertEquals(3,Math.round((result[0]-120)*2/result[1]));
    }
    @Test public void completeWhiteRulesResolveOneLinePhaseError() throws Exception {correct(rules(0,5),130);}
    @Test public void downhillWhiteRulesNeedNoWholePageWarp() throws Exception {correct(rules(.12f,5),136);}
    @Test public void uphillWhiteRulesNeedNoWholePageWarp() throws Exception {correct(rules(-.12f,5),136);}
    @Test public void unlabelledBeamCannotReplaceTheOuterStaffRule() throws Exception {
        byte[][] ink=rules(0,5);
        for(int x=130;x<290;x++){ink[1][160*W+x]=0;ink[0][160*W+x]=5;}
        correct(ink,152);
    }
    @Test public void horizontalWhiteRulesKeepTheSamePitch() throws Exception {correct(rules(0,5),144);}

    private static float[] printed(byte[][] ink) {
        return StaffPitchTrack.localPrintedRules(ink[0],ink[1],W,H,210,203,217,136,16);
    }
    @Test public void fourRulesCannotProveAReplacementStaff() {assertNull(printed(rules(.12f,4)));}
    @Test public void fiveUnlabelledBeamEdgesAreNotAStaff() {
        byte[][] ink=rules(.12f,5);Arrays.fill(ink[0],(byte)5);
        assertNull(printed(ink));
    }
    @Test public void rulesOnOnlyOneSideOfTheNoteAreInsufficient() {
        byte[][] ink=rules(.12f,5);
        for(int x=218;x<W;x++)for(int y=0;y<H;y++){ink[0][y*W+x]=0;ink[1][y*W+x]=(byte)245;}
        assertNull(printed(ink));
    }
    @Test public void anOuterRuleMissingOnOneSideCannotShiftTheStaff() {
        byte[][] ink=rules(.12f,5);
        for(int x=218;x<W-20;x++) {
            int y=80+Math.round((x-210)*.12f);ink[0][y*W+x]=0;ink[1][y*W+x]=(byte)245;
        }
        assertNull(printed(ink));
    }
    @Test public void shortLedgerSegmentsCannotFormAReplacementStaff() {
        byte[][] ink=rules(0,5);
        for(int x=0;x<W;x++)if(x<180||x>240)
            for(int y=0;y<H;y++){ink[0][y*W+x]=0;ink[1][y*W+x]=(byte)245;}
        assertNull(printed(ink));
    }
    @Test public void aDifferentStaffSpacingIsNotAccepted() {
        byte[][] ink=rules(0,0);
        for(int x=20;x<W-20;x++)for(int line=0;line<5;line++) {
            int y=160-line*20;ink[0][y*W+x]=4;ink[1][y*W+x]=50;
        }
        assertNull(printed(ink));
    }
    @Test public void crossingBeamsDoNotTiltACompleteFlatStaff() throws Exception {
        byte[][] ink=rules(0,5);
        for(int line=0;line<5;line++)for(int x=130;x<290;x++) {
            int y=144-line*16+Math.round((x-210)*.12f);
            if(ink[0][y*W+x]!=4){ink[0][y*W+x]=5;ink[1][y*W+x]=0;}
        }
        correct(ink,144);
    }
}
