// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original pixel-quantized curved fills with independent printed/semantic stems. */
public class QuantizedGrayBowTest {
    ShadedNoteheadRecoveryTest fixture(boolean down,int paper,int fill,boolean curved) {
        var p=new ShadedNoteheadRecoveryTest();p.fixture(down,paper,fill,false);
        for(int y=81;y<=99;y++)for(int x=78;x<=102;x++)p.gray[y*240+x]=(byte)paper;
        // Opposite caps retreat by one pixel. Averaging their edge positions
        // leaves half a pixel of measurable bow, unlike a straight rectangle.
        for(int y=82;y<=98;y++)for(int x=79;x<=101;x++)
            if(!curved||!(x==79&&y<=85||x==101&&y>=95))p.gray[y*240+x]=(byte)fill;
        int stem=down?78:102;
        for(int y=down?90:42;y<=(down?138:90);y++){
            p.gray[y*240+stem]=30;p.labels[y*240+stem]=1;
        }
        return p;
    }
    @Test public void halfPixelBowRecoversGrayUpstem(){assertEquals(1,fixture(false,240,185,true).find().size());}
    @Test public void halfPixelBowRecoversGrayDownstem(){assertEquals(1,fixture(true,240,185,true).find().size());}
    @Test public void darkGrayPassHasSameQuantizedGeometry(){assertEquals(1,fixture(false,220,140,true).find().size());}
    @Test public void denseGrayPassHasSameQuantizedGeometry(){assertEquals(1,fixture(false,220,120,true).find().size());}
    @Test public void straightRectangleIsStillRejected(){assertTrue(fixture(false,240,185,false).find().isEmpty());}
    @Test public void lowContrastIsStillRejected(){assertTrue(fixture(false,205,185,true).find().isEmpty());}
    @Test public void independentSemanticStemIsRequired(){var p=fixture(false,240,185,true);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
    @Test public void independentRawStemIsRequired(){var p=fixture(false,240,185,true);for(int y=42;y<78;y++)p.gray[y*240+102]=(byte)240;assertTrue(p.find().isEmpty());}
    @Test public void curvatureThresholdScalesWithStaffGap(){
        var p=fixture(false,240,185,true);byte[] gray=new byte[480*440],labels=new byte[gray.length];
        for(int y=0;y<440;y++)for(int x=0;x<480;x++){
            gray[y*480+x]=p.gray[(y/2)*240+x/2];labels[y*480+x]=p.labels[(y/2)*240+x/2];
        }
        assertEquals(1,ShadedNoteheadRecovery.find(labels,gray,480,440,40,80,320).size());
    }
    @Test public void decoderRecoversOnlyOneQuarterWithCorrectPitch(){
        var p=fixture(false,240,185,true);
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,240,220,List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
        assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
        assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
    }
}
