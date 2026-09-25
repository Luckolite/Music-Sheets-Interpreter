// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric two-grace exercise with an erased scan stripe; no score pixels. */
public class CreaseGracePairRecoveryTest {
    private static final int W=260,H=220;private static final float GAP=16;
    private final byte[] g=new byte[W*H];
    private final CreaseGracePairRecovery.Box first=new CreaseGracePairRecovery.Box(74,126,86,138,80,132);
    private final CreaseGracePairRecovery.Box principal=new CreaseGracePairRecovery.Box(133,124,155,140,144,132);
    private void rect(int l,int r,int t,int b,int ink){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=(byte)ink;}
    private void oval(float cx,float cy,float rx,float ry){for(int y=Math.round(cy-ry-3);y<=cy+ry+3;y++)for(int x=Math.round(cx-rx);x<=cx+rx;x++){float xx=(x-cx)/rx,yy=(y-cy+(x-cx)*.3f)/ry;if(xx*xx+yy*yy<=1)g[y*W+x]=20;}}
    private void page(boolean crease,int beams){Arrays.fill(g,(byte)255);for(int y=100;y<=164;y+=16)rect(20,240,y,y+1,125);
        rect(85,86,96,132,180);rect(113,114,94,124,180);oval(80,132,6,5);oval(108,124,6,5);oval(144,132,11,7);
        rect(86,99,96,99,20);rect(103,114,94,97,20);if(beams==2){rect(86,99,103,106,20);rect(103,114,101,104,20);}
        if(crease)rect(100,102,80,180,255);
    }
    private CreaseGracePairRecovery.Pair find(){return CreaseGracePairRecovery.find(g,W,H,GAP,100,164,first,principal);}
    @Test public void paleStemsAndSplitDoubleBeamRecoverSecondGrace(){page(true,2);var pair=find();assertNotNull(pair);assertEquals(2,pair.beams());assertEquals(108,pair.recovered().x(),2);assertEquals(124,pair.recovered().y(),2);}
    @Test public void noWhiteCreaseDoesNotTriggerThisRecovery(){page(false,2);assertNull(find());}
    @Test public void singleBeamIsNotSufficientForThisBoundedCase(){page(true,1);assertNull(find());}
    @Test public void absentSecondOvalCannotInventHead(){page(true,2);rect(103,116,115,133,255);assertNull(find());}
    @Test public void detachedUpperBarsDoNotProveStems(){page(true,2);rect(84,87,110,122,255);assertNull(find());}
    @Test public void absentPrincipalHeadCannotCreateGraceGroup(){page(true,2);rect(133,155,124,140,255);assertNull(find());}
    @Test public void absentFirstOvalCannotCreateGraceGroup(){page(true,2);rect(74,86,126,138,255);assertNull(find());}
    @Test public void broadErasedGapCannotBeBridged(){page(true,2);rect(96,108,80,180,255);assertNull(find());}
    @Test public void greyPaperBandIsNotWhiteErasure(){page(true,2);rect(100,102,80,180,225);assertNull(find());}
    @Test public void fullSizeFirstHeadIsNotPromotedToGrace(){page(true,2);assertNull(CreaseGracePairRecovery.find(g,W,H,GAP,100,164,new CreaseGracePairRecovery.Box(69,124,91,140,80,132),principal));}
    @Test public void sourcePixelsAreUnchanged(){page(true,2);byte[] before=g.clone();assertNotNull(find());assertArrayEquals(before,g);}
    @Test public void invalidInputIsRejected(){
        page(true,2);
        assertNull(CreaseGracePairRecovery.find(new byte[2],W,H,GAP,100,164,first,principal));
        assertNull(CreaseGracePairRecovery.find(g,W,H,GAP,100,164,null,principal));
        for(float[] bounds:new float[][]{{Float.NaN,164},{100,Float.NaN},{Float.NEGATIVE_INFINITY,164},
                {100,Float.POSITIVE_INFINITY},{-1,164},{100,H},{164,100},{100,100}})
            assertNull(CreaseGracePairRecovery.find(g,W,H,GAP,bounds[0],bounds[1],first,principal));
    }
    @Test public void semanticCapIsReplacedByActualGraceInDecoder(){
        page(true,2);byte[] labels=new byte[W*H];
        for(int y=100;y<=164;y+=16)for(int x=20;x<=240;x++)for(int dy=0;dy<=1;dy++)labels[(y+dy)*W+x]=4;
        for(int y=96;y<=132;y++)for(int x=85;x<=86;x++)labels[y*W+x]=1;
        for(int y=94;y<=124;y++)for(int x=113;x<=114;x++)labels[y*W+x]=1;
        for(int y=124;y<=140;y++)for(int x=70;x<=155;x++)if(g[y*W+x]==20)labels[y*W+x]=(byte)(x<92||x>130?2:5);
        for(int y=96;y<=106;y++)for(int x=86;x<=96;x++)labels[y*W+x]=2;
        for(int y=80;y<=180;y++)for(int x=100;x<=102;x++)labels[y*W+x]=0;
        var notes=OmrScoreInterpreter.extract(labels,g,W,H,List.of(new MeasureRegion(0,1,.35f,.8f)));
        assertEquals(3,notes.size());
        assertEquals(4,notes.get(0).staffStep());assertEquals(5,notes.get(1).staffStep());assertEquals(4,notes.get(2).staffStep());
        for(int i=0;i<2;i++){assertEquals(2,notes.get(i).beamCount());assertTrue((notes.get(i).articulations()&NoteOrnament.GRACE)!=0);}
        assertEquals(0,notes.get(2).articulations()&NoteOrnament.GRACE);
    }
}
