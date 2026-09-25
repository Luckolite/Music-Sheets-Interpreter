// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-system geometry, compressed semantic seed, and two returning arcs. */
public class PrintedSystemTieFrameTest {
    static final int W=1000,H=460;
    final byte[] gray=new byte[W*H],labels=new byte[W*H];
    PrintedSystemTieFrameTest page(boolean outgoing,boolean incoming,boolean straight,boolean opposite) {
        Arrays.fill(gray,(byte)245);
        for(int top:new int[]{70,300})for(int i=0;i<5;i++)for(int x=30;x<970;x++)ink(x,top+i*10,4);
        if(outgoing)arc(732,949,130,1,straight);
        if(incoming)arc(152,192,360,opposite?-1:1,straight);
        return this;
    }
    void ink(int x,int y,int label){gray[y*W+x]=20;labels[y*W+x]=(byte)label;}
    void arc(int a,int b,int cy,int side,boolean straight){for(int x=a;x<=b;x++){float t=(x-a)/(float)(b-a);int y=Math.round(cy+side*(4+(straight?0:7*4*t*(1-t))));ink(x,y,5);ink(x,y+1,5);}}
    static Class<?> type(String name)throws Exception{return Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);}
    static Object make(String name,Object... args)throws Exception{var c=type(name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    List<?> run(boolean tracks,boolean samePitch,boolean badTrack,boolean legacy,boolean sustained)throws Exception {
        var first=new ScoreNoteEvent(0,.65f,-4,0,1,130f/H,false,0,0,2,sustained?4:1,1,0,0,30);
        var second=new ScoreNoteEvent(1,.15f,samePitch?-4:-3,0,1,360f/H,false,0,0,2,4,1,0,0,30)
                .withCrossStaffBeam().withLeadingRest(.5f).withCompactOpening().withOctaveShift(1);
        Object h1=make("Component",60,714,726,126,134,720f,130f);
        Object h2=make("Component",60,204,216,356,364,210f,360f);
        var source=List.of(make("DetectedNote",first,h1,legacy?10f:7.5f),make("DetectedNote",second,h2,10f));
        Object s1=make("Staff",legacy?70f:80f,110f,legacy?10f:7.5f),s2=make("Staff",300f,340f,10f);
        if(tracks){set(s1,"pitchTrack",StaffPitchTrack.linear(W,badTrack?100:110,10,0));set(s2,"pitchTrack",StaffPitchTrack.linear(W,340,10,0));}
        var m=OmrScoreInterpreter.class.getDeclaredMethod("markTieContinuations",byte[].class,byte[].class,int.class,int.class,List.class,List.class);m.setAccessible(true);
        return (List<?>)m.invoke(null,labels,gray,W,H,source,List.of(s1,s2));
    }
    boolean tied(boolean tracks,boolean samePitch,boolean badTrack,boolean legacy,boolean sustained)throws Exception{return ((ScoreNoteEvent)field(run(tracks,samePitch,badTrack,legacy,sustained).get(1),"event")).tiedFromPrevious();}
    @Test public void provedLocalScaleConnectsLongTwoEndedSustain()throws Exception{page(true,true,false,false);assertTrue(tied(true,true,false,false,true));}
    @Test public void semanticMismatchWithoutTrackStillAbstains()throws Exception{page(true,true,false,false);assertFalse(tied(false,true,false,false,true));}
    @Test public void trackMustAgreeWithWrittenPitch()throws Exception{page(true,true,false,false);assertFalse(tied(true,true,true,false,true));}
    @Test public void unlikePitchesCannotTie()throws Exception{page(true,true,false,false);assertFalse(tied(true,false,false,false,true));}
    @Test public void outgoingAloneDoesNotTie()throws Exception{page(true,false,false,false);assertFalse(tied(true,true,false,false,true));}
    @Test public void incomingAloneDoesNotTie()throws Exception{page(false,true,false,false);assertFalse(tied(true,true,false,false,true));}
    @Test public void oppositeSideCurvesDoNotTie()throws Exception{page(true,true,false,true);assertFalse(tied(true,true,false,false,true));}
    @Test public void straightRulesDoNotTie()throws Exception{page(true,true,true,false);assertFalse(tied(true,true,false,false,true));}
    @Test public void ordinaryScaleCanReadLongSustainedArc()throws Exception{page(true,true,false,false);assertTrue(tied(false,true,false,true,true));}
    @Test public void movingNoteDoesNotExpandSystemEndSearch()throws Exception{page(true,true,false,false);assertFalse(tied(true,true,false,false,false));}
    @Test public void emittedGeometryAndPixelsStayUnchanged()throws Exception{page(true,true,false,false);byte[] l=labels.clone(),g=gray.clone();var r=run(true,true,false,false,true);assertEquals(7.5f,(float)field(r.get(0),"staffGap"),0);assertEquals(10f,(float)field(r.get(1),"staffGap"),0);assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
    @Test public void copiedTiePreservesOtherEventMetadata()throws Exception{page(true,true,false,false);var e=(ScoreNoteEvent)field(run(true,true,false,false,true).get(1),"event");assertTrue(e.tiedFromPrevious());assertTrue(e.crossStaffBeam());assertEquals(.5f,e.leadingRestBeats(),0);assertTrue(e.compactOpening());assertEquals(1,e.octaveShift());}
}
