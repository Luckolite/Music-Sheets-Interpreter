// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
public class MusicalOcrEvidenceTest {
    @Test public void strongMusicFontDoesNotRequireGenericOcr(){assertEquals("2/4",MusicalOcrEvidence.fontMeter("2/4",.89f,.92f,Set.of(),Set.of()));}
    @Test public void weakFontCannotReplaceAbsentOcr(){assertEquals("",MusicalOcrEvidence.fontMeter("4/4",.82f,.93f,Set.of(),Set.of()));}
    @Test public void conflictingNumeratorRemainsUnresolved(){assertEquals("",MusicalOcrEvidence.fontMeter("2/4",.89f,.92f,Set.of("3"),Set.of("4")));}
    @Test public void conflictingDenominatorRemainsUnresolved(){assertEquals("",MusicalOcrEvidence.fontMeter("2/4",.89f,.92f,Set.of(),Set.of("8")));}
    @Test public void partialAgreementMayCompleteFraction(){assertEquals("4/4",MusicalOcrEvidence.fontMeter("4/4",.91f,.93f,Set.of("4"),Set.of()));}
    @Test public void literalTrillAndPortAreNormalized(){assertEquals("tr",MusicalOcrEvidence.ornamentToken("Tr."));assertEquals("port",MusicalOcrEvidence.ornamentToken("Dort,"));assertEquals("port",MusicalOcrEvidence.ornamentToken("por"));}
    @Test public void ordinaryWordsAreNotTrills(){for(String s:new String[]{"triplet","portrait","portamento","forte","t","3"})assertEquals("",MusicalOcrEvidence.ornamentToken(s));}
}
