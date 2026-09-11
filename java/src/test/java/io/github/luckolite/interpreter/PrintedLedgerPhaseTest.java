// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrintedLedgerPhaseTest {
    private static final int W=260,H=260;
    private static byte[] page(int ledger,float offset,boolean second,boolean wide,boolean oneSided) {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)245);
        for(int line=0;line<5;line++)for(int x=20;x<240;x++)gray[(96+line*16)*W+x]=(byte)160;
        for(int index=0;index<(second?2:1);index++) {
            int step=ledger+index*(ledger>0?-2:2),y=Math.round(160-step*8+offset);
            for(int x=wide?20:80;x<=(wide?240:120);x++)if(!oneSided||x<100)gray[y*W+x]=60;
        }
        return gray;
    }
    private static Object head(float y)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var c=type.getDeclaredConstructors()[0];c.setAccessible(true);
        return c.newInstance(220,90,110,Math.round(y)-7,Math.round(y)+7,100f,y);
    }
    private static float reference(byte[] gray,float y)throws Exception {
        Object head=head(y);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("printedLedgerBottom",byte[].class,int.class,int.class,head.getClass(),float.class,float.class);
        m.setAccessible(true);return (float)m.invoke(null,gray,W,H,head,160f,16f);
    }
    private static int analyze(int ledger,float y,float offset) {
        byte[] gray=page(ledger,offset,true,false,false),labels=new byte[W*H];
        for(int line=0;line<5;line++)for(int x=20;x<240;x++)labels[(96+line*16)*W+x]=4;
        for(int yy=Math.round(y)-7;yy<=Math.round(y)+7;yy++)for(int x=90;x<=110;x++)
            if((x-100)*(x-100)/100f+(yy-y)*(yy-y)/49f<=1){gray[yy*W+x]=0;labels[yy*W+x]=2;}
        int sx=ledger>0?90:110,stop=Math.round(y)+(ledger>0?40:-40);
        for(int yy=Math.min(Math.round(y),stop);yy<=Math.max(Math.round(y),stop);yy++){gray[yy*W+sx]=0;labels[yy*W+sx]=1;}
        var result=OmrScoreInterpreter.analyze(labels,gray,W,H,java.util.List.of(new MeasureRegion(.05f,.95f,.1f,.9f)));
        assertEquals(1,result.notes().size());return result.notes().get(0).staffStep();
    }
    @Test public void upperLedgerPairCorrectsAccumulatedStaffError(){assertEquals(13,analyze(12,60.4f,1));}
    @Test public void lowerLedgerPairCorrectsAccumulatedStaffError(){assertEquals(-4,analyze(-4,196.4f,1));}
    @Test public void negativeLedgerOffsetCanLowerThePitch()throws Exception{assertEquals(159,reference(page(12,-1,true,false,false),59.6f),.01);}
    @Test public void oneLineDoesNotCalibrateThePitch()throws Exception{assertEquals(160,reference(page(12,1,false,false,false),60.4f),.01);}
    @Test public void oneSidedInkDoesNotCalibrateThePitch()throws Exception{assertEquals(160,reference(page(12,1,true,false,true),60.4f),.01);}
    @Test public void longBeamsDoNotServeAsLedgerLines()throws Exception{assertEquals(160,reference(page(12,1,true,true,false),60.4f),.01);}
    @Test public void distantLinesCannotReplaceTheStaffPhase()throws Exception{assertEquals(160,reference(page(12,4,true,false,false),60.4f),.01);}
}
