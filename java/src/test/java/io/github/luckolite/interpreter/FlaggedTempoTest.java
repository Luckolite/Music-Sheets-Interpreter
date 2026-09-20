package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic tempo glyphs; no score images. */
public class FlaggedTempoTest {
    private ScoreTempoChange mark(boolean flag, boolean dot, int scale) {
        int w=300*scale,h=160*scale;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        rect(gray,w,scale,80,40,2,28);rect(gray,w,scale,73,64,9,6);
        if(flag){rect(gray,w,scale,82,41,4,4);rect(gray,w,scale,85,44,4,7);rect(gray,w,scale,87,50,2,6);}
        if(dot)rect(gray,w,scale,flag?93:87,65,3,3);
        rect(gray,w,scale,104,58,11,2);rect(gray,w,scale,104,63,11,2);
        rect(gray,w,scale,120,54,3,16);
        return TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(163,
                120f/300,54f/160,145f/300,70f/160)),gray,w,h,
                List.of(new MeasureRegion(.2f,.9f,.55f,.9f))).get(0);
    }
    private void rect(byte[] gray,int w,int s,int x,int y,int a,int b){
        for(int yy=y*s;yy<(y+b)*s;yy++)for(int xx=x*s;xx<(x+a)*s;xx++)gray[yy*w+xx]=0;
    }
    @Test public void eighthNotePulseRetainsHalfBpmWithoutRounding(){
        for(int s=1;s<=3;s++)assertEquals(new ScoreTempoChange(0,0,81.5,.5),mark(true,false,s));
    }
    @Test public void plainQuarterRetainsItsOriginalTempo(){assertEquals(new ScoreTempoChange(0,0,163),mark(false,false,1));}
    @Test public void dottedEighthAndQuarterUseTheirOwnPulseLengths(){
        assertEquals(new ScoreTempoChange(0,0,122.25,.75),mark(true,true,2));
        assertEquals(new ScoreTempoChange(0,0,244.5,1.5),mark(false,true,2));
    }
}
