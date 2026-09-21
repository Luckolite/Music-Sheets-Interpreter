// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NativeOcrWireTest {
    @Test public void exactRasterRoundTripPreservesAlphaAndColor()throws Exception {
        int[] pixels={0,0xffffffff,0xff123456,0x80807060};var bytes=new ByteArrayOutputStream();
        NativeOcrWire.writeRaster(new DataOutputStream(bytes),pixels,2,2);
        var copy=NativeOcrWire.readRaster(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(2,copy.width());assertEquals(2,copy.height());assertArrayEquals(pixels,copy.pixels());
    }
    @Test public void evidenceRoundTripPreservesAllLevelsAndMissingBoxes()throws Exception {
        var box=new OcrText.Box(1,2,10,20);
        var text=new OcrText("𝄞 mf",List.of(new OcrText.Block("𝄞 mf",box,List.of(new OcrText.Line("mf",box,
                List.of(new OcrText.Element("mf",box,List.of(new OcrText.Symbol("m",null),new OcrText.Symbol("f",box)))))))));
        var bytes=new ByteArrayOutputStream();NativeOcrWire.writeText(new DataOutputStream(bytes),text);
        assertEquals(text,NativeOcrWire.readText(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
    @Test public void rejectsInvalidDimensionsAndLengths()throws Exception {
        assertThrows(IOException.class,()->NativeOcrWire.writeRaster(new DataOutputStream(new ByteArrayOutputStream()),new int[1],0,1));
        var bytes=new ByteArrayOutputStream();new DataOutputStream(bytes).writeInt(Integer.MAX_VALUE);
        assertThrows(IOException.class,()->NativeOcrWire.readRaster(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}
