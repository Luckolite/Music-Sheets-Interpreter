// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class OcrMultiDigitRestTest {
    private List<MeasureNumberReconciler.NumberToken> read(boolean note,boolean bar,boolean inside) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=100;y<=164;y+=16)for(int x=40;x<=360;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        if(bar)for(int y=128;y<=139;y++)for(int x=110;x<=290;x++){labels[y*w+x]=1;gray[y*w+x]=0;}
        int top=inside?108:65;
        for(int y=top;y<top+24;y++)for(int x=182;x<=212;x++)if(x<187||x>206){labels[y*w+x]=2;gray[y*w+x]=0;}
        if(note)for(int y=110;y<118;y++)for(int x=145;x<157;x++){labels[y*w+x]=2;gray[y*w+x]=0;}
        var token=new MeasureNumberReconciler.NumberToken(16,181f/w,(top-1f)/h,214f/w,(top+25f)/h);
        return MultiMeasureRestDetector.detect(labels,gray,w,h,List.of(new MeasureRegion(.2f,.8f,.35f,.85f)),List.of(token));
    }
    @Test public void ocrExplainsOnlyCountInkAboveHeavyRest(){var r=read(false,true,false);assertEquals(1,r.size());assertEquals(16,r.get(0).value());}
    @Test public void actualWrittenNoteStillVetoesRest(){assertTrue(read(true,true,false).isEmpty());}
    @Test public void missingHeavyBarStillVetoesRest(){assertTrue(read(false,false,false).isEmpty());}
    @Test public void countInsideStaffCannotExplainNoteheads(){assertTrue(read(false,true,true).isEmpty());}
}
