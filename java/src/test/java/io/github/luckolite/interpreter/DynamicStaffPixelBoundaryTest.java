// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicStaffPixelBoundaryTest {
    private List<ScoreDynamicChange> detect(float top,float bottom) {
        var word=new PlayingTechniqueDetector.Word("mp",.4f,top/200,.6f,bottom/200);
        var staff=new PlayingTechniqueDetector.Staff(50,104,13.5f,0,1);
        var region=new MeasureRegion(.1f,.9f,.15f,.60f);
        var note=new ScoreNoteEvent(0,.5f,4,0,1,.385f,false,0,0,2,1);
        return ScoreDynamicsDetector.detect(List.of(word),List.of(staff),List.of(region),List.of(note),null,200,200);
    }
    @Test public void belowCropRoundedPixelBoundaryStillOwnsItsWord(){assertEquals(1,detect(107,125).size());}
    @Test public void aboveCropRoundedPixelBoundaryStillOwnsItsWord(){assertEquals(1,detect(27,47).size());}
    @Test public void actualStaffIntrusionIsStillRejected(){assertTrue(detect(104,124).isEmpty());}
}
