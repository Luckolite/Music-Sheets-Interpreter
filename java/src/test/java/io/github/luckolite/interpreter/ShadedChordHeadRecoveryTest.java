// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original unions of ellipses with a shared stem and a medium-grey engraved fill. */
public class ShadedChordHeadRecoveryTest {
    static final int W=220,H=250;byte[] gray=new byte[W*H],labels=new byte[W*H];
    void fixture(int heads,boolean up,boolean rectangle,int paper){
        Arrays.fill(gray,(byte)paper);boolean[] shape=new boolean[W*H];
        for(int y=50;y<=90+20*heads;y++)for(int x=78;x<=102;x++){
            boolean inside=rectangle&&y>=70&&y<=70+(heads-1)*20;
            for(int i=0;i<heads;i++)inside|=(x-90)*(x-90)/144f+(y-70-i*20)*(y-70-i*20)/100f<=1;
            inside|=x>=84&&x<=96&&y>=70&&y<=70+(heads-1)*20;
            if(inside)shape[y*W+x]=true;
        }
        for(int y=2;y<H-2;y++)for(int x=2;x<W-2;x++)if(shape[y*W+x]){
            boolean border=false;for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if(!shape[(y+dy)*W+x+dx])border=true;
            gray[y*W+x]=(byte)(border?30:185);
        }
        int sx=up?102:78;for(int y=up?22:70;y<=(up?70:70+(heads-1)*20+48);y++){gray[y*W+sx]=30;labels[y*W+sx]=1;}
    }
    List<ShadedChordHeadRecovery.Chord> find(){return ShadedChordHeadRecovery.find(labels,gray,W,H,20,35,185);}
    @Test public void twoLobesYieldTwoPitches(){fixture(2,false,false,245);var c=find();assertEquals(1,c.size());assertEquals(2,c.get(0).heads().size());assertEquals(70,c.get(0).heads().get(0).centerY(),2);assertEquals(90,c.get(0).heads().get(1).centerY(),2);}
    @Test public void fourLobesYieldFourPitches(){fixture(4,true,false,245);var c=find();assertEquals(1,c.size());assertEquals(4,c.get(0).heads().size());}
    @Test public void filledRectangleCannotBecomeChord(){fixture(3,false,true,245);assertTrue(find().isEmpty());}
    @Test public void singleHeadNotDuplicated(){fixture(1,false,false,245);assertTrue(find().isEmpty());}
    @Test public void needsSemanticSharedStem(){fixture(2,false,false,245);Arrays.fill(labels,(byte)0);assertTrue(find().isEmpty());}
    @Test public void shadedPaperCannotBecomeChord(){fixture(2,false,false,205);assertTrue(find().isEmpty());}
    @Test public void inputArraysRemainUnchanged(){fixture(2,false,false,245);var g=gray.clone();var l=labels.clone();find();assertArrayEquals(g,gray);assertArrayEquals(l,labels);}
    void staff(int top){for(int y=top;y<=top+80;y+=20)for(int x=15;x<W-15;x++)if((gray[y*W+x]&255)>220){gray[y*W+x]=30;labels[y*W+x]=4;}}
    @Test public void independentlyPrintedChordSurvivesFullInterpreter(){fixture(2,false,false,245);staff(50);var n=OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.05f,.95f,.15f,.75f)));assertEquals(2,n.size());assertEquals(Set.of(4,6),new HashSet<>(n.stream().map(ScoreNoteEvent::staffStep).toList()));}
    @Test public void fourPrintedLobesCannotBeDiscardedAsPercussionCrosses(){fixture(4,true,false,245);staff(50);var n=OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.05f,.95f,.15f,.75f)));assertEquals(4,n.size());}
    void rails(boolean leftSide,boolean rightSide){Arrays.fill(gray,(byte)245);for(int y:new int[]{110,130})for(int x=65;x<=115;x++)if(x<78&&leftSide||x>102&&rightSide)gray[y*W+x]=30;}
    boolean ledger(){return ShadedChordHeadRecovery.hasLedgerRails(gray,W,H,78,102,132,10,90,20);}
    @Test public void twoIndependentLedgerRailsCanSupportOccludedCounter(){rails(true,true);assertTrue(ledger());}
    @Test public void oneSidedRuleDoesNotEstablishLedger(){rails(true,false);assertFalse(ledger());}
    @Test public void wideDarkRectangleIsNotShortPrintedLedger(){rails(true,true);for(int y=102;y<=139;y++)for(int x=65;x<=115;x++)gray[y*W+x]=30;assertFalse(ledger());}
    @Test public void onlyOneLedgerDoesNotJustifyExtremePitch(){rails(true,true);for(int x=65;x<=115;x++)gray[130*W+x]=(byte)245;assertFalse(ledger());}
    @Test public void malformedLedgerInputIsRejected(){assertFalse(ShadedChordHeadRecovery.hasLedgerRails(new byte[3],W,H,78,102,132,10,90,20));}
}
