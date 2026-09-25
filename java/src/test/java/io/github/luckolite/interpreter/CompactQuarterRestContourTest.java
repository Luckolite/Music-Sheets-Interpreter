// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import static org.junit.Assert.*;
import org.junit.Test;
public class CompactQuarterRestContourTest {
    @Test public void pairedAccidentalSpinesAreNotRestStroke(){
        byte[] g=new byte[80*90];java.util.Arrays.fill(g,(byte)240);
        for(int y=10;y<=60;y++)for(int x:new int[]{30,31,40,41})g[y*80+x]=0;
        assertTrue(CompactQuarterRestContour.parallelSpines(g,80,25,46,10,60,new boolean[90],0,20));
    }
    @Test public void aSingleDiagonalRestTailIsNotParallelSpines(){
        byte[] g=new byte[80*90];java.util.Arrays.fill(g,(byte)240);
        for(int y=10;y<=60;y++)for(int dx=0;dx<2;dx++)g[y*80+30+(60-y)/5+dx]=0;
        assertFalse(CompactQuarterRestContour.parallelSpines(g,80,25,46,10,60,new boolean[90],0,20));
    }
    @Test public void compactQuarterGlyphDoesNotHaveParallelSpines(){
        var p=new CompactQuarterRestTest.Page(16,true,true,false,false);
        assertFalse(CompactQuarterRestContour.parallelSpines(p.gray,600,150,175,100,165,new boolean[280],0,16));
    }
    @org.junit.Test public void compactHookMayRequireIndependentCompleteRestProof(){org.junit.Assert.assertTrue(CompactQuarterRestContour.compactTail(13,9,80,18));}
    @org.junit.Test public void ordinaryPrintedHeadCannotUseCompactHookException(){org.junit.Assert.assertFalse(CompactQuarterRestContour.compactTail(21,15,180,18));}
    private double[] shape(double... anchors) {
        double[] result=new double[61];
        for(int i=0;i<result.length;i++) {
            double position=i*(anchors.length-1.0)/(result.length-1);int a=(int)position;
            result[i]=a==anchors.length-1?anchors[a]:anchors[a]+(anchors[a+1]-anchors[a])*(position-a);
        }
        return result;
    }
    @Test public void compactEarlyReturningHookIsQuarterRest(){assertTrue(CompactQuarterRestContour.matches(shape(2,4,10,9,5,5,11,7,8,9,10),18));}
    @Test public void lowHookMayLeaveAShortClearlyReturningFoot(){assertTrue(CompactQuarterRestContour.matches(shape(2,5,10,11,5,6,10,8,7,5,9),18));}
    @Test public void scaleDoesNotAlterContour(){double[] r=shape(2,4,10,9,5,5,11,7,8,9,10);for(int i=0;i<r.length;i++)r[i]*=1.6;assertTrue(CompactQuarterRestContour.matches(r,28.8f));}
    @Test public void straightStemIsNotRest(){assertFalse(CompactQuarterRestContour.matches(shape(7,7,7,7,7,7,7,7,7,7,7),18));}
    @Test public void diagonalTailIsNotRest(){assertFalse(CompactQuarterRestContour.matches(shape(2,3,4,5,6,7,8,9,10,11,12),18));}
    @Test public void oneRoundBulbIsNotRest(){assertFalse(CompactQuarterRestContour.matches(shape(2,4,6,8,10,11,10,9,8,7,6),18));}
    @Test public void missingSecondZigzagIsNotRest(){assertFalse(CompactQuarterRestContour.matches(shape(2,4,10,9,5,5,5,5,6,8,10),18));}
    @Test public void missingHookIsNotRest(){assertFalse(CompactQuarterRestContour.matches(shape(2,4,10,9,5,5,7,8,9,10,11),18));}
    @Test public void invalidCoordinatesCannotMatch(){assertFalse(CompactQuarterRestContour.matches(new double[]{Double.NaN},18));}
    @Test public void greyPaperDoesNotSupplyTheRestStroke(){
        byte[] g=new byte[80*60];java.util.Arrays.fill(g,(byte)158);
        for(int y=10;y<=50;y++)g[y*80+40]=(byte)149;
        assertFalse(CompactQuarterRestContour.hasContrastedInk(g,80,35,45,10,50,new boolean[60],0,15));
    }
    @Test public void darkOutlineOnGreyPaperIsIndependentInk(){
        byte[] g=new byte[80*60];java.util.Arrays.fill(g,(byte)158);
        for(int y=10;y<=50;y++)g[y*80+40]=(byte)60;
        assertTrue(CompactQuarterRestContour.hasContrastedInk(g,80,35,45,10,50,new boolean[60],0,15));
    }
    @Test public void palePrintedTailOnWhitePaperStillHasIndependentContrast(){
        byte[] g=new byte[80*60];java.util.Arrays.fill(g,(byte)250);
        for(int y=10;y<=50;y++)g[y*80+40]=(byte)155;
        assertTrue(CompactQuarterRestContour.hasContrastedInk(g,80,35,45,10,50,new boolean[60],0,15));
    }
    @Test public void equallyPaleStrokeOnGreyPaperHasNoIndependentContrast(){
        byte[] g=new byte[80*60];java.util.Arrays.fill(g,(byte)180);
        for(int y=10;y<=50;y++)g[y*80+40]=(byte)155;
        assertFalse(CompactQuarterRestContour.hasContrastedInk(g,80,35,45,10,50,new boolean[60],0,15));
    }
    @Test public void fewStaffFragmentsCannotSupplyACompleteRest(){
        byte[] g=new byte[80*60];java.util.Arrays.fill(g,(byte)158);
        for(int y=10;y<=15;y++)g[y*80+40]=(byte)60;
        assertFalse(CompactQuarterRestContour.hasContrastedInk(g,80,35,45,10,50,new boolean[60],0,15));
    }
}
