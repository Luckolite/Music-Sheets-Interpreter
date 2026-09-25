// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric header letters and C-shaped meter signs. */
public class HeaderKeyShapeGuardTest {
    private void flat(JoinedSignatureSharpTest f,int x,int pitch) {
        for(int y=pitch-28;y<=pitch+7;y++)for(int xx=x;xx<=x+2;xx++)f.ink(xx,y,3);
        for(int y=pitch-7;y<=pitch+7;y++)for(int xx=x+2;xx<=x+12;xx++) {
            double r=Math.pow((xx-x-3)/9d,2)+Math.pow((y-pitch)/7d,2);
            if(r<=1&&r>=.35)f.ink(xx,y,3);
        }
    }
    private JoinedSignatureSharpTest page(boolean cut,boolean sign) {
        var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);
        flat(f,64,132);flat(f,85,108);flat(f,106,140);
        if(sign) {
            for(int y=113;y<=151;y++)for(int x=137;x<=163;x++) {
                double outer=Math.pow((x-150)/13d,2)+Math.pow((y-132)/19d,2);
                double inner=Math.pow((x-150)/8d,2)+Math.pow((y-132)/14d,2);
                if(outer<=1&&inner>=1&&!(x>150&&Math.abs(y-132)<7))f.gray[y*JoinedSignatureSharpTest.W+x]=0;
            }
            if(cut)for(int y=109;y<=155;y++)for(int x=149;x<=151;x++)f.gray[y*JoinedSignatureSharpTest.W+x]=0;
            // Only the curved left shoulder survives as a semantic accidental.
            for(int y=123;y<=140;y++)for(int x=137;x<=145;x++)
                if(f.gray[y*JoinedSignatureSharpTest.W+x]==0)f.labels[y*JoinedSignatureSharpTest.W+x]=3;
        }
        return f;
    }
    @Test public void flatShapedLetterAboveStaffCannotInventAKey(){var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);flat(f,80,83);assertEquals(List.of(),f.keys());}
    @Test public void realFlatWithinStaffRemains(){var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);flat(f,80,132);assertEquals(List.of(-1),f.keys());}
    @Test public void threeFlatsBeforeCutTimeRemainThreeFlats(){assertEquals(List.of(-3),page(true,true).keys());}
    @Test public void threeFlatsBeforeCommonTimeRemainThreeFlats(){assertEquals(List.of(-3),page(false,true).keys());}
    @Test public void threeFlatsWithoutMeterRemainThreeFlats(){assertEquals(List.of(-3),page(false,false).keys());}
    @Test public void genuineSharpStillSurvives(){var f=new JoinedSignatureSharpTest();f.row(100,1,0,false);assertEquals(List.of(1),f.keys());}
    @Test public void sharpInHighSignatureSlotStillSurvives(){var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);f.sharp(80,76);assertEquals(List.of(1),f.keys());}
    @Test public void inputPixelsAreNotChanged(){var f=page(true,true);var l=f.labels.clone();var g=f.gray.clone();f.keys();assertArrayEquals(l,f.labels);assertArrayEquals(g,f.gray);}
}
