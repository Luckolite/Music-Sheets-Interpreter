// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original staff rules and tempo strokes, not score-derived pixels. */
public class TempoStaffOwnershipTest {
    List<ScoreTempoChange> read(boolean staff,int top,int bottom) {
        int w=360,h=240;byte[] g=new byte[w*h];Arrays.fill(g,(byte)255);
        if(staff)for(int y=100;y<=148;y+=12)for(int x=20;x<340;x++)g[y*w+x]=0;
        for(int x=104;x<=116;x++){g[(top+6)*w+x]=0;g[(top+12)*w+x]=0;}
        return TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(144,120f/w,top/(float)h,150f/w,bottom/(float)h)),g,w,h,List.of(new MeasureRegion(.1f,.94f,80f/h,165f/h)));
    }
    @Test public void directionAbovePrintedStaffSurvivesTallClefBox(){assertEquals(List.of(new ScoreTempoChange(0,0,144)),read(true,76,94));}
    @Test public void withoutFiveRulesInteriorInkIsStillRejected(){assertTrue(read(false,76,94).isEmpty());}
    @Test public void actualInStaffDigitsStayRejected(){assertTrue(read(true,104,122).isEmpty());}
}
