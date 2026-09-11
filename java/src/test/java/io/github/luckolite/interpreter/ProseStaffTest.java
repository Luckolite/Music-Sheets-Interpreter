// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProseStaffTest {
    private static byte[] white(int w,int h) { byte[] a=new byte[w*h];Arrays.fill(a,(byte)255);return a; }
    private static void paragraph(byte[] gray,int w,int top) {
        for(int line=0;line<5;line++)for(int x=60;x<540;x+=10) {
            int y=top+line*24;
            // Original block-letter outlines, with no fonts or third-party score pixels.
            for(int i=0;i<7;i++){gray[y*w+x+i]=0;gray[(y-7)*w+x+i]=0;}
            for(int j=1;j<7;j++){gray[(y-j)*w+x]=0;gray[(y-j)*w+x+6]=0;}
        }
    }
    @Test public void fiveTextBaselinesAreNotFiveStaffRules() {
        byte[] gray=white(600,320);paragraph(gray,600,90);
        assertTrue(RawStaffLineDetector.detect(gray,600,320).isEmpty());
    }
    @Test public void closingTextDoesNotAddAnExtraSilentMeasure() {
        int w=600,h=440;byte[] gray=white(w,h),labels=new byte[w*h];
        for(int row=40;row<=80;row+=10)for(int x=40;x<560;x++){gray[row*w+x]=0;labels[row*w+x]=4;}
        paragraph(gray,w,210);
        assertEquals(1,OmrMeasurePostProcessor.process(labels,gray,w,h).size());
    }
    @Test public void lightlyBrokenStaffStillHasEnoughContinuousRule() {
        int w=600,h=220;byte[] gray=white(w,h);
        for(int row=60;row<=100;row+=10)for(int x=40;x<560;x++)if(x%70>1)gray[row*w+x]=0;
        assertEquals(1,RawStaffLineDetector.detect(gray,w,h).size());
    }
    @Test public void denseNotesDoNotRequireAnUnbrokenPageWideStaff() {
        int w=600,h=220;byte[] gray=white(w,h);
        for(int row=60;row<=100;row+=10)for(int x=40;x<560;x++)gray[row*w+x]=0;
        for(int x=100;x<520;x+=75)for(int y=48;y<113;y++)for(int dx=0;dx<24;dx++)gray[y*w+x+dx]=0;
        assertEquals(1,RawStaffLineDetector.detect(gray,w,h).size());
    }
}
