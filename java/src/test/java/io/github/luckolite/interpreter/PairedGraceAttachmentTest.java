// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original reduced-note pairs beside one full-size principal. */
public class PairedGraceAttachmentTest {
    private static final int W=300,H=240;
    private static List<ScoreNoteEvent> run(boolean trailing,boolean paired,int paper,float principalX,boolean fullPair)throws Exception {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)paper);
        for(int y=110;y<=150;y++)gray[y*W+105]=40;
        for(int y=112;y<=158;y++)gray[y*W+130]=40;
        for(int x=105;x<=130;x++){int y=110+Math.round((x-105)*2f/25);for(int b=0;b<(paired?2:1);b++)for(int dy=0;dy<5;dy++)gray[(y+b*11+dy)*W+x]=40;}
        var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];hc.setAccessible(true);
        var dc=Class.forName(OmrScoreInterpreter.class.getName()+"$DetectedNote").getDeclaredConstructors()[0];dc.setAccessible(true);
        var events=new ArrayList<ScoreNoteEvent>();var detected=new ArrayList<Object>();
        float[] xs=trailing?new float[]{principalX,100,125}:new float[]{100,125,principalX};
        for(float x:xs){boolean principal=x==principalX;float y=x==125?158:150;boolean full=principal||fullPair;int rx=full?11:5,ry=full?7:4;
            var head=hc.newInstance(full?260:65,(int)x-rx,(int)x+rx,(int)y-ry,(int)y+ry,x,y);
            var event=new ScoreNoteEvent(0,x/W,2,0,1,y/H,false,0,principal?0:2,2,principal?1:0);
            events.add(event);detected.add(dc.newInstance(event,head,16f));}
        var m=OmrScoreInterpreter.class.getDeclaredMethod("markPairedGraces",byte[].class,int.class,int.class,List.class,List.class);m.setAccessible(true);m.invoke(null,gray,W,H,detected,events);
        return events;
    }
    @Test public void confirmedPairCanReachPrincipalThreeSpacesAway()throws Exception {var r=run(false,true,220,180,false);assertTrue((r.get(0).articulations()&32768)!=0);assertTrue((r.get(1).articulations()&32768)!=0);assertEquals(0,r.get(2).articulations());}
    @Test public void shadedPaperCannotExtendTheStems()throws Exception {var r=run(false,true,155,180,false);assertTrue((r.get(0).articulations()&32768)!=0);}
    @Test public void terminalPairMayFollowThePrincipal()throws Exception {var r=run(true,true,220,50,false);assertEquals(0,r.get(0).articulations());assertTrue((r.get(1).articulations()&32768)!=0);assertTrue((r.get(2).articulations()&32768)!=0);}
    @Test public void singleBeamDoesNotProveThisPair()throws Exception {for(var n:run(false,false,220,180,false))assertEquals(0,n.articulations());}
    @Test public void fullSizePrintedNotesStayMetrical()throws Exception {for(var n:run(false,true,220,180,true))assertEquals(0,n.articulations());}
    @Test public void distantPrincipalCannotOwnThePair()throws Exception {for(var n:run(false,true,220,240,false))assertEquals(0,n.articulations());}
}
