// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original five-line staff and straight bar, drawn at independent paper/ink shades. */
public class FaintSemanticBarTest {
    private static final int W=400,H=240;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private void setup(int paper,int ink,boolean semantic,boolean broken){Arrays.fill(gray,(byte)paper);for(int y=80;y<=144;y+=16)for(int x=20;x<=380;x++){gray[y*W+x]=80;labels[y*W+x]=4;}for(int y=80;y<=144;y++){if(semantic)labels[y*W+200]=1;if(!broken||y<110||y>119)gray[y*W+200]=(byte)ink;}}
    @SuppressWarnings("unchecked") private List<Integer> boundaries()throws Exception{var m=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",byte[].class,byte[].class,int.class,int.class,int[].class,float.class,int.class,int.class,float.class);m.setAccessible(true);return (List<Integer>)m.invoke(null,labels,gray,W,H,new int[]{80,96,112,128,144},16f,20,380,0f);}
    @Test public void nearFullSemanticTraceRetainsAPalePrintedBar()throws Exception{setup(250,214,true,false);assertEquals(List.of(20,200,380),boundaries());}
    @Test public void paperTextureCannotSupplyBarInk()throws Exception{setup(220,214,true,false);assertEquals(List.of(20,380),boundaries());}
    @Test public void rawInkWithoutSemanticTraceDoesNotSplit()throws Exception{setup(250,214,false,false);assertEquals(List.of(20,380),boundaries());}
    @Test public void semanticTraceCannotFillAnUnprintedStaffSpace()throws Exception{setup(250,214,true,true);assertEquals(List.of(20,380),boundaries());}
    @Test public void aHeadStillOwnsItsFaintStem()throws Exception{setup(250,214,true,false);for(int y=131;y<=141;y++)for(int x=187;x<=200;x++)if((x-194)*(x-194)/49d+(y-136)*(y-136)/25d<=1){gray[y*W+x]=90;labels[y*W+x]=2;}assertEquals(List.of(20,380),boundaries());}
    @Test public void aDarkBarKeepsExistingBehavior()throws Exception{setup(250,90,true,false);assertEquals(List.of(20,200,380),boundaries());}
    @Test public void inputsRemainUnchanged()throws Exception{setup(250,214,true,false);var g=gray.clone();var l=labels.clone();boundaries();assertArrayEquals(g,gray);assertArrayEquals(l,labels);}
}
