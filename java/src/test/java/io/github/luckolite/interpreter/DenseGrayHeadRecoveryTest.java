// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original dark-gray ellipses and counterexamples, not copied score pixels. */
public class DenseGrayHeadRecoveryTest {
    ShadedNoteheadRecoveryTest fixture(boolean down,int paper,int fill,boolean rectangle) {
        var p=new ShadedNoteheadRecoveryTest();p.fixture(down,paper,fill,rectangle);return p;
    }
    @Test public void denseGrayUpstemIsRecovered(){assertEquals(1,fixture(false,200,120,false).find().size());}
    @Test public void denseGrayDownstemIsRecovered(){assertEquals(1,fixture(true,200,120,false).find().size());}
    @Test public void strongerContrastIsRequiredForDensePass(){assertTrue(fixture(false,190,129,false).find().isEmpty());}
    @Test public void deeplyShadedPaperDoesNotQualify(){assertTrue(fixture(false,185,110,false).find().isEmpty());}
    @Test public void denseRectangleIsNotAHead(){assertTrue(fixture(false,200,120,true).find().isEmpty());}
    @Test public void noSemanticStemMeansNoHead(){var p=fixture(false,200,120,false);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
    @Test public void noRawStemMeansNoHead(){var p=fixture(false,200,120,false);for(int y=42;y<78;y++)p.gray[y*240+102]=(byte)200;assertTrue(p.find().isEmpty());}
    @Test public void existingMediumHeadRemainsSingle(){assertEquals(1,fixture(false,245,185,false).find().size());}
    @Test public void existingDarkHeadRemainsSingle(){assertEquals(1,fixture(false,207,140,false).find().size());}
    @Test public void decoderPreservesPitchAndQuarterValue(){
        var p=fixture(false,200,120,false);
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,240,220,List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
        assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
        assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
    }
}
