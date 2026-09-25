// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;

/** Original three-page route: the display cursor follows the source while the clock advances. */
public class NavigationPageTimelineTest {
    @Test public void sourceCursorJumpsBackAndThenToCodaAcrossPages(){
        int[] counts={5,6,3},starts={1,6,12};var range=new ScorePageTimeline.PageRange(0,3);
        var plan=ScoreNavigationPlan.create(14,List.of(new ScorePlaybackDirection(3,SEGNO),
                new ScorePlaybackDirection(7,TO_CODA),new ScorePlaybackDirection(10,DAL_SEGNO_AL_CODA),
                new ScorePlaybackDirection(11,CODA)));
        int[] offsets=ScorePageTimeline.playbackOffsets(counts,starts,range);
        assertEquals(17,plan.measureCount());
        int[][] checks={{9,1,4},{10,0,3},{11,0,4},{12,1,0},{13,1,1},{14,2,0},{16,2,2}};
        for(int[] c:checks){int source=plan.sourceMeasure(c[0]);int page=ScorePageTimeline.pageForMeasure(source,counts,starts,range);
            assertEquals(c[1],page);assertEquals(c[2],source-offsets[page]);}
    }
    @Test public void noAnchorsKeepEverySourceCursorPositionIdentical(){
        int[] counts={5,6,3},starts={1,6,12};var range=new ScorePageTimeline.PageRange(0,3);
        var plan=ScoreNavigationPlan.create(14,List.of());
        for(int p=0;p<14;p++)assertEquals(ScorePageTimeline.pageForMeasure(p,counts,starts,range),
                ScorePageTimeline.pageForMeasure(plan.sourceMeasure(p),counts,starts,range));
    }
}
