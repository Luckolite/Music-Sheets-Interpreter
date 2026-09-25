// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;import static org.junit.Assert.*;
/** Original low-paper fixtures reuse the existing redistributable ellipse builders. */
public class ContrastedShadedHeadRecoveryTest {
 @Test public void singleHeadWithStrongContrastSurvivesModeratelyDimPaper(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,217,163,false);assertEquals(1,p.find().size());}
 @Test public void downstemHeadWithStrongContrastSurvivesDimPaper(){var p=new ShadedNoteheadRecoveryTest();p.fixture(true,210,155,false);assertEquals(1,p.find().size());}
 @Test public void faintPaperTextureStillCannotBecomeHead(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,217,185,false);assertTrue(p.find().isEmpty());}
 @Test public void darkPaperStillCannotBecomeHead(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,200,155,false);assertTrue(p.find().isEmpty());}
 @Test public void strongContrastRectangleStillRejected(){var p=new ShadedNoteheadRecoveryTest();p.fixture(false,217,163,true);assertTrue(p.find().isEmpty());}
 ShadedChordHeadRecoveryTest chord(boolean rectangle,int fill){var p=new ShadedChordHeadRecoveryTest();p.fixture(3,false,rectangle,217);for(int i=0;i<p.gray.length;i++)if((p.gray[i]&255)==185)p.gray[i]=(byte)fill;return p;}
 @Test public void independentChordLobesOnDimPaperAreRecovered(){var p=chord(false,163);assertEquals(1,p.find().size());assertEquals(3,p.find().get(0).heads().size());}
 @Test public void lowContrastChordBodyCannotBecomeNotes(){assertTrue(chord(false,185).find().isEmpty());}
 @Test public void rectangularHighContrastBodyCannotBecomeChord(){assertTrue(chord(true,163).find().isEmpty());}
}
