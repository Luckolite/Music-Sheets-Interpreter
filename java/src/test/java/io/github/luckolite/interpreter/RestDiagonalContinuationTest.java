// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
public class RestDiagonalContinuationTest {
    private byte[] line(int top,int bottom){byte[] g=new byte[120*120];java.util.Arrays.fill(g,(byte)250);for(int y=top;y<=bottom;y++)for(int dx=0;dx<2;dx++)g[y*120+80-y/2+dx]=(byte)145;return g;}
    @Test public void longGlissandoContinuesOutsideTheApparentRest(){assertTrue(RestDiagonalContinuation.crosses(line(20,90),120,45,63,36,66,16));}
    @Test public void boundedRestTailDoesNotContinueOutside(){assertFalse(RestDiagonalContinuation.crosses(line(36,66),120,45,63,36,66,16));}
    @Test public void oneAdjacentMarkCannotSupplyBothEnds(){assertFalse(RestDiagonalContinuation.crosses(line(20,66),120,45,63,36,66,16));}
    @Test public void oneCrossingStaffRuleCannotSupplyContinuation(){var g=line(36,66);for(int x=0;x<120;x++){g[28*120+x]=0;g[74*120+x]=0;}assertFalse(RestDiagonalContinuation.crosses(g,120,45,63,36,66,16));}
    @Test public void sourceRemainsUnchanged(){var g=line(20,90);var before=g.clone();RestDiagonalContinuation.crosses(g,120,45,63,36,66,16);assertArrayEquals(before,g);}
}
