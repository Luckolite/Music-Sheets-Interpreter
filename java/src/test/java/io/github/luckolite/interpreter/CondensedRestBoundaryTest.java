// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original system geometry: a short rest followed by four ordinary written bars. */
public class CondensedRestBoundaryTest {
    private static List<MeasureRegion> page() {
        List<MeasureRegion> result=new ArrayList<>();
        for(int row=0;row<5;row++) {
            float y=.1f+row*.16f;
            if(row==2) {
                result.add(new MeasureRegion(.1f,.15f,y,y+.08f));
                for(int bar=0;bar<4;bar++)result.add(new MeasureRegion(.16f+bar*.185f,.335f+bar*.185f,y,y+.08f));
            } else for(int bar=0;bar<4;bar++)result.add(new MeasureRegion(.1f+bar*.2f,.29f+bar*.2f,y,y+.08f));
        }
        return result;
    }
    private static MeasureNumberReconciler.NumberToken count() {
        return new MeasureNumberReconciler.NumberToken(2,.12f,.4f,.14f,.42f);
    }
    @Test public void aCondensedRestDoesNotSwallowTheFollowingWrittenBar() {
        var raw=page();var result=MeasureNumberReconciler.reconcile(raw,List.of(),List.of(count()));
        assertEquals(22,result.size());assertTrue(result.contains(raw.get(9)));
    }
    @Test public void allWrittenBarsRetainTheirOriginalGeometry() {
        var raw=page();var result=MeasureNumberReconciler.reconcile(raw,List.of(),List.of(count()));
        for(int i=0;i<raw.size();i++)if(i!=8)assertTrue("written bar "+i,result.contains(raw.get(i)));
    }
    @Test public void aDifferentRowsRestDoesNotDisableStemCorrectionHere() {
        var other=new MeasureNumberReconciler.NumberToken(2,.17f,.08f,.20f,.10f);
        var result=MeasureNumberReconciler.reconcile(page(),List.of(),List.of(other));
        assertEquals(21,result.size());
    }
    @Test public void anUncorroboratedNarrowRegionStillUsesTheExistingCorrection() {
        assertEquals(20,MeasureNumberReconciler.reconcile(page(),List.of(),List.of()).size());
    }
}
