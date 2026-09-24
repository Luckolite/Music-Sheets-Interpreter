// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic pale filled notation; no source-score pixels. */
public class FadedNoteheadRecoveryTest {
    private static final int W=240,H=220;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];

    private void fixture(boolean down,int paper,int inside,boolean rectangle) {
        Arrays.fill(gray,(byte)paper);Arrays.fill(labels,(byte)0);
        for(int y=60;y<=140;y+=20)for(int x=15;x<225;x++) {
            gray[y*W+x]=30;labels[y*W+x]=4;
        }
        for(int y=81;y<=99;y++)for(int x=78;x<=102;x++) {
            float ellipse=(x-90)*(x-90)/144f+(y-90)*(y-90)/81f;
            if(rectangle||ellipse<=1)gray[y*W+x]=(byte)(rectangle
                    ?(x==78||x==102||y==81||y==99?30:inside):ellipse>.65f?30:inside);
        }
        int sx=down?78:102;
        for(int y=down?90:42;y<=(down?138:90);y++) {
            gray[y*W+sx]=30;labels[y*W+sx]=1;
        }
    }
    private List<FadedNoteheadRecovery.Head> find(){return FadedNoteheadRecovery.find(labels,gray,W,H,20,40,160);}
    @Test public void recoversUpStemPaleFilledHead(){fixture(false,240,175,false);assertEquals(1,find().size());}
    @Test public void recoversDownStemPaleFilledHead(){fixture(true,240,175,false);assertEquals(1,find().size());}
    @Test public void toleratesShadedPaperWithPrintedContrast(){fixture(false,205,175,false);assertEquals(1,find().size());}
    @Test public void doesNotTreatHollowHeadAsPaleFilled(){fixture(false,240,240,false);assertTrue(find().isEmpty());}
    @Test public void doesNotRecoverLowContrastPaperPocket(){fixture(false,195,180,false);assertTrue(find().isEmpty());}
    @Test public void excludesRectangularStaffCell(){fixture(false,240,175,true);assertTrue(find().isEmpty());}
    @Test public void requiresSemanticStemSupport(){fixture(false,240,175,false);Arrays.fill(labels,(byte)0);assertTrue(find().isEmpty());}
    @Test public void requiresPrintedStem(){fixture(false,240,175,false);for(int y=42;y<78;y++)gray[y*W+102]=(byte)240;assertTrue(find().isEmpty());}
    @Test public void doesNotDuplicateRecognizedHead(){fixture(false,240,175,false);labels[90*W+90]=2;assertTrue(find().isEmpty());}
    @Test public void requiresClosedOutline(){fixture(false,240,175,false);for(int x=75;x<=85;x++)gray[90*W+x]=(byte)240;assertTrue(find().isEmpty());}
    @Test public void ignoresMissingImage(){assertTrue(FadedNoteheadRecovery.find(labels,null,W,H,20,40,160).isEmpty());}
    @Test public void integratesIntoDecodedNotesWithPitchAndQuarterDuration(){
        fixture(false,240,175,false);
        List<ScoreNoteEvent> notes=OmrScoreInterpreter.extract(labels,gray,W,H,
                List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
        assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
        assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
    }
}
