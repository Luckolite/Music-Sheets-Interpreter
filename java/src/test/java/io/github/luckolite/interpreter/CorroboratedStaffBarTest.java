// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original boundary lists and raster bars under a shifted semantic frame. */
public class CorroboratedStaffBarTest {
    @SuppressWarnings("unchecked") List<Integer> merge(List<Integer> original,List<Integer> raw,List<Integer> tracked,float gap)throws Exception {
        var m=OmrMeasurePostProcessor.class.getDeclaredMethod("corroboratedInnerBars",List.class,List.class,List.class,float.class);m.setAccessible(true);
        return (List<Integer>)m.invoke(null,original,raw,tracked,gap);
    }
    @Test public void matchingIndependentFramesRestoreOnlyMissingBar()throws Exception{assertEquals(List.of(20,251,700,980),merge(List.of(20,700,980),List.of(20,250,700,980),List.of(20,251,700,980),16));}
    @Test public void singleNewFrameCannotInventBoundary()throws Exception{assertEquals(List.of(20,700,980),merge(List.of(20,700,980),List.of(20,700,980),List.of(20,250,700,980),16));}
    @Test public void disagreeingPrintedFramesCannotInventBoundary()throws Exception{assertEquals(List.of(20,700,980),merge(List.of(20,700,980),List.of(20,270,700,980),List.of(20,250,700,980),16));}
    @Test public void existingInnerBoundariesRemainExact()throws Exception{assertEquals(List.of(20,250,701,980),merge(List.of(20,250,701,980),List.of(20,251,700,980),List.of(20,252,700,980),16));}
    @Test public void closeDoubleBarCannotCreateTinyMeasure()throws Exception{assertEquals(List.of(20,700,980),merge(List.of(20,700,980),List.of(20,720,980),List.of(20,720,980),16));}
    @Test public void outerEdgesCannotMove()throws Exception{assertEquals(List.of(20,700,980),merge(List.of(20,700,980),List.of(5,35,700,965,995),List.of(5,35,700,965,995),16));}
    @Test public void unresolvedOrInvalidFrameIsUnchanged()throws Exception{var a=List.of(20,700,980);assertSame(a,merge(a,List.of(20,980),a,16));assertSame(a,merge(a,a,a,Float.NaN));}
    @Test public void sourceListsAreNotMutated()throws Exception{var a=new ArrayList<>(List.of(20,700,980));merge(a,List.of(20,250,700,980),List.of(20,250,700,980),16);assertEquals(List.of(20,700,980),a);}
    @Test @SuppressWarnings("unchecked") public void rawColumnStillNeedsTwoCompletePrintedFrames()throws Exception {
        int w=800,h=240;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)245);
        for(int y=80;y<=144;y+=16)for(int x=20;x<=780;x++){gray[y*w+x]=30;labels[y*w+x]=4;}
        for(int x:new int[]{250,600})for(int y=x==250?80:70;y<=(x==250?144:180);y++){gray[y*w+x]=20;labels[y*w+x]=1;}
        var method=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",byte[].class,byte[].class,int.class,int.class,int[].class,float.class,int.class,int.class,float.class,StaffPitchTrack.class);method.setAccessible(true);
        var semantic=(List<Integer>)method.invoke(null,labels,gray,w,h,new int[]{104,120,136,152,168},16f,20,780,0f,null);
        var raw=(List<Integer>)method.invoke(null,labels,gray,w,h,new int[]{80,96,112,128,144},16f,20,780,0f,null);
        var tracked=(List<Integer>)method.invoke(null,labels,gray,w,h,new int[]{104,120,136,152,168},16f,20,780,0f,StaffPitchTrack.linear(w,144,16,0));
        assertFalse(semantic.toString(),semantic.stream().anyMatch(x->Math.abs(x-250)<4));
        var merged=merge(semantic,raw,tracked,16);
        assertTrue(merged.toString(),merged.stream().anyMatch(x->Math.abs(x-250)<4));
        assertTrue(merged.containsAll(semantic));
    }
}
