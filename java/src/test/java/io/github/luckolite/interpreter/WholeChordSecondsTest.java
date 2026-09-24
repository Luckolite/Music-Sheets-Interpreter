// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic geometry; no score scans or model predictions. */
public class WholeChordSecondsTest {
    private static List<ScoreNoteEvent> chord(boolean open,boolean dots) {
        int w=600,h=300;
        byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y:new int[]{60,80,100,120,140})for(int x=20;x<580;x++) {
            labels[y*w+x]=OmrMeasurePostProcessor.STAFF;gray[y*w+x]=0;
        }
        for(int[] center:new int[][]{{220,100},{220,120},{220,160},{255,150}}) {
            int cx=center[0],cy=center[1];
            for(int y=cy-10;y<=cy+10;y++)for(int x=cx-18;x<=cx+18;x++) {
                double oval=Math.pow((x-cx)/18.0,2)+Math.pow((y-cy)/10.0,2);
                if(oval<=1) {labels[y*w+x]=OmrMeasurePostProcessor.NOTEHEAD;
                    if(!open||oval>=.40)gray[y*w+x]=0;}
            }
        }
        // A thin model bridge joins the adjacent seconds, not their raw white centres.
        for(int y=153;y<=157;y++)for(int x=236;x<=240;x++)labels[y*w+x]=OmrMeasurePostProcessor.NOTEHEAD;
        if(dots)for(int cy:new int[]{95,115,145,165})for(int y=cy-3;y<=cy+3;y++)for(int x=285-3;x<=285+3;x++)
            if((x-285)*(x-285)+(y-cy)*(y-cy)<=9)gray[y*w+x]=0;
        return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.1f,.65f)));
    }
    @Test public void displacedWholeSecondsRetainBothTonesAndDotColumn() {
        var notes=chord(true,true);
        assertEquals(4,notes.size());
        assertEquals(Set.of(-2,-1,2,4),new HashSet<>(notes.stream().map(ScoreNoteEvent::staffStep).toList()));
        for(var n:notes) {assertEquals(4f,n.unbeamedDurationBeats(),.01f);assertEquals(1,n.augmentationDots());
            assertEquals(notes.get(0).positionInMeasure(),n.positionInMeasure(),.001f);}
    }
    @Test public void openSecondsDoNotInventUnprintedDots() {
        var notes=chord(true,false);assertEquals(4,notes.size());
        for(var n:notes)assertEquals(0,n.augmentationDots());
    }
    @Test public void wideSolidBlobIsNotSplitAsWholeSeconds() {
        assertTrue(chord(false,false).size()<4);
    }
}
