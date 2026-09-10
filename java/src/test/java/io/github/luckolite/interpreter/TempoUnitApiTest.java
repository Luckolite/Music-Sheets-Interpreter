// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

public class TempoUnitApiTest {
    @Test public void apiDoesNotPlayTheTempoBeatUnit(){
        var p=new TempoUnitNoteTest.Page();p.realNote();
        var score=SheetInterpreter.analyze(p.labels,p.gray,p.w,p.h);
        assertEquals(1,score.notes().size());assertEquals(4,score.notes().get(0).staffStep());
    }
}
