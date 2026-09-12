// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric glyphs with semantic spine endpoints removed. */
public class ClippedNaturalTest {
    private int decode(boolean natural,boolean clipped,boolean withGray) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        for(int y=117;y<=166;y++)for(int x=169;x<=181;x++) {
            int slope=(x-169)/3; boolean bars=(y+slope>=135&&y+slope<=139)||(y+slope>=154&&y+slope<=158); boolean ink=(x<=171&&y<=158)||(x>=179&&y>=131)||bars;
            if(!natural)ink=(x<=171||x>=179)||bars;
            if(ink){gray[y*w+x]=0;labels[y*w+x]=(byte)(clipped&&(y<131||y>158)?1:3);}
        }
        for(int y=136;y<=152;y++)for(int x=195;x<=215;x++)if(Math.pow((x-205)/10.,2)+Math.pow((y-144)/8.,2)<=1){labels[y*w+x]=2;gray[y*w+x]=0;}
        for(int y=100;y<=144;y++){labels[y*w+214]=1;gray[y*w+214]=0;}
        var notes=OmrScoreInterpreter.extract(labels,withGray?gray:null,w,h,List.of(new MeasureRegion(.05f,.95f,.2f,.85f)));
        assertEquals(1,notes.size());assertEquals(0,notes.get(0).staffStep());
        return notes.get(0).writtenAccidental();
    }
    @Test public void fullPrintedNaturalCorrectsClippedMask(){assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL,decode(true,true,true));}
    @Test public void completeNaturalRemainsNatural(){assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL,decode(true,false,true));}
    @Test public void clippedGenuineSharpRemainsSharp(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(false,true,true));}
    @Test public void completeGenuineSharpRemainsSharp(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(false,false,true));}
    @Test public void absentPixelsCannotProveNatural(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,false));}
}
