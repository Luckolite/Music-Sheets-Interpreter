// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
public class DoubleSharpGlyphTest {
    @Test public void diagonalCrossIsDistinctFromSolidOrSharpGlyph() {
        byte[] cross=new byte[225],solid=new byte[225],sharp=new byte[225];
        for(int y=0;y<15;y++)for(int x=0;x<15;x++) {
            if(Math.abs(x-y)<=2||Math.abs(x+y-14)<=2)cross[y*15+x]=3;
            solid[y*15+x]=3;
            if(x==4||x==10||y==4||y==10)sharp[y*15+x]=3;
        }
        assertTrue(DoubleSharpGlyph.matches(cross,15,15,0,0,14,14,(byte)3,14));
        assertFalse(DoubleSharpGlyph.matches(solid,15,15,0,0,14,14,(byte)3,14));
        assertFalse(DoubleSharpGlyph.matches(sharp,15,15,0,0,14,14,(byte)3,14));
    }
    @Test public void doubleSharpSurvivesNoteStorageAndMeansTwoSemitones() {
        var note=new ScoreNoteEvent(0,.2f,0,0,1,.3f,false,0,0,ScoreNoteEvent.ACCIDENTAL_DOUBLE_SHARP);
        assertEquals(3,note.writtenAccidental());assertEquals(2,ScoreNoteEvent.accidentalSemitones(note.writtenAccidental()));
        assertEquals(1,ScoreNoteEvent.accidentalSemitones(ScoreNoteEvent.ACCIDENTAL_SHARP));
    }
}
