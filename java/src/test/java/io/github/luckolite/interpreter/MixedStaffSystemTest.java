// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original mixed-size instrumental systems with printed connecting rules. */
public class MixedStaffSystemTest {
    @Test public void smallerSoloStaffDoesNotDetachTheLowerPianoStaff() {
        var measures = score(true);
        assertEquals("Three shared measures, not a separate bass timeline", 3, measures.size());
        assertTrue(measures.get(0).top() < .2f);
        assertTrue(measures.get(0).bottom() > .8f);
    }

    @Test public void matchingBoundariesStillNeedAPrintedConnector() {
        var measures = score(false);
        assertEquals(9, measures.size());
        assertTrue(measures.get(2).bottom() < measures.get(3).top());
        assertTrue(measures.get(5).bottom() < measures.get(6).top());
    }

    private static java.util.List<MeasureRegion> score(boolean connected) {
        int width=1000,height=550;
        byte[] labels=new byte[width*height],gray=new byte[labels.length];
        Arrays.fill(gray,(byte)255);
        int[] tops={60,200,385},bars={80,340,660,930};
        float[] gaps={11.75f,15.25f,15.5f};
        for(int staff=0;staff<tops.length;staff++) {
            int top=tops[staff],bottom=Math.round(top+gaps[staff]*4);
            for(int line=0;line<5;line++)for(int x=80;x<=930;x++) {
                int at=((int)(top+line*gaps[staff]))*width+x;
                labels[at]=OmrMeasurePostProcessor.STAFF;gray[at]=0;
            }
            for(int x:bars)for(int y=top;y<=bottom;y++) {
                labels[y*width+x]=OmrMeasurePostProcessor.STEM_OR_REST;gray[y*width+x]=0;
            }
        }
        if(connected)for(int y=60;y<=447;y++)gray[y*width+80]=0;
        return OmrMeasurePostProcessor.process(labels,gray,width,height);
    }
}
