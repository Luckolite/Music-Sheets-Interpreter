// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original five-line staff and treble-like loop drawings. */
public class TrebleTailHeadTest {
    private int notes(boolean contact,boolean tall,boolean narrow,boolean right,boolean above) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        // Two loops and a descending spine occupy the clef class.
        for(int y=tall?56:96;y<=164;y++)for(int x=60;x<=104;x++) {
            boolean ink=Math.abs(x-(85+(y-100)*.12))<=2;
            double loop=Math.pow((x-82)/(narrow?4.:20.),2)+Math.pow((y-115)/24.,2);
            double upper=Math.pow((x-84)/(narrow?4.:12.),2)+Math.pow((y-80)/24.,2);
            ink|=loop>=.56&&loop<=1||tall&&upper>=.55&&upper<=1;
            if(ink){labels[y*w+x]=3;gray[y*w+x]=0;}
        }
        int cx=right?120:78,cy=above?134:157;
        // Connect the bulb to the descending clef tail only in the positive fixture.
        if(contact)for(int x=78;x<=94;x++)for(int y=158;y<=162;y++){labels[y*w+x]=3;gray[y*w+x]=0;}
        for(int y=cy-6;y<=cy+6;y++)for(int x=cx-6;x<=cx+6;x++) {
            if(Math.pow((x-cx)/6.,2)+Math.pow((y-cy)/6.,2)>1)continue;
            labels[y*w+x]=2;gray[y*w+x]=0;
        }
        return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.2f,.85f))).size();
    }
    @Test public void connectedLowerTrebleTipIsSilent(){assertEquals(0,notes(true,true,false,false,false));}
    @Test public void detachedLowerNoteRemains(){assertEquals(1,notes(false,true,false,false,false));}
    @Test public void neighboringNoteRemains(){assertEquals(1,notes(true,true,false,true,false));}
    @Test public void shortAccidentalCannotStandInForTrebleClef(){assertEquals(1,notes(true,false,false,false,false));}
    @Test public void narrowBracketCannotStandInForTrebleClef(){assertEquals(1,notes(true,true,true,false,false));}
    @Test public void noteInsideStaffIsNotALowerTip(){assertEquals(1,notes(false,true,false,false,true));}
}
