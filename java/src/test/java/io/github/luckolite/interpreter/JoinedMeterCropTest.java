// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original stacked digits beside long ink that merges their projection crop. */
public class JoinedMeterCropTest {
    private static final int W=400,H=180,X=270;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    public JoinedMeterCropTest() {
        Arrays.fill(gray,(byte)255);
        for(int y=60;y<=116;y+=14)for(int x=10;x<390;x++){gray[y*W+x]=0;labels[y*W+x]=4;}
        for(int y=65;y<=121;y+=14)rect(60,X,y,y,0);
        rect(X,X+2,60,116,0);
    }
    private void rect(int l,int r,int t,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)v;}
    private void counter(int y,boolean triangle){
        rect(X-9,X+2,y-2,y+6,0);
        for(int row=0;row<5;row++){int width=triangle?Math.min(6,row+2):6;rect(X-2-width+1,X-2,y+row,y+row,255);}
    }
    private void stackedSymbols() {
        for(int y=65;y<=110;y++)for(int x=X-60;x<=X-52;x++)
            labels[y*W+x]=OmrMeasurePostProcessor.CLEF_OR_KEY;
        for(int y=72;y<=83;y++)for(int x=X-9;x<=X+4;x++) {
            gray[y*W+x]=0;labels[y*W+x]=OmrMeasurePostProcessor.SYMBOL;
        }
        for(int y=96;y<=108;y++)for(int x=X-9;x<=X+4;x++) {
            gray[y*W+x]=0;labels[y*W+x]=OmrMeasurePostProcessor.SYMBOL;
        }
    }
    private boolean found(){return MeterChangeDetector.candidates(labels,gray,W,H).stream().anyMatch(c->c.left()<=X-9&&c.right()>X+2&&c.right()-c.left()<45&&c.top()<75&&c.bottom()>107);}
    @Test public void joinedInkStillYieldsBoundedMeterCrop(){counter(75,true);counter(103,true);assertTrue(found());}
    @Test public void joinedNonFourMeterSymbolsStillYieldCrop(){stackedSymbols();assertTrue(found());}
    @Test public void falseClefHeadCannotHideTwoAlignedMeterGlyphs(){
        stackedSymbols();
        for(int x=X-11;x<=X+6;x++)for(int y=67;y<=113;y++)
            if(y<=85||y>=95){gray[y*W+x]=0;labels[y*W+x]=OmrMeasurePostProcessor.SYMBOL;}
        for(int x=X-60;x<=X-58;x++)for(int y=84;y<=94;y++)
            labels[y*W+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertTrue(found());
    }
    @Test public void noteheadInsideJoinedSymbolsIsNotAMeter(){
        stackedSymbols();
        for(int y=87;y<=93;y++)for(int x=X-6;x<=X-1;x++)
            labels[y*W+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertFalse(found());
    }
    @Test public void plainVerticalStrokeDoesNotYieldMeterCrop(){assertFalse(found());}
    @Test public void singleCounterDoesNotYieldMeterCrop(){counter(75,true);assertFalse(found());}
    @Test public void parallelBarSpacesDoNotYieldMeterCrop(){counter(75,false);counter(103,false);assertFalse(found());}
    @Test public void tiltedJoinedDigitsStillYieldCrop(){
        counter(75,true);counter(103,true);byte[] g=gray.clone(),l=labels.clone();Arrays.fill(gray,(byte)255);Arrays.fill(labels,(byte)0);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){int yy=y+Math.round(.03f*(x-W*.5f));if(yy>=0&&yy<H){gray[yy*W+x]=g[y*W+x];labels[yy*W+x]=l[y*W+x];}}
        assertTrue(found());
    }
    @Test public void cropSearchPreservesSourceArrays(){counter(75,true);counter(103,true);byte[] g=gray.clone(),l=labels.clone();found();assertArrayEquals(g,gray);assertArrayEquals(l,labels);}
}
