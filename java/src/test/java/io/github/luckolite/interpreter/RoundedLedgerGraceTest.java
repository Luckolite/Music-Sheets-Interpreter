// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rounded grace ellipses, short stems, beams and ledger strokes. */
public class RoundedLedgerGraceTest {
    static final int W=480,H=360,G=14;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.025f,.975f,.05f,.92f));
    static class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];
        Page(boolean pair,boolean largePrincipal,boolean shortStem,boolean beams,boolean own,boolean bilateral) {
            Arrays.fill(gray,(byte)255);
            for(int y=160;y<=216;y+=G)for(int x=12;x<=467;x++){pixel(x,y,4);pixel(x,y+1,4);}
            if(own)rule(189,211,118);
            rule(bilateral?191:200,bilateral?209:218,132);
            if(pair){rule(211,233,118);rule(211,233,132);}
            rule(233,267,132);rule(233,267,146);
            head(200,118,7,6);if(pair)head(222,125,7,6);
            head(250,132,largePrincipal?11:7,largePrincipal?8:6);
            int length=shortStem?30:56;
            stem(207,118-length,118);if(pair)stem(229,125-length,125);
            if(beams)for(int x=207;x<=229;x++)for(int band:new int[]{0,6})for(int dy=0;dy<3;dy++)pixel(x,118-length+Math.round((x-207)*7f/22)+band+dy,1);
            stem(239,132,180);
            head(350,188,11,8);stem(361,140,188);
        }
        void pixel(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void rule(int left,int right,int y){for(int x=left;x<=right;x++)pixel(x,y,4);}
        void stem(int x,int top,int bottom){for(int y=top;y<=bottom;y++){pixel(x,y,1);pixel(x+1,y,1);}}
        void head(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)pixel(x,y,2);}
        List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M).notes();}
        ScoreNoteEvent at(int x){return notes().stream().filter(n->Math.abs((M.get(0).left()+n.positionInMeasure()*.95f)*W-x)<3).findFirst().orElse(null);}
    }
    static Page good(){return new Page(true,true,true,true,true,true);}
    @Test public void roundedFirstGraceKeepsItsShortInnerLedger(){var n=good().at(200);assertNotNull(n);assertEquals(14,n.staffStep());}
    @Test public void bothGracePitchesAreKept(){assertEquals(13,good().at(222).staffStep());}
    @Test public void principalPitchIsUnchanged(){assertEquals(12,good().at(250).staffStep());}
    @Test public void recoveredHeadJoinsTheGracePrefix(){assertTrue((good().at(200).articulations()&NoteOrnament.GRACE)!=0);}
    @Test public void loneRoundedHeadDoesNotGainThePrefixAllowance(){assertNull(new Page(false,true,true,true,true,true).at(200));}
    @Test public void sameSizeFollowingNotesDoNotEstablishAGracePrefix(){assertNull(new Page(true,false,true,true,true,true).at(200));}
    @Test public void longOrdinaryStemsDoNotEstablishAGracePrefix(){assertNull(new Page(true,true,false,true,true,true).at(200));}
    @Test public void unconnectedShortStemsDoNotEstablishAGracePrefix(){assertNull(new Page(true,true,true,false,true,true).at(200));}
    @Test public void headStillNeedsItsOwnLedger(){assertNull(new Page(true,true,true,true,false,true).at(200));}
    @Test public void shortInnerLedgerMustExtendBothSides(){assertNull(new Page(true,true,true,true,true,false).at(200));}
    @Test public void staffRuleCannotReplaceTheSharedBeam(){var p=new Page(true,true,true,false,true,true);p.stem(229,88,95);p.rule(180,260,88);assertNull(p.at(200));}
}
