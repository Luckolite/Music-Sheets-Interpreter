// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original thick elliptical outlines surrounding a flat quantized gray core. */
public class OutlinedGrayNeckTest {
    ShadedChordHeadRecoveryTest fixture(int count,boolean up,boolean curved) {
        var p=new ShadedChordHeadRecoveryTest();p.fixture(count,up,true,245);
        if(curved)for(int y=60;y<=70+20*(count-1)+10;y++){
            double dy=10;for(int i=0;i<count;i++)dy=Math.min(dy,Math.abs(y-(70+20*i)));
            int edge=(int)Math.round(78-5*Math.sqrt(1-dy*dy/100));
            for(int x=edge;x<=78;x++)p.gray[y*220+x]=30;
        }
        return p;
    }
    @Test public void printedThreeLobesSurviveFlatGrayNecks(){var c=fixture(3,true,true).find();assertEquals(1,c.size());assertEquals(3,c.get(0).heads().size());}
    @Test public void fourPrintedLobesStayFour(){var c=fixture(4,true,true).find();assertEquals(1,c.size());assertEquals(4,c.get(0).heads().size());}
    @Test public void unchangedRectangleRemainsRejected(){assertTrue(fixture(3,true,false).find().isEmpty());}
    @Test public void missingSemanticStemStillRejects(){var p=fixture(3,true,true);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
    @Test public void everyNeckNeedsIndependentCurvature(){var p=fixture(3,true,true);for(int y=74;y<=87;y++)for(int x=72;x<79;x++)p.gray[y*220+x]=30;assertTrue(p.find().isEmpty());}
    @Test public void straightOuterBandDoesNotProveLobes(){var p=fixture(3,true,true);for(int y=60;y<=120;y++)for(int x=72;x<79;x++)p.gray[y*220+x]=30;assertTrue(p.find().isEmpty());}
    @Test public void decoderRetainsThreeIndependentPitchEvents(){
        var p=fixture(3,true,true);p.staff(50);
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,220,250,List.of(new MeasureRegion(.05f,.95f,.15f,.75f)));
        assertEquals(3,notes.size());
    }
}
