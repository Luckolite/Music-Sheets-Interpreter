// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class SelectedTempoUnitTest {
 @Test public void selectedEighthPulseRetainsTheExactOpeningAndRelativeChanges(){
  var printed=List.of(new ScoreTempoChange(0,0,81.5,.5),new ScoreTempoChange(8,0,60,.5));
  double selected=ScoreTempoSelection.selectedQuarterBpm(163,163,printed);
  assertEquals(81.5,selected,0);
  assertEquals(printed,ScoreTempoSelection.atOpeningTempo(selected,printed));
  assertEquals(120,ScoreTempoSelection.atOpeningTempo(163,printed).get(1).bpm(),0);
 }
 @Test public void meterDefaultRemainsAvailableWithoutAPrintedUnit(){
  assertEquals(213,ScoreTempoSelection.selectedQuarterBpm(142,213,List.of()),0);
 }
}
