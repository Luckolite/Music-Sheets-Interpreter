// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class OcrTextTest {
    @Test public void preservesMusicalEvidenceWithoutNormalization() {
        var box=new OcrText.Box(3,7,19,28);
        var symbol=new OcrText.Symbol("0",box);
        var word=new OcrText.Element("0o mf 6/8",box,List.of(symbol));
        var line=new OcrText.Line("0o mf 6/8",box,List.of(word,word));
        var block=new OcrText.Block(line.text(),box,List.of(line));
        var text=new OcrText("0o mf 6/8\n",List.of(block));
        assertEquals("0o mf 6/8\n",text.getText());
        assertEquals(2,text.getTextBlocks().get(0).getLines().get(0).getElements().size());
        assertSame(symbol,word.getSymbols().get(0));
        assertEquals(new OcrText.Box(3,7,19,28),symbol.getBoundingBox());
    }
    @Test public void missingGeometryAndSymbolsStayMissing() {
        var word=new OcrText.Element("tr",null,List.of());
        assertNull(word.getBoundingBox());assertTrue(word.getSymbols().isEmpty());
    }
    @Test public void nestedListsCannotBeChangedAfterConstruction() {
        var symbols=new ArrayList<OcrText.Symbol>();
        symbols.add(new OcrText.Symbol("1",null));
        var word=new OcrText.Element("1",null,symbols);
        symbols.clear();assertEquals(1,word.getSymbols().size());
        assertThrows(UnsupportedOperationException.class,()->word.getSymbols().clear());
        var elements=new ArrayList<OcrText.Element>(List.of(word));
        var line=new OcrText.Line("1",null,elements);elements.clear();
        assertEquals(1,line.getElements().size());
        assertThrows(UnsupportedOperationException.class,()->line.getElements().clear());
        var lines=new ArrayList<OcrText.Line>(List.of(line));
        var block=new OcrText.Block("1",null,lines);lines.clear();
        assertEquals(1,block.getLines().size());
        var blocks=new ArrayList<OcrText.Block>(List.of(block));
        var text=new OcrText("1",blocks);blocks.clear();
        assertEquals(1,text.getTextBlocks().size());
        assertThrows(UnsupportedOperationException.class,()->text.getTextBlocks().clear());
    }
    @Test public void boundsRetainInputCoordinatesRatherThanClamping() {
        var box=new OcrText.Box(-2,4,8,15);
        assertEquals(10,box.width());assertEquals(11,box.height());
        assertEquals(3,box.exactCenterX(),0);assertEquals(9.5,box.exactCenterY(),0);
    }
}
