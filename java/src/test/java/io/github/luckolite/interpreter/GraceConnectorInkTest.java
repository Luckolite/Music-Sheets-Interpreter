// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact curved and straight marks, with no source-score pixels. */
public final class GraceConnectorInkTest {
    private static final int W=160,H=160;
    private static byte[] page(){byte[] p=new byte[W*H];Arrays.fill(p,(byte)255);return p;}
    private static void ink(byte[] p,int x,int y){p[y*W+x]=0;}
    private static byte[] hook(){
        byte[] p=page();
        for(int x=65;x<=83;x++) {
            double t=(x-65)/18.;int y=(int)Math.round(82-9*t+3*Math.sin(Math.PI*t));
            for(int dy=-1;dy<=1;dy++)ink(p,x,y+dy);
        }
        return p;
    }
    private static boolean test(byte[] p){return GraceConnectorInk.isHook(p,W,H,66,74,81,84,74,80,48,14,100);}
    @Test public void compactRisingHookIsRecognized(){assertTrue(test(hook()));}
    @Test public void sourcePixelsAreUnchanged(){byte[] p=hook(),copy=p.clone();test(p);assertArrayEquals(copy,p);}
    @Test public void shadedPaperMaintainsInkSeparation(){byte[] p=hook();for(int i=0;i<p.length;i++)p[i]=(byte)((p[i]&255)==0?55:170);assertTrue(test(p));}
    @Test public void missingRawIsNotEvidence(){assertFalse(test(null));}
    @Test public void blankPageIsNotConnector(){assertFalse(test(page()));}
    @Test public void straightTiltedStrokeIsNotHook(){byte[] p=page();for(int x=65;x<=83;x++){int y=(int)Math.round(82-(x-65)*.5);for(int d=-1;d<=1;d++)ink(p,x,y+d);}assertFalse(test(p));}
    @Test public void horizontalStrokeIsNotHook(){byte[] p=page();for(int x=65;x<=83;x++)for(int y=79;y<=81;y++)ink(p,x,y);assertFalse(test(p));}
    @Test public void filledOvalIsNotHook(){byte[] p=page();for(int y=75;y<=85;y++)for(int x=66;x<=82;x++)if(Math.pow((x-74)/8.,2)+Math.pow((y-80)/5.,2)<=1)ink(p,x,y);assertFalse(test(p));}
    @Test public void tiltedFilledOvalIsNotHook(){byte[] p=page();for(int y=70;y<=90;y++)for(int x=66;x<=82;x++)if(Math.pow((x-74)/8.,2)+Math.pow((y-80+(x-74)*.4)/4.,2)<=1)ink(p,x,y);assertFalse(test(p));}
    @Test public void hollowOvalIsNotHook(){byte[] p=page();for(int y=74;y<=86;y++)for(int x=65;x<=83;x++){double r=Math.pow((x-74)/8.,2)+Math.pow((y-80)/5.,2);if(r<=1.2&&r>=.5)ink(p,x,y);}assertFalse(test(p));}
    @Test public void thickWedgeIsNotHook(){byte[] p=page();for(int x=65;x<=83;x++)for(int y=80-(x-65)/2;y<=84;y++)ink(p,x,y);assertFalse(test(p));}
    @Test public void invalidDimensionsAreRejected(){assertFalse(GraceConnectorInk.isHook(hook(),W+1,H,66,74,81,84,74,80,48,14,100));}
}
