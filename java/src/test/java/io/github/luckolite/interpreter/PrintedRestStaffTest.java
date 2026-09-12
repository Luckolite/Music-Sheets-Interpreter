// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic count, rest bar and staff; no source score pixels. */
public class PrintedRestStaffTest {
    private List<MeasureNumberReconciler.NumberToken> detect(int lines,boolean semantic,boolean notes,boolean bar,boolean countHead) {
        int w=200,h=120;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int k=0;k<lines;k++)for(int x=50;x<=170;x++){int at=(60+k*10)*w+x;gray[at]=0;if(semantic)labels[at]=4;}
        if(bar)for(int y=72;y<=77;y++)for(int x=88;x<=132;x++){gray[y*w+x]=0;labels[y*w+x]=5;}
        for(int y=35;y<=54;y++)gray[y*w+116]=0;
        for(int offset=0;offset<=10;offset++)gray[(35+offset)*w+106+offset]=0;
        for(int x=106;x<=116;x++)gray[46*w+x]=0;
        if(countHead)for(int y=35;y<=54;y++)for(int x=106;x<=116;x++)if(gray[y*w+x]==0)labels[y*w+x]=2;
        if(notes)for(int y=86;y<=94;y++)for(int x=138;x<=146;x++)labels[y*w+x]=2;
        return MultiMeasureRestDetector.detect(labels,gray,w,h,List.of(new MeasureRegion(.25f,.85f,countHead?.35f:.45f,.9f)),List.of());
    }
    @Test public void printedStaffRestoresCountWhenModelMissesAllRules(){var rests=detect(5,false,false,true,false);assertEquals(1,rests.size());assertEquals(4,rests.get(0).value());}
    @Test public void existingSemanticStaffStillWorks(){assertEquals(1,detect(5,true,false,true,false).size());}
    @Test public void fourLinesCannotProveAStaff(){assertTrue(detect(4,false,false,true,false).isEmpty());}
    @Test public void heavyBarAloneCannotProveAStaff(){assertTrue(detect(0,false,false,true,false).isEmpty());}
    @Test public void countWithoutHeavyBarIsNotARest(){assertTrue(detect(5,false,false,false,false).isEmpty());}
    @Test public void realNotesKeepMeasureFromExpanding(){assertTrue(detect(5,false,true,true,false).isEmpty());}
    @Test public void countHeadIsExcludedOnlyAfterPrintedStaffProof(){assertEquals(1,detect(5,false,false,true,true).size());}
}
