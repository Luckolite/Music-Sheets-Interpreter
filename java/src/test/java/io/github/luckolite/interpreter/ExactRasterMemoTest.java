// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExactRasterMemoTest {
    @Test public void duplicatePixelsReuseButDimensionsAndEditsDoNot()throws Exception {
        var cache=new ExactRasterMemo<String>(8);
        int[] pixels={1,2,3,4};
        assertEquals("first",cache.read(2,2,pixels,()->"first"));
        assertEquals("first",cache.read(2,2,pixels.clone(),()->{throw new AssertionError("duplicate OCR");}));
        assertEquals("shape",cache.read(1,4,pixels,()->"shape"));
        pixels[0]=9;
        assertEquals("edited",cache.read(2,2,pixels,()->"edited"));
        assertEquals(1,cache.hits());
    }
    @Test public void failuresRetryAndEvictionDoesNotReturnAnotherRaster()throws Exception {
        var cache=new ExactRasterMemo<String>(1);
        try{cache.read(1,1,new int[]{1},()->{throw new java.io.IOException();});fail();}
        catch(java.io.IOException expected){}
        assertEquals("retry",cache.read(1,1,new int[]{1},()->"retry"));
        cache.read(1,1,new int[]{2},()->"second");
        assertEquals("fresh",cache.read(1,1,new int[]{1},()->"fresh"));
        assertEquals(0,cache.hits());
    }
}
