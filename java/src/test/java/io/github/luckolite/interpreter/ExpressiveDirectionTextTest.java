// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

public class ExpressiveDirectionTextTest {
    private ExpressiveDirectionText.Direction one(String text, Kind kind) {
        var values = ExpressiveDirectionText.parse(text);
        assertEquals(1, values.size());
        var value = values.get(0);
        assertEquals(kind, value.kind());
        assertEquals(text, value.printedText());
        return value;
    }

    @Test
    public void gradualAndImmediateSlowingStayDifferent() {
        one("rit.", Kind.RITARDANDO);
        one("Rallentando", Kind.RALLENTANDO);
        one("riten.", Kind.RITENUTO);
    }

    @Test
    public void strengthWorksBeforeAndAfterTheMark() {
        assertEquals(Strength.MOLTO, one("molto rall.", Kind.RALLENTANDO).strength());
        assertEquals(Strength.MOLTO, one("rall. molto", Kind.RALLENTANDO).strength());
        assertEquals(Strength.POCO, one("poco rit.", Kind.RITARDANDO).strength());
    }

    @Test
    public void restorationRetainsItsFasterQualifierAndRawAccents() {
        var value = one("a tempo (più presto)", Kind.A_TEMPO);
        assertTrue(value.qualifierText().contains("piu presto"));
        one("Tempo I", Kind.TEMPO_PRIMO);
        one("L’istesso tempo", Kind.SAME_TEMPO);
    }

    @Test
    public void compoundCrescendoAndSlowingShareFullPrintedEvidence() {
        String phrase = "crescendo e rall. a poco a poco";
        var values = ExpressiveDirectionText.parse(phrase);
        assertEquals(
                Set.of(Kind.CRESCENDO, Kind.RALLENTANDO),
                values.stream()
                        .map(ExpressiveDirectionText.Direction::kind)
                        .collect(java.util.stream.Collectors.toSet()));
        for (var value : values) {
            assertEquals(phrase, value.printedText());
            assertEquals(Strength.UNSPECIFIED, value.strength());
            assertTrue(value.qualifierText().contains("a poco a poco"));
        }
    }

    @Test
    public void attacksAreNotPersistentDynamicLevels() {
        one("sf", Kind.SFORZANDO);
        one("sfz", Kind.SFORZATO);
        one("sfp", Kind.SFORZANDO_PIANO);
    }

    @Test
    public void pedalReleaseIsNotAlsoPedalDown() {
        one("Ped.", Kind.PEDAL_DOWN);
        one("senza pedale", Kind.PEDAL_UP);
        one("senza ped.", Kind.PEDAL_UP);
        one("senza Ped", Kind.PEDAL_UP);
    }

    @Test
    public void progressionWithoutLeadingAIsNotAQuietStrengthModifier() {
        var value = one("poco a poco cresc.", Kind.CRESCENDO);
        assertEquals(Strength.UNSPECIFIED, value.strength());
        assertTrue(value.qualifierText().contains("poco a poco"));
    }

    @Test
    public void verifiedUnsupportedVocabularyRemainsDistinct() {
        one("stringendo sempre", Kind.UNRESOLVED_DIRECTION);
        one("Molto più vivo", Kind.UNRESOLVED_DIRECTION);
    }

    @Test
    public void ordinaryProseAndPrefixLookalikesAreNotMusicalEvents() {
        assertTrue(ExpressiveDirectionText.parse("The artist wrote a beautiful piece").isEmpty());
        assertTrue(ExpressiveDirectionText.parse("riddle saffron pedalboard").isEmpty());
        assertTrue(ExpressiveDirectionText.parse(null).isEmpty());
    }
}
