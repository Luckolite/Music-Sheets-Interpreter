// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original mixed quarter/eighth exercise with a proved beam between staves. */
public class CrossStaffOverlapClockTest {
 private List<ScoreNoteEvent> phrase(boolean bridge) {
  List<ScoreNoteEvent> notes=new ArrayList<>();
  float[] x={.08f,.20f,.37f,.50f,.63f,.63f,.75f,.88f};int[] staff={1,1,0,0,0,1,1,0};
  for(int i=0;i<x.length;i++) {
   boolean quarter=i==4||i==7;
   ScoreNoteEvent n=new ScoreNoteEvent(0,x[i],i%5,staff[i],2,.4f+staff[i]*.2f,false,0,quarter?0:1,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,quarter?1:0);
   if(bridge&&(i==1||i==2))n=n.withCrossStaffBeam();notes.add(n);
  }
  return notes;
 }
 @Test public void overlapDoesNotDelayTheProvedCrossStaffRun(){var n=phrase(true);double[] beats={0,.5,1,1.5,2,2,2.5,3};for(int i=0;i<n.size();i++)assertEquals(beats[i],ScoreNoteTiming.beatInMeasure(n.get(i),n,4),.0001);}
 @Test public void shorterVoiceDoesNotShortenTheQuarter(){var n=phrase(true);assertEquals(1,ScoreNoteTiming.resolvedWrittenDurationBeats(n.get(4),n,4),.0001);assertEquals(1,ScoreNoteTiming.resolvedWrittenDurationBeats(n.get(7),n,4),.0001);}
 @Test public void visualSpacingAloneDoesNotCreateACrossStaffClock(){var n=phrase(false);assertNotEquals(1,ScoreNoteTiming.beatInMeasure(n.get(2),n,4),.0001);}
 @Test public void inputOrderDoesNotChangeSimultaneousVoices(){var n=phrase(true);var target=n.get(4);Collections.reverse(n);assertEquals(2,ScoreNoteTiming.beatInMeasure(target,n,4),.0001);}
}
