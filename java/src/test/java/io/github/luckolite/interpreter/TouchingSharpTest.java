// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original drawings of a sharp with its left spine assigned to the stem class. */
public class TouchingSharpTest {
    private int decode(boolean missingSpine,boolean upperBar,boolean lowerBar,int offset,boolean grayAvailable,boolean precedingNote) {
        int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){labels[y*w+x]=4;gray[y*w+x]=0;}
        if(precedingNote) {
            oval(labels,gray,w,162,136);
            for(int y=90;y<=136;y++){labels[y*w+172]=1;gray[y*w+172]=0;}
        }
        for(int y=120;y<=165;y++)for(int x=168;x<=187;x++) {
            boolean left=x>=172&&x<=174,right=x>=181&&x<=183;
            boolean bar=(upperBar&&y>=133&&y<=137)||(lowerBar&&y>=150&&y<=154);
            if(left||right||bar){int at=(y+offset)*w+x;gray[at]=0;labels[at]=(byte)(missingSpine&&left&&!bar?1:3);}
        }
        oval(labels,gray,w,205,144);
        for(int y=100;y<=144;y++){labels[y*w+214]=1;gray[y*w+214]=0;}
        var notes=OmrScoreInterpreter.extract(labels,grayAvailable?gray:null,w,h,List.of(new MeasureRegion(.05f,.95f,.2f,.85f)));
        var targets=notes.stream().filter(n->n.staffStep()==0).toList();assertEquals(1,targets.size());
        return targets.get(0).writtenAccidental();
    }
    private void oval(byte[] labels,byte[] gray,int w,int cx,int cy) {
        for(int y=cy-8;y<=cy+8;y++)for(int x=cx-10;x<=cx+10;x++)if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/8.,2)<=1){labels[y*w+x]=2;gray[y*w+x]=0;}
    }
    @Test public void stemLabelCannotEraseSharpSpine(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,true,0,true,false));}
    @Test public void touchingPreviousNoteStemStillAllowsSharp(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,true,0,true,true));}
    @Test public void completeSharpRemainsSharp(){assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(false,true,true,0,true,false));}
    @Test public void oneCrossbarIsInsufficient(){assertNotEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,false,0,true,false));}
    @Test public void isolatedSpinesAreInsufficient(){assertNotEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,false,false,0,true,false));}
    @Test public void accidentalAtAnotherPitchDoesNotAttach(){assertNotEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,true,-16,true,false));}
    @Test public void missingPixelsCannotRestoreSharp(){assertNotEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,decode(true,true,true,0,false,false));}
}
