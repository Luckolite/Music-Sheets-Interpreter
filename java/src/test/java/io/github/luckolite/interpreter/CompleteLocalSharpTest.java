// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
/** Local accidentals must use their complete printed shape, including mixed semantic pieces. */
public class CompleteLocalSharpTest {
    private SharpPitchAlignmentTest.Page fragment(boolean mixed) {
        var p=new SharpPitchAlignmentTest.Page(124,124);
        for(int y=102;y<=146;y++)for(int x=248;x<=263;x++)if(p.labels[y*420+x]==3)p.labels[y*420+x]=0;
        for(int y=104;y<=131;y++)for(int x=251;x<=253;x++)p.labels[y*420+x]=3;
        for(int y=119;y<=129;y++)for(int x=260;x<=262;x++)p.labels[y*420+x]=3;
        for(int cy:new int[]{119,128})for(int y=cy;y<=cy+2;y++)for(int x=251;x<=262;x++)p.labels[y*420+x]=3;
        if(mixed)for(int y=102;y<=146;y++)for(int x=248;x<=263;x++)if(p.gray[y*420+x]==0&&p.labels[y*420+x]==0)p.labels[y*420+x]=5;
        return p;
    }
    @Test public void fullPrintedSharpOverridesFlatShapedSemanticFragment(){var p=fragment(false);assertEquals(1,p.accidental(p.gray));}
    @Test public void mixedSemanticFragmentsStillDescribeOneSharp(){var p=fragment(true);assertEquals(1,p.accidental(p.gray));}
    @Test public void semanticFlatWithoutPrintedSharpRemainsFlat(){var p=fragment(false);for(int y=102;y<=146;y++)for(int x=248;x<=263;x++)p.gray[y*420+x]=p.labels[y*420+x]==3?(byte)0:(byte)255;assertEquals(-1,p.accidental(p.gray));}
    @Test public void missingRawInkCannotOverrideSemanticFlat(){var p=fragment(false);assertEquals(-1,p.accidental(null));}
    @Test public void sourcePixelsRemainUnchanged(){var p=fragment(true);var l=p.labels.clone();var g=p.gray.clone();p.accidental(p.gray);assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
