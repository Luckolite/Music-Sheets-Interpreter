// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Original generated engraving: a nearby head is joined to a bar only by a staff rule. */
public final class BarlineStaffCenterTest {
    @Test public void offsetSemanticRulesDoNotAttachANeighborToABar() throws Exception {
        assertFalse(new Page(1, 2, false).attached());
    }
    @Test public void oppositeOffsetAlsoUsesThePrintedRules() throws Exception {
        assertFalse(new Page(1, -2, false).attached());
    }
    @Test public void highResolutionRulesKeepTheSameSeparation() throws Exception {
        assertFalse(new Page(2, 2, false).attached());
    }
    @Test public void actualHeadOnTheColumnStillRejectsAStem() throws Exception {
        assertTrue(new Page(1, 2, true).attached());
    }
    @Test public void highResolutionAttachedHeadStillRejectsAStem() throws Exception {
        assertTrue(new Page(2, -2, true).attached());
    }
    @Test public void exactSemanticCentersKeepExistingSeparation() throws Exception {
        assertFalse(new Page(1, 0, false).attached());
    }
    @Test public void aShortLedgerCannotStandInForFiveStaffRules() throws Exception {
        Page p = new Page(1, 2, false);
        for (int y : new int[]{60, 88, 102, 116})
            for (int x=270;x<=330;x++) p.gray[y*p.width+x]=(byte)255;
        assertTrue(p.attached());
    }
    @Test public void oneSidedInkCannotMoveAStaffRule() throws Exception {
        Page p = new Page(1, 2, false);
        for(int x=306;x<=328;x++)p.gray[74*p.width+x]=(byte)255;
        assertTrue(p.attached());
    }
    @Test public void recoversAnInternalBarEvenWhenOtherBarsAlreadySplitTheStaff() throws Exception {
        Page p = new Page(1, 2, false);
        List<?> boundaries = p.boundaries();
        assertEquals(4, boundaries.size());
        assertTrue(Math.abs(((Number)boundaries.get(2)).intValue()-300)<=2);
    }
    @Test public void fullBoundarySearchStillExcludesAnAttachedHead() throws Exception {
        assertEquals(3, new Page(1, 2, true).boundaries().size());
    }

    private static final class Page {
        final int scale,width,height,bar; final byte[] labels,gray; final int[] rows;
        Page(int scale,int offset,boolean attached) {
            this.scale=scale; width=500*scale;height=200*scale;bar=300*scale;
            labels=new byte[width*height];gray=new byte[width*height];Arrays.fill(gray,(byte)255);
            rows=new int[5];
            for(int line=0;line<5;line++) {
                int raw=(60+14*line)*scale;
                rows[line]=raw+(line<3?offset*scale:0);
                for(int x=50*scale;x<=450*scale;x++) {
                    labels[rows[line]*width+x]=OmrMeasurePostProcessor.STAFF;
                    gray[raw*width+x]=0;
                }
            }
            for(int column:new int[]{150*scale,bar}) for(int y=58*scale;y<=118*scale;y++) {
                labels[y*width+column]=OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y*width+column]=0;
            }
            int cx=bar-(attached?3:9)*scale,cy=74*scale;
            for(int y=cy-3*scale;y<=cy+3*scale;y++)for(int x=cx-5*scale;x<=cx+5*scale;x++) {
                if(Math.pow((x-cx)/(5.0*scale),2)+Math.pow((y-cy)/(3.0*scale),2)>1)continue;
                labels[y*width+x]=OmrMeasurePostProcessor.NOTEHEAD;gray[y*width+x]=0;
            }
        }
        boolean attached() throws Exception {
            Method method=OmrMeasurePostProcessor.class.getDeclaredMethod("headTouchesColumn",
                    byte[].class,byte[].class,int.class,int.class,int.class,int.class,int.class,int[].class,float.class,float.class);
            method.setAccessible(true);
            return (Boolean)method.invoke(null,labels,gray,width,height,bar,40*scale,140*scale,rows,14f*scale,0f);
        }
        List<?> boundaries() throws Exception {
            Method method=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",
                    byte[].class,byte[].class,int.class,int.class,int[].class,float.class,int.class,int.class,float.class);
            method.setAccessible(true);
            return (List<?>)method.invoke(null,labels,gray,width,height,rows,14f*scale,50*scale,450*scale,0f);
        }
    }
}
