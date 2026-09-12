// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rectangular rest and oval note drawings on a five-line staff. */
public class WholeMeasureRestHeadTest {
    private int notes(boolean rectangle,boolean stem,boolean hollow,boolean grayAvailable,int shift) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        int cy=103+shift;
        for(int y=cy-8;y<=cy+8;y++)for(int x=194;x<=216;x++) {
            double d=Math.pow((x-205)/11.,2)+Math.pow((y-cy)/8.,2);
            boolean ink=rectangle?y>=cy-5&&y<=cy+4:d<=1;
            if(ink){labels[y*w+x]=(byte)(rectangle&&(Math.pow((x-205)/3.,2)+Math.pow((y-cy)/5.,2)>1)?5:2);gray[y*w+x]=(byte)(hollow&&d<.45?255:0);}
        }
        if(stem)for(int y=cy-40;y<=cy;y++){labels[y*w+215]=1;gray[y*w+215]=0;}
        return OmrScoreInterpreter.extract(labels,grayAvailable?gray:null,w,h,List.of(new MeasureRegion(.05f,.95f,.2f,.85f))).size();
    }
    @Test public void wholeRestRectangleIsSilent(){assertEquals(0,notes(true,false,false,true,0));}
    @Test public void filledOvalAtSamePitchRemainsNote(){assertEquals(1,notes(false,false,false,true,0));}
    @Test public void stemmedOvalRemainsNote(){assertEquals(1,notes(false,true,false,true,0));}
    @Test public void hollowOvalRemainsNote(){assertEquals(1,notes(false,false,true,true,0));}
    @Test public void attachedStemPreventsRestClassification(){assertEquals(1,notes(true,true,false,true,0));}
    @Test public void rectangleAwayFromRestLineIsNotRemoved(){assertEquals(1,notes(true,false,false,true,-16));}
    @Test public void missingPixelsCannotEstablishRest(){assertEquals(1,notes(true,false,false,false,0));}
}
