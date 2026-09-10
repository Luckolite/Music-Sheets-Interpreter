// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompactStaffGroupingTest {
    private static final int W=600,H=620;
    private static byte[][] page() {
        byte[] labels=new byte[W*H],gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int top:new int[]{80,160,240,320,400,480}) {
            for(int line=0;line<5;line++)for(int x=60;x<=540;x++) {
                labels[(top+line*8)*W+x]=4;gray[(top+line*8)*W+x]=0;
            }
            for(int x:new int[]{60,180,300,420,540})for(int y=top;y<=top+32;y++) {
                labels[y*W+x]=1;gray[y*W+x]=0;
            }
        }
        return new byte[][]{labels,gray};
    }
    @Test public void compactUnconnectedSoloRowsRemainSequential() {
        var page=page();var measures=OmrMeasurePostProcessor.process(page[0],page[1],W,H);
        assertEquals(24,measures.size());
        for(int row=0;row<6;row++) {
            if(row>0)assertTrue(measures.get(row*4).top()>measures.get((row-1)*4).top());
            for(int col=1;col<4;col++)assertTrue(measures.get(row*4+col).left()>measures.get(row*4+col-1).left());
        }
    }
    @Test public void sharedOpeningBarsGroupOnlyTheirConnectedPairs() {
        var page=page();for(int top:new int[]{80,240,400})for(int y=top;y<=top+112;y++)page[1][y*W+60]=0;
        var measures=OmrMeasurePostProcessor.process(page[0],page[1],W,H);
        assertEquals(12,measures.size());
        assertTrue(measures.get(4).top()>measures.get(0).top());
        assertTrue(measures.get(8).top()>measures.get(4).top());
    }
    @Test public void overlappingRawAndSemanticEstimatesRemainOneRow()throws Exception {
        var type=Class.forName(OmrMeasurePostProcessor.class.getName()+"$StaffRun");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var bars=java.util.List.of(60,180,300,420,540);
        var upper=ctor.newInstance(80,112,8f,60,540,bars,0f);
        var lower=ctor.newInstance(104,136,8f,60,540,bars,0f);
        var merge=OmrMeasurePostProcessor.class.getDeclaredMethod("mergeAlignedStaffs",
                java.util.List.class,byte[].class,int.class,int.class);
        merge.setAccessible(true);
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        var systems=(java.util.List<?>)merge.invoke(null,java.util.List.of(upper,lower),gray,W,H);
        assertEquals("Overlapping estimates must not duplicate the same row's measures",1,systems.size());
    }

}
