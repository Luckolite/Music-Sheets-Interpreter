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
        // OCR text without printed ink is now rejected for either source.
        assertTrue(SheetInterpreter.analyze(page.labels,page.gray,page.w,page.h,generic)
                .dynamicChanges().isEmpty());
        assertTrue(SheetInterpreter.analyze(page.labels,page.gray,page.w,page.h,nativeTab)
                .dynamicChanges().isEmpty());
    }
    @Test public void genericDynamicWithPrintedInkStillWorks() {
        var page=new HeaderSymbolNormalizationTest.Page(false,true);
        page.eraseSign();page.note(210,112,10,7,false);
        // An original compact p-like stroke, independently supplied as generic OCR.
        for(int y=170;y<185;y++)for(int x=193;x<196;x++)page.gray[y*page.w+x]=0;
        for(int y=170;y<179;y++)for(int x=196;x<205;x++)
            if(y<173||y>=177||x>=202)page.gray[y*page.w+x]=0;
        var p=new SheetInterpreter.Word("p",190f/page.w,168f/page.h,207f/page.w,187f/page.h);
        var generic=new SheetInterpreter.Annotations(List.of(),List.of(),List.of(),List.of(p),List.of());
        assertFalse(SheetInterpreter.analyze(page.labels,page.gray,page.w,page.h,generic)
                .dynamicChanges().isEmpty());
    }
}
