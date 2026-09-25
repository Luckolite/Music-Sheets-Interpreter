// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
public class DynamicGlyphEvidenceTest {
    @Test public void exactIndependentMezzoReadingCorroborates(){assertTrue(DynamicGlyphEvidence.corroborated("mf",.45f,.11f,"mf"));}
    @Test public void truncatedOcrCannotConfirmDifferentLevel(){assertFalse(DynamicGlyphEvidence.corroborated("mf",.45f,.11f,"m"));}
    @Test public void ambiguousShapeStillFails(){assertFalse(DynamicGlyphEvidence.corroborated("mp",.45f,.03f,"mp"));}
    @Test public void poorShapeStillFails(){assertFalse(DynamicGlyphEvidence.corroborated("mf",.25f,.15f,"mf"));}
    @Test public void arbitrarySingleLetterNotRelaxed(){assertFalse(DynamicGlyphEvidence.corroborated("m",.45f,.11f,"m"));}
    @Test public void detachedFDoesNotEraseMezzoPrefix(){assertTrue(DynamicGlyphEvidence.clippedCompound("f","mf",30,50,10,52));}
    @Test public void detachedPDoesNotErasePianissimo(){assertTrue(DynamicGlyphEvidence.clippedCompound("p","pp",30,50,10,52));}
    @Test public void detachedMDoesNotEraseForteSuffix(){assertTrue(DynamicGlyphEvidence.clippedCompound("m","mf",10,30,9,52));}
    @Test public void detachedLeadingFDoesNotEraseFortissimo(){assertTrue(DynamicGlyphEvidence.clippedCompound("f","ff",10,30,9,52));}
    @Test public void CompleteShapeMayCorrectOcr(){assertFalse(DynamicGlyphEvidence.clippedCompound("f","mf",10,50,9,52));}
    @Test public void DifferentMarkIsNotASuffix(){assertFalse(DynamicGlyphEvidence.clippedCompound("p","mf",30,50,10,52));}
    @Test public void FullCompoundCanReplaceTruncatedOcr(){assertFalse(DynamicGlyphEvidence.clippedCompound("ff","f",10,50,30,52));}
    @Test public void NearbyNonoverlappingWordDoesNotVeto(){assertFalse(DynamicGlyphEvidence.clippedCompound("f","mf",60,80,10,52));}
}
