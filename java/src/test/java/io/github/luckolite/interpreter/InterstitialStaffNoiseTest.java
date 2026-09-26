// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original close three-group projection with unsupported middle semantic paint. */
public class InterstitialStaffNoiseTest {
    static final int W=600,H=320,G=12;
    final byte[] gray=new byte[W*H];final int[] strength=new int[H];
    public InterstitialStaffNoiseTest(){Arrays.fill(gray,(byte)255);staff(50,0);staff(110,-1);staff(170,0);}
    void staff(int top,int ink){for(int j=0;j<5;j++){
        int y=top+j*G;strength[y]=450;
        if(ink>=0)for(int x=35;x<565;x++)gray[y*W+x]=(byte)ink;
    }}
    List<RawStaffLineDetector.StaffLines> detect(){return RawStaffLineDetector.detectFromStrength(strength,30,H,gray,W);}
    @Test public void unsupportedMiddleProjectionCannotSplitGrandStaff(){assertEquals(2,detect().size());assertEquals(50,detect().get(0).top());assertEquals(170,detect().get(1).top());}
    @Test public void realMiddleStaffIsRetained(){staff(110,0);assertEquals(3,detect().size());}
    @Test public void faintRealMiddleStaffIsRetained(){staff(110,220);assertEquals(3,detect().size());}
    @Test public void twoSurvivingRulesPreventDeletion(){for(int y:new int[]{110,122})for(int x=35;x<565;x++)gray[y*W+x]=0;assertEquals(3,detect().size());}
    @Test public void noRawPagePreservesSemanticEvidence(){assertEquals(3,RawStaffLineDetector.detectFromStrength(strength,30,H,null,W).size());}
    @Test public void unsupportedNeighborCannotAuthorizeDeletion(){for(int y=50;y<=98;y++)Arrays.fill(gray,y*W,(y+1)*W,(byte)255);assertEquals(3,detect().size());}
    @Test public void largeInterStaffSpaceIsOutsideThisRule(){
        Arrays.fill(strength,0);Arrays.fill(gray,(byte)255);staff(20,0);staff(110,-1);staff(200,0);assertEquals(3,detect().size());
    }
    @Test public void inputsAreUnchanged(){byte[] g=gray.clone();int[] s=strength.clone();detect();assertArrayEquals(g,gray);assertArrayEquals(s,strength);}
    @Test public void measureReaderKeepsBothRealStavesInOneSystem(){
        byte[] labels=new byte[W*H];
        for(int y=0;y<H;y++)if(strength[y]>0)for(int x=35;x<=565;x++)labels[y*W+x]=4;
        for(int x:new int[]{35,300,565})for(int y=50;y<=218;y++){gray[y*W+x]=0;labels[y*W+x]=1;}
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals(2,measures.size());
        for(var m:measures){assertTrue(m.top()<50f/H);assertTrue(m.bottom()>218f/H);}
    }
}
