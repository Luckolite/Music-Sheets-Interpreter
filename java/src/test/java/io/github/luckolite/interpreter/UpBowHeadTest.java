// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ink-only bow marks and noteheads; no score scans. */
public class UpBowHeadTest {
    private static final int W=360,H=240;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    public UpBowHeadTest(){Arrays.fill(gray,(byte)255);}
    private void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void line(int x0,int y0,int x1,int y1,int radius,int label) {
        int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));
        for(int i=0;i<=steps;i++) {
            int x=x0+Math.round((x1-x0)*i/(float)steps),y=y0+Math.round((y1-y0)*i/(float)steps);
            for(int dx=-radius;dx<=radius;dx++)for(int dy=-radius;dy<=radius;dy++)
                if(dx*dx+dy*dy<=radius*radius)ink(x+dx,y+dy,label);
        }
    }
    private void bow(boolean slanted) {
        line(slanted?96:104,slanted?64:58,120,94,3,5);
        line(120,94,slanted?128:136,58,3,5);
        for(int y=85;y<=97;y++)for(int x=111;x<=129;x++)if(gray[y*W+x]==0)labels[y*W+x]=2;
    }
    private boolean detected(){return NoteArticulationDetector.upBowAtHead(gray,W,H,111,85,129,97,14);}
    private void note(int x,int y,boolean stem) {
        for(int yy=y-7;yy<=y+7;yy++)for(int xx=x-11;xx<=x+11;xx++)
            if(Math.pow((xx-x)/11.,2)+Math.pow((yy-y)/7.,2)<=1)ink(xx,yy,2);
        if(stem)line(x-11,y,x-11,y+42,0,1);
    }
    private List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.03f,.97f,.1f,.9f))).notes();}
    private void staff(){for(int y=100;y<=156;y+=14)for(int x=16;x<344;x++)ink(x,y,4);}
    @Test public void completePrintedBowIsRecognized(){bow(false);assertTrue(detected());}
    @Test public void slantedHandwrittenBowIsRecognized(){bow(true);assertTrue(detected());}
    @Test public void bowTipDoesNotAddPitchOrAccent(){
        staff();bow(false);note(120,120,true);var notes=notes();assertEquals(1,notes.size());
        assertEquals(0,notes.get(0).articulations()&NoteArticulation.MARCATO);
    }
    @Test public void slantedTipDoesNotAddPitch(){staff();bow(true);note(120,120,true);assertEquals(1,notes().size());}
    @Test public void roundNoteIsPreserved(){staff();note(120,91,false);note(120,120,true);assertFalse(detected());assertEquals(2,notes().size());}
    @Test public void aDistantNoteCannotOwnTheBow(){staff();bow(false);note(220,120,true);assertEquals(2,notes().size());}
    @Test public void aStemlessChordIsPreserved(){staff();note(120,91,false);note(120,120,false);assertEquals(2,notes().size());}
    @Test public void aFlatBottomMarkIsNotABow(){line(104,58,104,94,2,5);line(104,94,136,94,2,5);line(136,94,136,58,2,5);assertFalse(detected());}
    @Test public void aHollowOvalIsNotABow(){
        for(int y=58;y<=94;y++)for(int x=104;x<=136;x++) {
            double r=Math.pow((x-120)/16.,2)+Math.pow((y-76)/18.,2);
            if(r<=1&&r>=.6)ink(x,y,2);
        }
        assertFalse(detected());
    }
    @Test public void aSingleSlashIsNotABow(){line(104,58,120,94,3,2);assertFalse(detected());}
    @Test public void anUpwardCaretIsNotAnUpBow(){line(104,94,120,58,3,5);line(120,58,136,94,3,5);assertFalse(detected());}
    @Test public void aDiamondHeadIsNotABow(){line(104,76,120,58,2,2);line(120,58,136,76,2,2);line(136,76,120,94,2,2);line(120,94,104,76,2,2);assertFalse(detected());}
    @Test public void aConnectedStemPreventsBowRecovery(){bow(false);line(120,94,120,150,1,1);assertFalse(detected());}
    @Test public void masksAreReadOnly(){bow(true);byte[] before=gray.clone(),semantic=labels.clone();detected();assertArrayEquals(before,gray);assertArrayEquals(semantic,labels);}
    @Test public void rawInkIsRequired(){assertFalse(NoteArticulationDetector.upBowAtHead(null,W,H,111,85,129,97,14));}
}
