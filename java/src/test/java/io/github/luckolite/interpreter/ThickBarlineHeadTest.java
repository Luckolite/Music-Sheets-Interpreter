// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff and ending-bar drawings with a small false head prediction. */
public class ThickBarlineHeadTest {
    private int notes(boolean full,boolean protrusion,boolean grayAvailable,boolean extending,boolean oval) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        if(!oval)for(int y=full?(extending?68:80):115;y<=(extending?156:144);y++)for(int x=201;x<=209;x++){gray[y*w+x]=0;labels[y*w+x]=5;}
        for(int y=130;y<=142;y++)for(int x=201;x<=209;x++) {
            if(Math.pow((x-205)/4.,2)+Math.pow((y-136)/6.,2)>1)continue;
            labels[y*w+x]=2;gray[y*w+x]=0;
        }
        if(protrusion||oval)for(int y=130;y<=142;y++)for(int x=191;x<=213;x++){
            if(Math.pow((x-202)/11.,2)+Math.pow((y-136)/6.,2)>1)continue;
            gray[y*w+x]=0;if(oval)labels[y*w+x]=2;
        }
        return OmrScoreInterpreter.extract(labels,grayAvailable?gray:null,w,h,List.of(new MeasureRegion(.05f,.95f,.2f,.85f))).size();
    }
    @Test public void predictionInsideFullEndingBarIsSilent(){assertEquals(0,notes(true,false,true,false,false));}
    @Test public void shorterStemIsNotAFullBar(){assertEquals(1,notes(false,false,true,false,false));}
    @Test public void rawOvalProtrusionPreservesSmallSemanticIsland(){assertEquals(1,notes(true,true,true,false,false));}
    @Test public void missingPixelsCannotEstablishBarline(){assertEquals(1,notes(true,false,false,false,false));}
    @Test public void strokeExtendingOutsideStaffIsNotPlainEndingBar(){assertEquals(1,notes(true,false,true,true,false));}
    @Test public void ordinaryFilledOvalRemainsNote(){assertEquals(1,notes(false,false,true,false,true));}
}
