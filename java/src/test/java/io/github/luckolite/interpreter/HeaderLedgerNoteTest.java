// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original semantic staff, accidental and head shapes. */
public class HeaderLedgerNoteTest {
    private static final int W=600,H=600,GAP=12;
    private final byte[] labels=new byte[W*H];
    private void staff(int top) {
        for(int y=top;y<=top+4*GAP;y+=GAP)
            for(int x=60;x<=540;x++)labels[y*W+x]=4;
        for(int y=top;y<=top+4*GAP;y++)labels[y*W+540]=1;
        // A key/accidental label extends to the right of the first head.
        for(int y=top+GAP;y<=top+3*GAP;y++)
            for(int x=124;x<=127;x++)labels[y*W+x]=3;
    }
    private void head(int x,int y) {
        for(int yy=y-4;yy<=y+4;yy++)for(int xx=x-7;xx<=x+7;xx++)
            if((xx-x)*(xx-x)/49.0+(yy-y)*(yy-y)/16.0<=1)labels[yy*W+xx]=2;
    }
    private float left(int row) {
        return OmrMeasurePostProcessor.process(labels,W,H).get(row).left()*W;
    }
    @Test public void upperLedgerHeadStopsHeaderCropping() {
        staff(180);head(110,150);assertTrue(left(0)<103);
    }
    @Test public void lowerLedgerHeadStopsHeaderCropping() {
        staff(180);head(110,258);assertTrue(left(0)<103);
    }
    @Test public void veryHighViolinHeadUsesSupportedLedgerRange() {
        staff(180);head(110,108);assertTrue(left(0)<103);
    }
    @Test public void veryLowHeadUsesSupportedLedgerRange() {
        staff(180);head(110,300);assertTrue(left(0)<103);
    }
    @Test public void precedingSystemHeadCannotStopThisHeader() {
        staff(100);staff(240);head(90,169);
        assertTrue(left(0)<90);assertTrue(left(1)>127);
    }
    @Test public void followingSystemHeadCannotStopThisHeader() {
        staff(100);staff(240);head(90,220);
        assertTrue(left(0)>127);assertTrue(left(1)<90);
    }
    @Test public void withoutAHeadTheFullHeaderIsTrimmed() {
        staff(180);assertTrue(left(0)>127);
    }
}
