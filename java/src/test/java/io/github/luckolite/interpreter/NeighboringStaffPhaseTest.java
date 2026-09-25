// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloped five-rule ink with a crossing pen stroke. */
public class NeighboringStaffPhaseTest {
    static final int W=640,H=260,G=16;
    byte[] labels=new byte[W*H],gray=new byte[W*H];
    void line(float base,float slope,int label){for(int x=30;x<W-30;x++){int y=Math.round(base+(x-320)*slope);for(int dy=-1;dy<=1;dy++){labels[(y+dy)*W+x]=(byte)label;gray[(y+dy)*W+x]=0;}}}
    NeighboringStaffPhaseTest page(){Arrays.fill(gray,(byte)255);for(int i=0;i<5;i++)line(164-i*G,.025f,4);return this;}
    float[] resolve(){return NeighboringStaffPhase.resolve(labels,gray,W,H,320,176,G);}
    @Test public void independentWindowsRestoreTheFiveRulePhase(){page();var v=resolve();assertNotNull(v);assertEquals(164,v[0],1.3f);assertEquals(16,v[1],.6f);}
    @Test public void diagonalAnnotationDoesNotMoveTheStaff(){page();for(int x=220;x<380;x++){int cy=Math.round(147+(x-220)*.28f);for(int d=-2;d<=2;d++)gray[(cy+d)*W+x]=0;}var v=resolve();assertNotNull(v);assertEquals(164,v[0],1.3f);}
    @Test public void sixthParallelRuleIsAmbiguous(){page();line(180,.025f,4);assertNull(resolve());}
    @Test public void incompleteStaffCannotEstablishAPhase(){page();for(int y=90;y<=104;y++)for(int x=0;x<W;x++){gray[y*W+x]=(byte)255;labels[y*W+x]=0;}assertNull(resolve());}
    @Test public void narrowLedgerGroupCannotEstablishAPhase(){page();for(int x=0;x<W;x++)if(x<300||x>350)for(int y=0;y<H;y++){gray[y*W+x]=(byte)255;labels[y*W+x]=0;}assertNull(resolve());}
    @Test public void nearAccurateSeedIsNotRecalibrated(){page();assertNull(NeighboringStaffPhase.resolve(labels,gray,W,H,320,167,G));}
    @Test public void inputPixelsArePreserved(){page();var l=labels.clone();var g=gray.clone();resolve();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
