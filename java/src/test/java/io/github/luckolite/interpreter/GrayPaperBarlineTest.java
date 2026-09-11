// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrayPaperBarlineTest {
    private boolean boundary(int paper,int ink,boolean complete,boolean draw)throws Exception {
        return boundary(paper,ink,complete,draw,0,false);
    }
    private boolean boundary(int paper,int ink,boolean complete,boolean draw,int offset,boolean brightTexture)throws Exception {
        int w=260,h=200;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)paper);
        int[] rows={80,96,112,128,144};
        for(int y:rows)for(int x=20;x<240;x++)gray[y*w+x]=(byte)ink;
        if(draw)for(int y=77;y<148;y++)for(int x=119;x<=121;x++)
            if(complete||y<106||y>110&&y<125||y>131)gray[y*w+x]=(byte)ink;
        if(brightTexture)for(int y=70;y<155;y++)for(int x=135;x<138;x++)gray[y*w+x]=(byte)230;
        for(int i=0;i<rows.length;i++)rows[i]+=offset;
        var method=OmrMeasurePostProcessor.class.getDeclaredMethod("rawBarlineSpansStaff",
                byte[].class,int.class,int.class,int.class,int[].class,float.class,float.class,float.class);
        method.setAccessible(true);return (boolean)method.invoke(null,gray,w,h,120,rows,16f,0f,0f);
    }
    @Test public void grayPaperDoesNotFillGapsInARest()throws Exception {
        assertFalse(boundary(200,50,false,true));
    }
    @Test public void aSolidBarStillCrossesGrayPaper()throws Exception {
        assertTrue(boundary(200,50,true,true));
    }
    @Test public void aFaintBarOnWhitePaperIsPreserved()throws Exception {
        assertTrue(boundary(255,190,true,true));
    }
    @Test public void aFaintBarOnGrayPaperIsPreserved()throws Exception {
        assertTrue(boundary(200,170,true,true));
    }
    @Test public void lowContrastInkOnGrayPaperStillFormsAContinuousBar()throws Exception {
        assertTrue(boundary(200,185,true,true));
    }
    @Test public void paperAndHorizontalRulesAloneAreNotABarline()throws Exception {
        assertFalse(boundary(200,50,false,false));
    }
    @Test public void brightPaperSpecklesDoNotTurnARestIntoABarline()throws Exception {
        assertFalse(boundary(200,50,false,true,0,true));
    }
    @Test public void localPrintedRulesCorrectAnOffsetStaffPrediction()throws Exception {
        assertTrue(boundary(200,50,true,true,16,false));
    }
    @Test public void anOffsetPredictionStillCannotAcceptARest()throws Exception {
        assertFalse(boundary(200,50,false,true,16,false));
    }
}
