// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** A symbolic diamond at a long note stem must not split a printed bar. */
public final class SymbolDiamondBarlineTest {
    @Test public void diamondOwnedStemDoesNotBecomeAnExtraBar() throws Exception {
        Page page=new Page();page.diamond(300);
        assertEquals(List.of(50,150,450),page.boundaries());
    }

    @Test public void unownedLongStrokeRetainsItsExistingBoundary() throws Exception {
        assertEquals(List.of(50,150,300,450),new Page().boundaries());
    }

    @Test public void isolatedPrintedBarSurvivesNearbyDiamond() throws Exception {
        Page page=new Page();page.diamond(150);
        assertEquals(List.of(50,150,300,450),page.boundaries());
    }

    @Test public void symbolOnlyHeaderStrokeIsNotMistakenForANoteStem() throws Exception {
        Page page=new Page();
        for(int y=45;y<=125;y++)page.labels[y*page.width+300]=OmrMeasurePostProcessor.SYMBOL;
        page.diamond(300);
        assertEquals(List.of(50,150,300,450),page.boundaries());
    }

    @Test public void onePixelLowStaffEstimateKeepsAPrintedBar() throws Exception {
        int width=500,height=200,gap=21,bar=150;
        int[] rows={61,82,103,124,145};
        byte[] labels=new byte[width*height],gray=new byte[width*height];
        Arrays.fill(gray,(byte)255);
        for(int line=0;line<6;line++) {
            int printedY=57+line*gap;
            for(int x=50;x<=450;x++)gray[printedY*width+x]=0;
            for(int x=50;x<=450;x++)if(line<5)
                labels[rows[line]*width+x]=OmrMeasurePostProcessor.STAFF;
        }
        // An imperfect first printed rule makes the neighboring staff line
        // a competing alignment when the semantic rows are one pixel low.
        gray[57*width+173]=(byte)255;
        gray[57*width+174]=(byte)255;
        for(int y=57;y<=141;y++)for(int x=bar-1;x<=bar+1;x++)gray[y*width+x]=0;
        for(int y=rows[0];y<=rows[4];y++)labels[y*width+bar]=OmrMeasurePostProcessor.STEM_OR_REST;
        Method method=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",
                byte[].class,byte[].class,int.class,int.class,int[].class,float.class,
                int.class,int.class,float.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked") List<Integer> boundaries=(List<Integer>)method.invoke(null,
                labels,gray,width,height,rows,(float)gap,50,450,0f);
        assertEquals(List.of(50,bar,450),boundaries);
    }

    @Test public void nearbyHeadConnectedOnlyByStaffFringeDoesNotOwnBar() throws Exception {
        Page page=new Page();
        for(int x=138;x<=152;x++)page.gray[57*page.width+x]=0;
        for(int y=55;y<=57;y++)for(int x=138;x<=142;x++)
            page.labels[y*page.width+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(List.of(50,150,300,450),page.boundaries());
    }

    @Test public void headConnectedThroughStaffSpaceStillOwnsStroke() throws Exception {
        Page page=new Page();
        for(int x=138;x<=152;x++)page.gray[80*page.width+x]=0;
        for(int y=79;y<=82;y++)for(int x=138;x<=142;x++)
            page.labels[y*page.width+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(List.of(50,300,450),page.boundaries());
    }

    private static final class Page {
        final int width=500,height=180;
        final byte[] labels=new byte[width*height],gray=new byte[width*height];
        final int[] rows={60,74,88,102,116};
        Page() {
            Arrays.fill(gray,(byte)255);
            for(int row:rows)for(int x=50;x<=450;x++) {
                labels[row*width+x]=OmrMeasurePostProcessor.STAFF;
                gray[row*width+x]=0;
            }
            for(int y=60;y<=116;y++) {
                labels[y*width+150]=OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y*width+150]=0;
            }
            for(int y=45;y<=125;y++) {
                labels[y*width+300]=OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y*width+300]=0;
            }
        }
        void diamond(int stem) {
            int cx=stem-8,cy=110;
            for(int y=cy-9;y<=cy+9;y++)for(int x=cx-10;x<=cx+10;x++)
                if(Math.abs(x-cx)/10.0+Math.abs(y-cy)/9.0<=1) {
                    labels[y*width+x]=OmrMeasurePostProcessor.SYMBOL;
                    gray[y*width+x]=0;
                }
        }
        @SuppressWarnings("unchecked")
        List<Integer> boundaries() throws Exception {
            Method method=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",
                    byte[].class,byte[].class,int.class,int.class,int[].class,float.class,
                    int.class,int.class,float.class);
            method.setAccessible(true);
            return (List<Integer>)method.invoke(null,labels,gray,width,height,rows,14f,50,450,0f);
        }
    }
}
