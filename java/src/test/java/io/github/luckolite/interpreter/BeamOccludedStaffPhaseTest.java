// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Original geometric raster controls; no score-derived pixels. */
public final class BeamOccludedStaffPhaseTest {
    private static final int W=240,H=200,LEFT=110,RIGHT=130;
    private static final float X=120,GAP=14,BOTTOM=150;
    private record Page(byte[] labels,byte[] gray) {}
    private static Page page(int covered,float slope,int semanticShift) {
        byte[] l=new byte[W*H],g=new byte[W*H];Arrays.fill(g,(byte)240);
        for(int line=0;line<5;line++)for(int x=40;x<200;x++) {
            int y=Math.round(BOTTOM-line*GAP+slope*(x-X));
            if(line==covered)for(int dy=-4;dy<=4;dy++)g[(y+dy)*W+x]=20;
            else {g[y*W+x]=70;l[(y+semanticShift)*W+x]=4;}
        }
        return new Page(l,g);
    }
    private static float[] resolve(Page p,float reference) {
        return BeamOccludedStaffPhase.resolve(p.labels,p.gray,W,H,X,LEFT,RIGHT,reference,GAP);
    }
    private static void exact(Page p) {
        float[] found=resolve(p,BOTTOM+6);assertNotNull(found);
        assertEquals(BOTTOM,found[0],1.0f);assertEquals(GAP,found[1],.35f);
        assertEquals(7,Math.round((found[0]-(BOTTOM-3.5f*GAP))/(found[1]*.5f)));
    }
    @Test public void bottomRuleHiddenByBeam(){exact(page(0,0,0));}
    @Test public void topRuleHiddenByBeam(){exact(page(4,0,0));}
    @Test public void slantedThinRulesRemainConsistent(){exact(page(0,-.08f,0));}
    @Test public void semanticStripeMayBeSlightlyOffset(){exact(page(0,-.04f,3));}
    @Test public void completeFiveRulesAreNotThisFallback(){assertNull(resolve(page(-1,0,0),BOTTOM+6));}
    @Test public void interiorMissingRuleDoesNotProveOuterPhase(){assertNull(resolve(page(2,0,0),BOTTOM+6));}
    @Test public void noCorrectionWhenReferenceAlreadyAgrees(){assertNull(resolve(page(0,0,0),BOTTOM));}
    @Test public void absentSemanticSupportCannotRephase(){Page p=page(0,0,0);Arrays.fill(p.labels,(byte)0);assertNull(resolve(p,BOTTOM+6));}
    @Test public void onlyThreeThinRulesCannotRephase(){Page p=page(0,0,0);for(int x=40;x<200;x++){p.labels[(int)(BOTTOM-GAP)*W+x]=0;p.gray[(int)(BOTTOM-GAP)*W+x]=(byte)240;}assertNull(resolve(p,BOTTOM+6));}
    @Test public void noThickBeamCannotInferMissingRule(){Page p=page(0,0,0);for(int y=146;y<=154;y++)Arrays.fill(p.gray,y*W,(y+1)*W,(byte)240);assertNull(resolve(p,BOTTOM+6));}
    @Test public void oneSidedBeamCannotRephase(){Page p=page(0,0,0);for(int y=146;y<=154;y++)Arrays.fill(p.gray,y*W+RIGHT+1,(y+1)*W,(byte)240);assertNull(resolve(p,BOTTOM+6));}
    @Test public void inputArraysAreNeverModified(){Page p=page(0,0,0);byte[] l=p.labels.clone(),g=p.gray.clone();exact(p);assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
    @Test public void invalidInputRejected(){assertNull(BeamOccludedStaffPhase.resolve(new byte[3],new byte[3],W,H,X,LEFT,RIGHT,BOTTOM,GAP));}
    @Test public void sixthParallelRuleMakesOuterPhaseAmbiguous(){Page p=page(0,0,0);for(int x=40;x<200;x++){p.gray[164*W+x]=70;p.labels[164*W+x]=4;}float[] f=resolve(p,BOTTOM+6);assertTrue(Arrays.toString(f),f==null);}
    @Test public void referenceCanBeAboveTheActualStaff(){float[] found=resolve(page(0,.04f,0),BOTTOM-6);assertNotNull(found);assertEquals(BOTTOM,found[0],1);}
    @Test public void widelyShiftedSemanticStripeCannotSupportThinRules(){assertNull(resolve(page(0,0,6),BOTTOM+6));}
    @Test public void oneSidedPrintedRulesCannotEstablishStaff(){Page p=page(0,0,0);for(int y=80;y<145;y++)Arrays.fill(p.labels,y*W+RIGHT+1,(y+1)*W,(byte)0);assertNull(resolve(p,BOTTOM+6));}
    @Test public void nonFiniteGeometryRejected(){Page p=page(0,0,0);assertNull(BeamOccludedStaffPhase.resolve(p.labels,p.gray,W,H,Float.NaN,LEFT,RIGHT,BOTTOM,GAP));assertNull(resolve(p,Float.NaN));}
}
