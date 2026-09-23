// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** A tab/embedded-PDF P near notation cannot become a piano instruction. */
public class WordSourceDynamicsTest {
    @Test public void nativeTabTextDoesNotInventDynamics() {
        var page=new HeaderSymbolNormalizationTest.Page(false,true);
        page.eraseSign();page.note(210,112,10,7,false);
        var p=new SheetInterpreter.Word("P",.40f,.68f,.44f,.74f);
        var generic=new SheetInterpreter.Annotations(List.of(),List.of(),List.of(),
                List.of(p),List.of());
        var nativeTab=new SheetInterpreter.Annotations(List.of(),List.of(),List.of(),
                List.of(),List.of(),List.of(p));
        assertFalse(SheetInterpreter.analyze(page.labels,page.gray,page.w,page.h,generic)
                .dynamicChanges().isEmpty());
        assertTrue(SheetInterpreter.analyze(page.labels,page.gray,page.w,page.h,nativeTab)
                .dynamicChanges().isEmpty());
    }
}
