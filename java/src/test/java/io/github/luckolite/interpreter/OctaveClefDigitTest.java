// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class OctaveClefDigitTest {
    @Test public void eightHasTwoCountersAndOrdinaryLoopHasOne() {
        byte[] gray=new byte[9*15];Arrays.fill(gray,(byte)255);
        for(int y=1;y<14;y++)for(int x=1;x<8;x++)
            if(x==1||x==7||y==1||y==7||y==13)gray[y*9+x]=0;
        assertEquals(2,OctaveClefDigit.holes(gray,9,0,0,9,15));
        for(int x=2;x<7;x++)gray[7*9+x]=(byte)255;
        assertEquals(1,OctaveClefDigit.holes(gray,9,0,0,9,15));
    }
    @Test public void octaveClefSurvivesRhythmAndAccidentalCopies() {
        var note=new ScoreNoteEvent(0,.2f,0,0,1,.3f,false).withClef(ScoreNoteEvent.CLEF_TREBLE_OTTAVA);
        assertEquals(37,note.withLeadingRest(.25f).withCompactOpening().clefBottomDiatonic());
        assertEquals(37,note.diatonicPitchIdentity());
    }
}
