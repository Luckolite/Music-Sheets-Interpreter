// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original component geometry checks local curved-staff clef framing. */
public class CurvedPrintedClefTest {
    static final int W=800,H=400;
    private int clef(float slope,boolean tracked,int glyphWidth,int glyphHeight,boolean after)throws Exception {
        var root=OmrScoreInterpreter.class;var hc=Class.forName(root.getName()+"$Component");
        var sc=Class.forName(root.getName()+"$Staff");var dc=Class.forName(root.getName()+"$DetectedNote");
        var ch=hc.getDeclaredConstructors()[0];ch.setAccessible(true);
        var cs=sc.getDeclaredConstructors()[0];cs.setAccessible(true);
        var cd=dc.getDeclaredConstructors()[0];cd.setAccessible(true);
        Object staff=cs.newInstance(140f,204f,16f);
        if(tracked){var field=sc.getDeclaredField("pitchTrack");field.setAccessible(true);field.set(staff,StaffPitchTrack.linear(W,204,16,slope));}
        int shift=Math.round(slope*(100-W*.5f));
        Object glyph=ch.newInstance(700,100-glyphWidth/2,100+glyphWidth/2,120+shift,120+shift+glyphHeight,100f,180f+shift);
        Object head=ch.newInstance(120,242,258,170,180,250f,175f);
        var event=new ScoreNoteEvent(0,.5f,3,0,1,175f/H,false,0,0);
        Object note=cd.newInstance(event,head,16f);
        if(after)glyph=ch.newInstance(700,300-glyphWidth/2,300+glyphWidth/2,120+shift,120+shift+glyphHeight,300f,180f+shift);
        byte[] gray=new byte[W*H],labels=new byte[W*H];Arrays.fill(gray,(byte)255);
        var m=root.getDeclaredMethod("applyPrintedClefs",List.class,List.class,List.class,byte[].class,byte[].class,int.class,int.class);m.setAccessible(true);
        var result=(List<?>)m.invoke(null,List.of(note),List.of(staff),List.of(glyph),labels,gray,W,H);
        var field=dc.getDeclaredField("event");field.setAccessible(true);
        return ((ScoreNoteEvent)field.get(result.get(0))).clefBottomDiatonic();
    }
    @Test public void upwardCurledHeaderUsesItsLocalRules()throws Exception {assertEquals(30,clef(.1f,true,30,115,false));}
    @Test public void downwardCurledHeaderUsesItsLocalRules()throws Exception {assertEquals(30,clef(-.1f,true,30,115,false));}
    @Test public void flatHeaderIsUnchanged()throws Exception {assertEquals(30,clef(0,false,30,115,false));}
    @Test public void noTrackCannotInventOffsetReference()throws Exception {assertEquals(-1,clef(.1f,false,30,115,false));}
    @Test public void narrowBracketStillCannotBeTreble()throws Exception {assertEquals(-1,clef(.1f,true,2,115,false));}
    @Test public void shortAccidentalStillCannotBeTreble()throws Exception {assertEquals(-1,clef(.1f,true,20,38,false));}
    @Test public void clefAfterTheNoteDoesNotApplyRetroactively()throws Exception {assertEquals(-1,clef(0,true,30,115,true));}
}
