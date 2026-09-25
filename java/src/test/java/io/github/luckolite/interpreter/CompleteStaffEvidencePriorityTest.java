// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ruled-line fixture: a broad faint staff and narrow darker interference. */
public class CompleteStaffEvidencePriorityTest {
    private static final int W=400,H=280;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    public CompleteStaffEvidencePriorityTest(){Arrays.fill(gray,(byte)250);}
    private void fixture(boolean paleRules) {
        for(int line=0;line<5;line++)for(int x=100;x<=260;x++) {
            if(paleRules)gray[(80+line*14)*W+x]=(byte)215;
            for(int y=77+line*14;y<=83+line*14;y++)labels[y*W+x]=4;
        }
        for(int line=0;line<5;line++)for(int x=148;x<=212;x++)if(x<=159||x>=201) {
            int y=Math.round(77+line*15.5f);gray[y*W+x]=(byte)100;
        }
    }
    private float[] local()throws Exception {
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructors()[0];sc.setAccessible(true);
        var ct=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var cc=ct.getDeclaredConstructors()[0];cc.setAccessible(true);
        var staff=sc.newInstance(80f,136f,14f);var head=cc.newInstance(200,170,190,163,177,180f,170f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,st,ct);method.setAccessible(true);
        return (float[])method.invoke(null,labels,gray,W,H,staff,head);
    }
    @Test public void completeFaintRulesOutrankNarrowOccludedEstimate()throws Exception {
        fixture(true);
        assertNull(StaffPitchTrack.localPrintedRules(labels,gray,W,H,180,170,190,136,14));
        assertArrayEquals(new float[]{139,15.5f},StaffPitchTrack.localOccludedRules(labels,gray,W,H,180,170,190,136,14),.001f);
        assertArrayEquals(new float[]{136,14},StaffPitchTrack.localFadedRules(labels,gray,W,H,180,170,190,136,14),.001f);
        assertArrayEquals(new float[]{136,14},local(),.001f);
        assertEquals(-5,Math.round((local()[0]-170)*2/local()[1]));
    }
    @Test public void partialEstimateRemainsAvailableWithoutCompleteFaintRules()throws Exception {
        fixture(false);assertNull(StaffPitchTrack.localFadedRules(labels,gray,W,H,180,170,190,136,14));
        assertArrayEquals(new float[]{139,15.5f},local(),.001f);
    }
    @Test public void blankInkDoesNotInventAStaffCorrection()throws Exception {
        assertArrayEquals(new float[]{136,14},local(),.001f);
    }
    @Test public void sourceLabelsAndInkAreUnchanged()throws Exception {
        fixture(true);byte[] beforeGray=gray.clone(),beforeLabels=labels.clone();local();
        assertArrayEquals(beforeGray,gray);assertArrayEquals(beforeLabels,labels);
    }
}
