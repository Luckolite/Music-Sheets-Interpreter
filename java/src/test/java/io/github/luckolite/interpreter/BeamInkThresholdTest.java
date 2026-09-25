// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original gray-paper beam rows, with no scanned score pixels. */
public class BeamInkThresholdTest {
    private BeamCoreSeparationTest.Page page(int paper) {
        var p=new BeamCoreSeparationTest.Page();Arrays.fill(p.gray,(byte)paper);
        for(int y=82;y<=146;y+=16)p.rect(0,y-2,199,y+2,90,4);
        return p;
    }
    @Test public void shadedPaperIsNotAnExtraInkBand()throws Exception{var p=page(145);p.rect(60,73,150,79,15,5);assertEquals(1,p.count());}
    @Test public void twoRealBeamsStaySeparateOnDarkPaper()throws Exception{var p=page(145);p.beams();assertEquals(2,p.count());}
    @Test public void emptyShadedPaperHasNoBeam()throws Exception{assertEquals(0,page(145).count());}
    @Test public void whitePaperRetainsTheExistingThreshold(){var p=page(255);assertEquals(165,BeamInkThreshold.at(p.gray,200,180,90,70,100,16));}
    @Test public void localPaperDeterminesTheContrastLimit(){var p=page(145);assertEquals(113,BeamInkThreshold.at(p.gray,200,180,90,70,100,16));}
    @Test public void noImageKeepsTheExistingFallback(){assertEquals(165,BeamInkThreshold.at(null,200,180,90,70,100,16));}
}
