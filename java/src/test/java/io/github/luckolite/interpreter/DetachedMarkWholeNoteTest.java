// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original hollow heads, real stems, and separate bow marks. */
public class DetachedMarkWholeNoteTest {
    static final class Page {
        final int w=400,h=260; final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(int direction,boolean stem,boolean hiddenJoin) {
            Arrays.fill(gray,(byte)255);
            for(int y=128;y<=192;y+=16)rect(20,y,360,1,4,true);
            if(stem) {
                int x=direction<0?161:138,top=direction<0?92:144;
                rect(x,top,2,53,1,true);
                if(hiddenJoin)for(int y=136;y<=155;y++)for(int xx=x;xx<x+2;xx++)labels[y*w+xx]=0;
            }
            for(int y=136;y<=152;y++)for(int x=138;x<=162;x++) {
                double d=(x-150)*(x-150)/144.0+(y-144)*(y-144)/64.0;
                if(d<=1){labels[y*w+x]=2;if(d>=.5)gray[y*w+x]=0;}
            }
            // Both up and down marks are detached by more than half a staff space.
            int top=direction<0?106:163;
            rect(140,top,20,3,1,true);rect(140,top,3,18,1,true);rect(157,top,3,18,1,true);
        }
        void rect(int x,int y,int rw,int rh,int label,boolean ink) {
            for(int yy=y;yy<y+rh;yy++)for(int xx=x;xx<x+rw;xx++){
                labels[yy*w+xx]=(byte)label;if(ink)gray[yy*w+xx]=0;
            }
        }
        ScoreNoteEvent note(){
            var notes=OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.25f,.9f)));
            assertEquals(1,notes.size());return notes.get(0);
        }
    }
    @Test public void detachedBowAboveWholeHeadCannotBecomeItsStem(){
        assertEquals(4,new Page(-1,false,false).note().unbeamedDurationBeats(),0);
    }
    @Test public void separateMarkBelowWholeHeadCannotBecomeItsStem(){
        assertEquals(4,new Page(1,false,false).note().unbeamedDurationBeats(),0);
    }
    @Test public void upwardHalfNoteKeepsItsAttachedStem(){
        assertEquals(2,new Page(-1,true,false).note().unbeamedDurationBeats(),0);
    }
    @Test public void downwardHalfNoteKeepsItsAttachedStem(){
        assertEquals(2,new Page(1,true,false).note().unbeamedDurationBeats(),0);
    }
    @Test public void rawStemBridgesASemanticGapAtTheHead(){
        assertEquals(2,new Page(1,true,true).note().unbeamedDurationBeats(),0);
    }
    @Test public void sourcePixelsAndPitchStayUnchanged(){
        Page p=new Page(-1,false,false);byte[] labels=p.labels.clone(),gray=p.gray.clone();
        assertEquals(6,p.note().staffStep());assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);
    }
}
