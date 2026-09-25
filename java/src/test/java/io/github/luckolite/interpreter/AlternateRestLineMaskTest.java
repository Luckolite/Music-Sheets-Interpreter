// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rest and distant row-ink halo; the broad erasure must not win alone. */
public class AlternateRestLineMaskTest {
    HalfRestRecognitionTest.Page page(){
        var p=new HalfRestRecognitionTest.Page();p.half();
        p.rect(220,108,340,111);return p;
    }
    @Test public void distantLineHaloCannotEraseTheOnlyValidHalfRest(){
        var rests=page().detect(List.of());assertEquals(1,rests.size());assertEquals(2,rests.get(0).durationBeats(),0);
    }
    @Test public void equivalentMasksDoNotDuplicateARest(){
        var p=new HalfRestRecognitionTest.Page();p.half();assertEquals(1,p.detect(List.of()).size());
    }
    @Test public void aRealHeadStillOwnsItsColumnUnderBothMasks(){
        var n=new ScoreNoteEvent(0,.274f,5,0,1,107.5f/240,false,0,1);
        assertTrue(page().detect(List.of(n)).isEmpty());
    }
    @Test public void sourceIsPreservedForEveryMask(){var p=page();var before=p.gray.clone();p.detect(List.of());assertArrayEquals(before,p.gray);}
}
