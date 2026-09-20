// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TabTextSourceTest {
    @Test public void preservesCompoundTokensAndDefersNumericValidationToGeometry() {
        var tokens=TabTextSource.tokens("Verse 42 | 7b9r7 14/16 <12> X 0 fret 99");
        assertTrue(tokens.containsAll(List.of("14/16", "7b9r7", "<12>", "0", "X")));
        assertFalse(tokens.contains("Verse"));
        assertTrue(TabNotation.parse("99",0,20,0,0).isEmpty());
    }

    @Test public void overlappingSearchResultsPreferCompoundToken() {
        var compound = new TablatureDecoder.Word("14/16", .1f, .2f, .2f, .3f);
        var fragment = new TablatureDecoder.Word("14", .1f, .2f, .14f, .3f);
        assertEquals(List.of(compound), TabTextSource.disjoint(List.of(fragment, compound)));
    }

    @Test public void separateChordFretsRemainSeparate() {
        var high = new TablatureDecoder.Word("5", .2f, .2f, .23f, .24f);
        var low = new TablatureDecoder.Word("7", .2f, .3f, .23f, .34f);
        assertEquals(2, TabTextSource.disjoint(List.of(high, low)).size());
    }
}
