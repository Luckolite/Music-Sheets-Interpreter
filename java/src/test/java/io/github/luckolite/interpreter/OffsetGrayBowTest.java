// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ellipses with local scan ink at the edge, not copied score pixels. */
public class OffsetGrayBowTest {
    ShadedNoteheadRecoveryTest smudged(boolean down,int paper,int fill) {
        var p=new ShadedNoteheadRecoveryTest();p.fixture(down,paper,fill,false);
        // An isolated dark scan row flattens only the middle gray-core contour.
        for(int x=78;x<=102;x++)if(x<83||x>97)p.gray[89*240+x]=30;
        return p;
    }
    @Test public void adjacentInnerRowsRecoverOffsetBow(){assertEquals(1,smudged(false,207,140).find().size());}
    @Test public void downstemHasTheSameContourProof(){assertEquals(1,smudged(true,207,140).find().size());}
    @Test public void lowContrastDoesNotGainRecovery(){assertTrue(smudged(false,190,150).find().isEmpty());}
    @Test public void semanticStemStillRequired(){var p=smudged(false,207,140);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
    @Test public void darkStemStillRequired(){var p=smudged(false,207,140);for(int y=42;y<78;y++)p.gray[y*240+102]=(byte)207;assertTrue(p.find().isEmpty());}
    @Test public void straightRectangleStillRejected(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,207,140,true);assertTrue(p.find().isEmpty());}
    @Test public void oneRowRasterNickDoesNotCurveRectangle(){
        var p=new ShadedNoteheadRecoveryTest();p.fixture(false,207,140,true);
        // Only one core row gets wider; two adjacent inner rows cannot support it.
        p.gray[89*240+78]=(byte)140;
        assertTrue(p.find().isEmpty());
    }
    @Test public void decoderRecoversOneCorrectQuarter(){
        var p=smudged(false,207,140);
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,240,220,List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
        assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
        assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
    }
}
