// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic rest moved below its ordinary voice position. */
public class DisplacedPolyphonicRestTest {
    private CompactQuarterRestTest.Page page() {
        var p=new CompactQuarterRestTest.Page(16,true,true,false,false);
        byte[] old=p.gray.clone(),mask=p.labels.clone();
        for(int y=80;y<230;y++)for(int x=145;x<180;x++)if(mask[y*600+x]==5) {
            p.gray[y*600+x]=(byte)240;p.labels[y*600+x]=0;
        }
        for(int y=100;y<=164;y+=16)for(int x=145;x<180;x++){p.gray[y*600+x]=0;p.labels[y*600+x]=4;}
        for(int y=80;y<210;y++)for(int x=145;x<180;x++)if(mask[y*600+x]==5){p.gray[(y+16)*600+x]=old[y*600+x];p.labels[(y+16)*600+x]=5;}
        return p;
    }
    private ScoreNoteEvent held(int staff,int measure) {
        return new ScoreNoteEvent(measure,.6f,10,staff,1,110/280f,false,0,0,2,2);
    }
    private List<ScoreRestEvent> rests(List<ScoreNoteEvent> notes) {
        var p=page();return SixteenthRestDetector.detect(p.gray,600,280,
                List.of(new MeasureRegion(.04f,.95f,.2f,.9f)),
                List.of(new SixteenthRestDetector.Staff(100,164,16,0,1)),notes);
    }
    @Test public void heldVoiceProvesLowerRestPlacement(){assertEquals(1,rests(List.of(held(0,0))).size());}
    @Test public void noIndependentVoiceDoesNotAuthorizeShift(){assertTrue(rests(List.of()).isEmpty());}
    @Test public void otherMeasureDoesNotAuthorizeShift(){assertTrue(rests(List.of(held(0,1))).isEmpty());}
    @Test public void otherStaffDoesNotAuthorizeShift(){assertTrue(rests(List.of(held(1,0))).isEmpty());}
    @Test public void movingUpperVoiceCanShareRestColumn(){
        var moving=new ScoreNoteEvent(0,(160/600f-.04f)/.91f,10,0,1,110/280f,false,0,1,2,0);
        assertEquals(1,rests(List.of(held(0,0),moving)).size());
    }
}
