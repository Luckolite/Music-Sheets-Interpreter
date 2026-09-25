// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
/** An accidental-shaped semantic fragment can belong to the note's own contour. */
public class OwnHeadFlatContourTest {
    private Object make(String name,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    private int read(int left)throws Exception {
        byte[] labels=new byte[420*260];int area=0;long sx=0,sy=0;
        for(int y=105;y<=129;y++)for(int x=left;x<=left+11;x++) {
            if(x<=left+1 || y>=117&&y<=125&&(x>=left+9||y==117||y==125)) {labels[y*420+x]=5;area++;sx+=x;sy+=y;}
        }
        Object glyph=make("Component",area,left,left+11,105,129,sx/(float)area,sy/(float)area);
        Object candidate=make("AccidentalCandidate",glyph,(byte)5);
        Object head=make("Component",100,272,288,118,130,280f,124f);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals("detectWrittenAccidental")&&m.getParameterCount()==6){
            m.setAccessible(true);return (int)m.invoke(null,labels,420,260,List.of(candidate),head,16f);
        }
        throw new AssertionError("Missing accidental reader");
    }
    @Test public void shortOwnContourCannotBecomeFlat()throws Exception{assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,read(265));}
    @Test public void separatedPrintedFlatSurvives()throws Exception{assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,read(252));}
}
