// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original signature drawings with a flat-shaped surviving semantic fragment. */
public class PrintedSignatureSharpTest {
    private static final int W=JoinedSignatureSharpTest.W;
    private void semanticFlat(JoinedSignatureSharpTest f,int top) {
        for(int y=top-22;y<=top+22;y++)for(int x=72;x<=87;x++)f.labels[y*W+x]=0;
        for(int y=top-20;y<=top+7;y++)for(int x=75;x<=77;x++)f.labels[y*W+x]=3;
        for(int y=top-5;y<=top+5;y++)for(int x=84;x<=86;x++)f.labels[y*W+x]=3;
        for(int cy:new int[]{top-5,top+4})for(int y=cy;y<=cy+2;y++)
            for(int x=75;x<=86;x++)f.labels[y*W+x]=3;
    }
    private void damagedSharp(JoinedSignatureSharpTest f,int top) {
        f.row(top,1,0,false);semanticFlat(f,top);
    }
    private void realFlat(JoinedSignatureSharpTest f,int top) {
        f.row(top,0,0,false);semanticFlat(f,top);
        for(int y=top-22;y<=top+22;y++)for(int x=72;x<=87;x++)
            if(f.labels[y*W+x]==3)f.gray[y*W+x]=0;
    }
    @Test public void damagedFirstSignatureRetainsItsPrintedSharp() {
        var f=new JoinedSignatureSharpTest();damagedSharp(f,100);assertEquals(List.of(1),f.keys());
    }
    @Test public void aRepeatedDamagedSharpDoesNotInventAFlatKey() {
        var f=new JoinedSignatureSharpTest();f.row(100,1,0,false);damagedSharp(f,310);
        assertEquals(List.of(1),f.keys());
    }
    @Test public void aRealChangeFromOneSharpToOneFlatSurvives() {
        var f=new JoinedSignatureSharpTest();f.row(100,1,0,false);realFlat(f,310);
        assertEquals(List.of(1,-1),f.keys());
    }
    @Test public void aRealFlatAtTheFirstHeaderSurvives() {
        var f=new JoinedSignatureSharpTest();realFlat(f,100);assertEquals(List.of(-1),f.keys());
    }
    @Test public void isolatedSpinesDoNotProveAPrintedSharp() {
        var f=new JoinedSignatureSharpTest();damagedSharp(f,100);
        for(int y=78;y<=122;y++)for(int x=72;x<=87;x++)f.gray[y*W+x]=(byte)255;
        for(int x:new int[]{75,76,83,84})for(int y=78;y<=122;y++)f.gray[y*W+x]=0;
        assertEquals(List.of(-1),f.keys());
    }
    @Test public void aSinglePrintedCrossbarDoesNotProveASharp() {
        var f=new JoinedSignatureSharpTest();damagedSharp(f,100);
        for(int y=104;y<=106;y++)for(int x=72;x<=87;x++)
            if(x!=75&&x!=76&&x!=83&&x!=84)f.gray[y*W+x]=(byte)255;
        assertEquals(List.of(-1),f.keys());
    }
    @Test public void intactSignatureRemainsUnchanged() {
        var f=new JoinedSignatureSharpTest();f.row(100,1,0,false);assertEquals(List.of(1),f.keys());
    }
    @Test public void reconstructionDoesNotModifyInputPixels() {
        var f=new JoinedSignatureSharpTest();damagedSharp(f,100);var labels=f.labels.clone();var gray=f.gray.clone();
        f.keys();assertArrayEquals(labels,f.labels);assertArrayEquals(gray,f.gray);
    }
}
