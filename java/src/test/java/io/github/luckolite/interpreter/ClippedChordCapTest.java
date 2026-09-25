// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original lobed chords whose gray fill meets an independently printed rule. */
public class ClippedChordCapTest {
    ShadedChordHeadRecoveryTest fixture(boolean leftRule,boolean rightRule,boolean rectangle) {
        var p=new ShadedChordHeadRecoveryTest();p.fixture(3,true,rectangle,245);
        for(int y=109;y<=115;y++)for(int x=79;x<=101;x++)p.gray[y*220+x]=(byte)185;
        for(int y=115;y<=116;y++)for(int x=65;x<=117;x++)
            if(x>=79&&x<=101||x<79&&leftRule||x>101&&rightRule)p.gray[y*220+x]=30;
        return p;
    }
    @Test public void clippedCapWithBothRuleFlanksStillHasThreeHeads(){
        var chords=fixture(true,true,false).find();assertEquals(1,chords.size());
        assertEquals(3,chords.get(0).heads().size());
        assertEquals(70,chords.get(0).heads().get(0).centerY(),2);
        assertEquals(110,chords.get(0).heads().get(2).centerY(),2);
    }
    @Test public void leftFlankAloneCannotReplaceCurvature(){assertTrue(fixture(true,false,false).find().isEmpty());}
    @Test public void rightFlankAloneCannotReplaceCurvature(){assertTrue(fixture(false,true,false).find().isEmpty());}
    @Test public void flatCoreWithoutIndependentRuleIsRejected(){assertTrue(fixture(false,false,false).find().isEmpty());}
    @Test public void repeatedLobesStillRequired(){assertTrue(fixture(true,true,true).find().isEmpty());}
    @Test public void semanticStemStillRequired(){var p=fixture(true,true,false);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
    @Test public void broadDarkBandIsNotAThinRule(){
        var p=fixture(true,true,false);
        for(int y=111;y<=121;y++)for(int x=65;x<=117;x++)if(x<79||x>101)p.gray[y*220+x]=30;
        assertTrue(p.find().isEmpty());
    }
    @Test public void fullDecoderKeepsThreeCorrectPitches(){
        var p=fixture(true,true,false);p.staff(50);
        var notes=OmrScoreInterpreter.extract(p.labels,p.gray,220,250,List.of(new MeasureRegion(.05f,.95f,.15f,.75f)));
        assertEquals(3,notes.size());
        assertEquals(Set.of(2,4,6),notes.stream().map(ScoreNoteEvent::staffStep).collect(Collectors.toSet()));
    }
}
