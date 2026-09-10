// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class StemSlashGeometryTest {
    private static final int W=320,H=240;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    private void ink(int x,int y,int label) {gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void oval(int cx,int cy,int rx,int ry) {
        for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)ink(x,y,2);
    }
    private void staffAndMainHead() {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++)ink(x,y,4);
        oval(160,122,10,6);
        for(int y=60;y<=122;y++)ink(170,y,1);
    }
    private List<ScoreNoteEvent> notes() {
        return OmrScoreInterpreter.analyze(labels,gray,W,H,
                List.of(new MeasureRegion(.05f,.95f,.05f,.90f))).notes();
    }
    @Test public void repeatedShortStrokesAcrossStemCannotBecomeAHead() {
        staffAndMainHead();
        for(int cy:new int[]{86,98,110})for(int x=160;x<=180;x++)
            for(int dy=-2;dy<=2;dy++)ink(x,cy+dy-Math.round((x-170)*.2f),5);
        // Semantic paint joins pieces of the middle stroke into a small head.
        for(int y=93;y<=102;y++)for(int x=167;x<=172;x++)labels[y*W+x]=2;
        assertEquals(1,notes().size());
    }
    @Test public void smallRealHeadOnOneSideOfSharedStemIsPreserved() {
        staffAndMainHead();oval(166,98,4,3);
        assertEquals(2,notes().size());
    }
}
