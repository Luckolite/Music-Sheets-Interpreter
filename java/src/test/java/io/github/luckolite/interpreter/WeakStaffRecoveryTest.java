// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeakStaffRecoveryTest {
    @Test public void establishedMeasureRowWithPartialStaffMaskKeepsItsNotes() {
        int w=1280,h=440;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int top:new int[]{60,240})for(int y=top;y<=top+48;y+=12)
            for(int x=40;x<1240;x++) {
                gray[y*w+x]=(byte)200;
                if(top==60||x>=90&&x<145)labels[y*w+x]=4;
            }
        for(int cy:new int[]{96,276}) {
            for(int y=cy-5;y<=cy+5;y++)for(int x=92;x<=108;x++)
                if(Math.pow((x-100)/8d,2)+Math.pow((y-cy)/5d,2)<=1) {
                    labels[y*w+x]=2;gray[y*w+x]=0;
                }
            for(int y=cy-35;y<=cy;y++){labels[y*w+108]=1;gray[y*w+108]=0;}
        }
        var measures=List.of(new MeasureRegion(.03f,.97f,20f/h,150f/h),new MeasureRegion(.03f,.97f,200f/h,330f/h));
        var notes=OmrScoreInterpreter.analyze(labels,gray,w,h,measures).notes();
        assertEquals(2,notes.size());
        assertEquals(1,notes.stream().filter(n->n.measureIndex()==1).count());
    }
}
