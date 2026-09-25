// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;import org.junit.Test;import static org.junit.Assert.*;
/** Original ellipse raster regressions for darker filled noteheads. */
public class DarkerGrayHeadRecoveryTest {
 @Test public void darkGraySingleHasStrongContrast(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,207,140,false);assertEquals(1,p.find().size());}
 @Test public void darkGraySingleOnDimmerPaper(){var p=new ShadedNoteheadRecoveryTest();p.fixture(true,201,140,false);assertEquals(1,p.find().size());}
 @Test public void lowContrastDarkGrayStillRejected(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,190,150,false);assertTrue(p.find().isEmpty());}
 @Test public void deeplyShadedPaperStillRejected(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,185,130,false);assertTrue(p.find().isEmpty());}
 @Test public void darkGrayRectangleStillRejected(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,201,140,true);assertTrue(p.find().isEmpty());}
 @Test public void semanticStemIsStillRequired(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,201,140,false);Arrays.fill(p.labels,(byte)0);assertTrue(p.find().isEmpty());}
 ShadedChordHeadRecoveryTest chord(int paper,int fill,boolean rectangle){var p=new ShadedChordHeadRecoveryTest();p.fixture(3,false,rectangle,paper);for(int i=0;i<p.gray.length;i++)if((p.gray[i]&255)==185)p.gray[i]=(byte)fill;return p;}
 @Test public void darkGrayChordHasIndependentLobes(){var p=chord(205,140,false);assertEquals(1,p.find().size());assertEquals(3,p.find().get(0).heads().size());}
 @Test public void lowContrastDarkGrayChordStillRejected(){assertTrue(chord(190,150,false).find().isEmpty());}
 @Test public void darkGrayChordRectangleStillRejected(){assertTrue(chord(201,140,true).find().isEmpty());}
 @Test public void mediumSingleSurvivesDarkerConnectedSmear(){
  var p=new ShadedNoteheadRecoveryTest();p.fixture(false,240,185,false);
  assertEquals(1,p.find().size());
  // A lower-tone bridge joins a large scan stain only in the optional darker pass.
  for(int y=86;y<=94;y++)for(int x=40;x<=81;x++)p.gray[y*ShadedNoteheadRecoveryTest.W+x]=(byte)140;
  for(int y=69;y<=111;y++)for(int x=30;x<=60;x++)p.gray[y*ShadedNoteheadRecoveryTest.W+x]=(byte)140;
  var preserved=p.find();assertEquals(1,preserved.size());
  assertEquals(91f,preserved.get(0).centerX(),.001f);
  assertEquals(90f,preserved.get(0).centerY(),.001f);
 }
 @Test public void mediumChordSurvivesDarkerConnectedSmear(){
  var p=chord(245,185,false);var expected=p.find();assertEquals(1,expected.size());
  for(int y=66;y<=74;y++)for(int x=40;x<=83;x++)p.gray[y*ShadedChordHeadRecoveryTest.W+x]=(byte)140;
  for(int y=49;y<=141;y++)for(int x=30;x<=60;x++)p.gray[y*ShadedChordHeadRecoveryTest.W+x]=(byte)140;
  assertEquals(expected,p.find());
 }
 @Test public void darkSingleSurvivesDecoderEntry(){
  var p=new ShadedNoteheadRecoveryTest();p.fixture(false,207,140,false);
  var notes=OmrScoreInterpreter.extract(p.labels,p.gray,ShadedNoteheadRecoveryTest.W,ShadedNoteheadRecoveryTest.H,
    java.util.List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
  assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
  assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
 }
 java.util.List<ScoreNoteEvent> chordWithNeighbor(boolean neighborHasStem){
  var p=chord(245,140,false);
  for(int y=58;y<=120;y++)for(int x=78;x<=102;x++)
   if((p.gray[y*ShadedChordHeadRecoveryTest.W+x]&255)<205&&p.labels[y*ShadedChordHeadRecoveryTest.W+x]==0)
    p.labels[y*ShadedChordHeadRecoveryTest.W+x]=2;
  for(int y=116;y<=124;y++)for(int x=114;x<=122;x++)if((x-118)*(x-118)+(y-120)*(y-120)<=16){
   p.gray[y*ShadedChordHeadRecoveryTest.W+x]=30;p.labels[y*ShadedChordHeadRecoveryTest.W+x]=2;
  }
  if(neighborHasStem)for(int y=83;y<=120;y++){
   p.gray[y*ShadedChordHeadRecoveryTest.W+122]=30;p.labels[y*ShadedChordHeadRecoveryTest.W+122]=1;
  }
  p.staff(50);
  return OmrScoreInterpreter.extract(p.labels,p.gray,ShadedChordHeadRecoveryTest.W,ShadedChordHeadRecoveryTest.H,
    java.util.List.of(new MeasureRegion(.05f,.95f,.15f,.75f)));
 }
 @Test public void recoveredChordKeepsSemanticAugmentationDotOwnership(){
  var notes=chordWithNeighbor(false);
  assertEquals(3,notes.size());
  assertTrue(notes.stream().allMatch(n->n.staffStep()==2||n.staffStep()==4||n.staffStep()==6));
 }
 @Test public void realSmallNeighborKeepsItsIndependentStem(){assertEquals(4,chordWithNeighbor(true).size());}
}
