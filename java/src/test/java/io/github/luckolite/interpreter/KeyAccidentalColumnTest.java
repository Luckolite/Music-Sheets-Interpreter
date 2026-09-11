// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyAccidentalColumnTest {
    private static final int W=400,H=240;
    private final byte[] labels=new byte[W*H];
    private void flat(int x,int top,int label) {
        for(int y=top;y<=top+20;y++)labels[y*W+x]=(byte)label;
        for(int y=top+10;y<=top+17;y++) {
            int reach=y<=top+13?y-top-9:top+18-y;
            for(int xx=x+1;xx<=x+Math.max(2,reach);xx++)labels[y*W+xx]=(byte)label;
        }
    }
    private void page(boolean extraColumn,int duplicateOffset) {
        for(int y=80;y<=120;y+=10)for(int x=20;x<380;x++)labels[y*W+x]=4;
        for(int y=80;y<=120;y++)labels[y*W+80]=1;
        flat(102,76,3);
        if(extraColumn)flat(118,83,3);
        if(duplicateOffset>=0)flat(102+duplicateOffset,106,5);
        for(int y=108;y<=112;y++)for(int x=187;x<=193;x++)labels[y*W+x]=2;
        for(int y=80;y<=110;y++)labels[y*W+193]=1;
    }
    private List<ScoreKeyChange> changes(boolean raw) {
        byte[] gray=new byte[labels.length];
        for(int i=0;i<labels.length;i++)gray[i]=labels[i]==0?(byte)255:0;
        return OmrScoreInterpreter.analyze(labels,raw?gray:null,W,H,
                List.of(new MeasureRegion(.20f,.90f,.25f,.55f))).keyChanges();
    }
    @Test public void stackedLocalAccidentalsAreNotATwoFlatKey() {
        page(false,0);assertTrue(changes(true).isEmpty());
    }
    @Test public void SlightlyOffsetFragmentsStillOccupyOneColumn() {
        page(false,3);assertTrue(changes(true).isEmpty());
    }
    @Test public void extraFragmentDoesNotInflateARealTwoFlatSignature() {
        page(true,0);assertEquals(List.of(new ScoreKeyChange(0,-2)),changes(true));
    }
    @Test public void separateTwoFlatSignatureRemains() {
        page(true,-1);assertEquals(List.of(new ScoreKeyChange(0,-2)),changes(true));
    }
    @Test public void columnCheckAlsoWorksWithoutRawImage() {
        page(false,0);assertTrue(changes(false).isEmpty());
    }
    @Test public void doesNotChangeInputLabels() {
        page(true,0);byte[] saved=labels.clone();changes(true);assertArrayEquals(saved,labels);
    }
}
