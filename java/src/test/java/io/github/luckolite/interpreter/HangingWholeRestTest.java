// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rectangles and staffs, never score-derived pixels. */
public class HangingWholeRestTest {
    @Test public void hangingRectangleIsAWholeRestNotAnEighth() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,104);
        var rests=p.detect(List.of());assertEquals(1,rests.size());
        assertEquals(4,rests.get(0).durationBeats(),0);
        assertEquals(109.5,rests.get(0).positionInMeasure()*400,.01);
    }
    @Test public void middleRuleRectangleRemainsAHalfRest() {
        var p=new HalfRestRecognitionTest.Page();p.half();
        assertEquals(2,p.detect(List.of()).get(0).durationBeats(),0);
    }
    @Test public void isolatedScanSpeckCannotExtendAWholeRestIntoAnotherSymbol() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,104);p.rect(104,123,104,123);
        var rests=p.detect(List.of());assertEquals(1,rests.size());
        assertEquals(4,rests.get(0).durationBeats(),0);
        assertTrue(rests.get(0).pageY()*240<105);
    }
    @Test public void oneRoundedBottomRowRetainsTheRectangularCore() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,103);p.rect(105,104,114,104);
        assertEquals(4,p.detect(List.of()).get(0).durationBeats(),0);
    }
    @Test public void aDisconnectedTailIsTooMuchInkToIgnore() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,104);p.rect(104,119,105,123);
        assertTrue(p.detect(List.of()).isEmpty());
    }
    @Test public void anOvalOrThinLineIsNotAWholeRest() {
        var p=new HalfRestRecognitionTest.Page();p.ellipse(110,101,9,4,false);
        assertTrue(p.detect(List.of()).isEmpty());
        p=new HalfRestRecognitionTest.Page();p.rect(100,98,119,99);
        assertTrue(p.detect(List.of()).isEmpty());
    }
    @Test public void detachedRectangleCannotHangInAnArbitrarySpace() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,86,119,92);
        assertTrue(p.detect(List.of()).isEmpty());
    }
    @Test public void connectedStemAndRealNoteOwnershipRemainProtected() {
        var p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,104);p.rect(119,85,120,144);
        assertTrue(p.detect(List.of()).isEmpty());
        p=new HalfRestRecognitionTest.Page();p.rect(100,97,119,104);
        var note=new ScoreNoteEvent(0,.274f,5,0,1,101f/240,false,0,1);
        assertTrue(p.detect(List.of(note)).isEmpty());
    }
    @Test public void loweringCandidatesBeyondPageEdgeIsSafeAndReadOnly() {
        var p=new HalfRestRecognitionTest.Page();var before=p.gray.clone();
        assertTrue(SixteenthRestDetector.detect(p.gray,400,240,HalfRestRecognitionTest.M,
                List.of(new SixteenthRestDetector.Staff(200,264,16,0,1)),List.of()).isEmpty());
        assertArrayEquals(before,p.gray);
    }
}
