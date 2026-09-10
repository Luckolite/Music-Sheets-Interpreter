// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric H-rest, note ellipses and partial mask fragments. */
public class HeavyRestBarFragmentTest {
    static final int W=400,H=240;
    static final List<MeasureRegion> REGIONS=List.of(new MeasureRegion(.05f,.6f,.1f,.8f),new MeasureRegion(.6f,.95f,.1f,.8f));
    static class Page {
        byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(){Arrays.fill(gray,(byte)255);for(int y=80;y<=144;y+=16)rect(20,y,379,y,4);}
        void rect(int l,int t,int r,int b,int label){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}}
        void head(int x,int y,int rx,int ry){for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++)if(Math.pow((xx-x)/(double)rx,2)+Math.pow((yy-y)/(double)ry,2)<=1){gray[yy*W+xx]=0;labels[yy*W+xx]=2;}}
        void fragment(int x,int y){for(int yy=y-3;yy<=y+3;yy++)for(int xx=x-3;xx<=x+3;xx++)labels[yy*W+xx]=2;}
        byte[] normalize(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,W,H,REGIONS);}
        OmrScoreInterpreter.Analysis analyze(){return OmrScoreInterpreter.analyze(labels,gray,W,H,REGIONS);}
    }
    static Page page(boolean leftCap,boolean rightCap,int right,int thickness){
        Page p=new Page();p.rect(90,106,right,105+thickness,1);
        if(leftCap)p.rect(90,96,92,128,1);if(rightCap)p.rect(right-2,96,right,128,1);
        p.fragment(94,115);p.head(300,136,10,7);p.rect(310,88,310,136,1);return p;
    }
    @Test public void aBarFragmentIsNotASoundingNote(){var p=page(true,true,230,12);assertEquals(1,p.analyze().notes().size());}
    @Test public void normalizationAllowsThePrintedRestCount(){
        var p=page(true,true,230,12);var token=new MeasureNumberReconciler.NumberToken(7,.37f,.20f,.42f,.30f,.37f);
        assertEquals(1,MultiMeasureRestDetector.detect(p.normalize(),p.gray,W,H,REGIONS,List.of(token)).size());
    }
    @Test public void aStaffRuleThroughTheFragmentCannotHideTheRest(){var p=page(true,true,230,12);p.fragment(160,112);assertEquals(1,p.analyze().notes().size());}
    @Test public void aNarrowCapFragmentCanExtendBeyondTheThickBand(){var p=page(true,true,230,12);for(int y=106;y<=118;y++)for(int x=91;x<=97;x++)p.labels[y*W+x]=2;assertEquals(0,p.normalize()[115*W+94]);assertEquals(1,p.analyze().notes().size());}
    @Test public void rightEndFragmentsAreAlsoRemoved(){var p=page(true,true,230,12);p.fragment(227,115);assertEquals(1,p.analyze().notes().size());}
    @Test public void aOneSidedBeamIsInsufficient(){var p=page(true,false,230,12);assertArrayEquals(p.labels,p.normalize());}
    @Test public void theOppositeMissingCapIsAlsoInsufficient(){var p=page(false,true,230,12);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aShortCrossbarIsInsufficient(){var p=page(true,true,150,12);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aThinRuleIsInsufficient(){var p=page(true,true,230,1);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aGenuineSmallNoteRemains(){var p=page(true,true,230,12);p.head(180,144,6,5);p.rect(186,96,186,144,1);assertEquals(2,p.analyze().notes().size());}
    @Test public void absentRawEvidenceDoesNotModifyTheMask(){var p=page(true,true,230,12);assertSame(p.labels,OmrScoreInterpreter.normalizeHeaderSymbols(p.labels,null,W,H,REGIONS));}
    @Test public void inputPixelsArePreservedAndPreparationIsIdempotent(){var p=page(true,true,230,12);byte[] mask=p.labels.clone(),gray=p.gray.clone(),normalized=p.normalize();assertArrayEquals(mask,p.labels);assertArrayEquals(gray,p.gray);assertEquals(0,normalized[115*W+94]);assertArrayEquals(normalized,OmrScoreInterpreter.normalizeHeaderSymbols(normalized,p.gray,W,H,REGIONS));}
}
