// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class TiltedRawStaffRecoveryTest {
    private static final int W=1600,H=1100,GAP=14;
    private static void ink(byte[] gray,byte[] labels,int x,int y,float slope,int label) {
        int row=Math.round(y+slope*(x-W*.5f));gray[row*W+x]=0;labels[row*W+x]=(byte)label;
    }
    private static OmrScoreInterpreter.Analysis page(float slope,int finalRules,boolean thick) {
        byte[] gray=new byte[W*H],labels=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int system=0;system<4;system++) {
            int top=120+system*240,bottom=top+4*GAP;
            for(int line=0;line<(system==3?finalRules:5);line++)for(int x=180;x<1450;x++)
                for(int dy=0;dy<(system==3&&thick?10:2);dy++)
                    ink(gray,labels,x,top+line*GAP+dy,slope,system==3?5:4);
            for(int x:new int[]{180,750,1449})for(int y=top;y<=bottom;y++)ink(gray,labels,x,y,slope,1);
            for(int i=0;i<3;i++) {
                int cx=450+i*350,cy=bottom-(i+1)*GAP;
                for(int y=cy-6;y<=cy+6;y++)for(int x=cx-9;x<=cx+9;x++)
                    if((x-cx)*(x-cx)/81f+(y-cy)*(y-cy)/36f<=1)ink(gray,labels,x,y,slope,2);
                for(int y=cy-3*GAP;y<=cy;y++)ink(gray,labels,cx+9,y,slope,1);
            }
            if(system==3) {
                for(int x=760;x<1449;x++)ink(gray,labels,x,top-55,slope,5);
                for(int y=top-55;y<top-20;y++)ink(gray,labels,760,y,slope,5);
            }
        }
        return OmrScoreInterpreter.analyze(labels,gray,W,H,OmrMeasurePostProcessor.process(labels,gray,W,H));
    }
    private static void check(float slope) {
        var result=page(slope,5,false);assertEquals(12,result.notes().size());
        var last=result.notes().stream().filter(n->n.measureIndex()>=6).toList();assertEquals(3,last.size());
        for(int i=0;i<3;i++)assertEquals((i+1)*2,last.get(i).staffStep());
    }
    @Test public void missingLabelledEndingIsRecoveredOnADownhillPage(){check(.012f);}
    @Test public void missingLabelledEndingIsRecoveredOnAnUphillPage(){check(-.012f);}
    @Test public void fourPrintedRulesDoNotInventAnEndingStaff(){assertEquals(9,page(.012f,4,false).notes().size());}
    @Test public void broadDarkBandsDoNotInventAnEndingStaff(){assertEquals(9,page(.012f,5,true).notes().size());}
}
