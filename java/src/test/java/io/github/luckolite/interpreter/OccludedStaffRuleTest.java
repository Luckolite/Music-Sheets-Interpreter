// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class OccludedStaffRuleTest {
    private final int width=400,height=260;
    private final byte[] gray=new byte[width*height],labels=new byte[width*height];
    private void page(int missing,boolean beam,boolean semantic) {
        Arrays.fill(gray,(byte)170);Arrays.fill(labels,(byte)0);
        for(int x=35;x<=337;x++)for(int line=0;line<5;line++) {
            if(line==missing&&x<280)continue;
            int row=Math.round(204+Math.max(0,x-240)*.065f-line*16.5f);
            for(int y=row;y<=row+1;y++){gray[y*width+x]=60;if(semantic)labels[y*width+x]=4;}
        }
        if(beam)for(int x=247;x<=288;x++) {
            int row=Math.round(204+Math.max(0,x-240)*.065f-4*16.5f);
            for(int y=row-12;y<=row-4;y++)gray[y*width+x]=10;
        }
    }
    private float[] local()throws Exception {
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructors()[0];sc.setAccessible(true);
        Object staff=sc.newInstance(138f,204f,16.5f);
        var ht=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var hc=ht.getDeclaredConstructors()[0];hc.setAccessible(true);
        Object head=hc.newInstance(380,289,315,183,201,302f,192.2f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,st,ht);m.setAccessible(true);
        return (float[])m.invoke(null,labels,gray,width,height,staff,head);
    }
    @Test public void beamBesideFinalHeadRetainsLinePitch()throws Exception {
        page(-1,true,true);float[] p=local();assertEquals(2,Math.round((p[0]-192.2f)/(p[1]*.5f)));
    }
    @Test public void uncoveredRulesRetainLinePitch()throws Exception {
        page(-1,false,true);float[] p=local();assertEquals(2,Math.round((p[0]-192.2f)/(p[1]*.5f)));
    }
    @Test public void missingOuterRuleIsNotTreatedAsBeamOcclusion() {
        page(4,false,true);assertNull(StaffPitchTrack.localOccludedRules(labels,gray,width,height,302,289,315,204,16.5f));
    }
    @Test public void rawBeamPatternWithoutStaffLabelsIsRejected() {
        page(-1,true,false);assertNull(StaffPitchTrack.localOccludedRules(labels,gray,width,height,302,289,315,204,16.5f));
    }
    @Test public void completeGroupOneStaffGapAwayCannotChangePhase() {
        page(-1,true,true);assertNull(StaffPitchTrack.localOccludedRules(labels,gray,width,height,302,289,315,188,16.5f));
    }
    @Test public void twoObstructedRulesAreInsufficient() {
        page(-1,true,true);
        for(int x=247;x<=288;x++)for(int y=157;y<=167;y++)gray[y*width+x]=10;
        assertNull(StaffPitchTrack.localOccludedRules(labels,gray,width,height,302,289,315,204,16.5f));
    }
}
