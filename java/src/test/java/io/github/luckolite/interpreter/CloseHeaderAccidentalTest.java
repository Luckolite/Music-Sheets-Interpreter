// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original repeated signature followed closely by an explicit note accidental. */
public final class CloseHeaderAccidentalTest {
    private void flat(JoinedSignatureSharpTest f,int x,int y) {
        for(int yy=y-28;yy<=y+7;yy++)for(int xx=x;xx<=x+2;xx++)f.ink(xx,yy,3);
        for(int yy=y-7;yy<=y+7;yy++)for(int xx=x+2;xx<=x+12;xx++) {
            double radius=Math.pow((xx-x-3)/9d,2)+Math.pow((yy-y)/7d,2);
            if(radius<=1&&radius>=.35)f.ink(xx,yy,3);
        }
    }
    private JoinedSignatureSharpTest page(boolean samePitch,boolean accidental) {
        var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);flat(f,80,132);
        f.row(310,0,0,false);flat(f,80,342);
        if(accidental)flat(f,104,samePitch?286:318);
        for(int y=282;y<=294;y++)for(int x=126;x<=142;x++)
            if((x-134)*(x-134)/64d+(y-288)*(y-288)/36d<=1)f.ink(x,y,2);
        for(int y=288;y<=340;y++)f.ink(126,y,1);
        var m=f.measures.get(1);f.measures.set(1,new MeasureRegion(115f/JoinedSignatureSharpTest.W,m.right(),m.top(),m.bottom()));
        return f;
    }
    @Test public void repeatedFlatThenSamePitchAccidentalDoesNotAddAFlat(){assertEquals(List.of(-1),page(true,true).keys());}
    @Test public void properlyOrderedTwoFlatSignatureStillChangesKey(){assertEquals(List.of(-1,-2),page(false,true).keys());}
    @Test public void singleRepeatedFlatStillHasOneFlat(){assertEquals(List.of(-1),page(true,false).keys());}
    @Test public void masksAreNotMutated(){var f=page(true,true);var g=f.gray.clone();var l=f.labels.clone();f.keys();assertArrayEquals(g,f.gray);assertArrayEquals(l,f.labels);}
    @Test public void clefDoesNotAuthorizeCloseUnorderedPairJustOutsideOldDistance(){
        var f=page(true,true);
        // Move only the musical head/stem six pixels right. The final accidental now
        // clears the old 1.35-space gate but is still a local printed accidental.
        for(int y=270;y<=345;y++)for(int x=150;x>=126;x--){if(x>=132){f.labels[y*JoinedSignatureSharpTest.W+x]=f.labels[y*JoinedSignatureSharpTest.W+x-6];f.gray[y*JoinedSignatureSharpTest.W+x]=f.gray[y*JoinedSignatureSharpTest.W+x-6];}else{f.labels[y*JoinedSignatureSharpTest.W+x]=0;f.gray[y*JoinedSignatureSharpTest.W+x]=(byte)255;}}
        assertEquals(List.of(-1),f.keys());
    }
}
