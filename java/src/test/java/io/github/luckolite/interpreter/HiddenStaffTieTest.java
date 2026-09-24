// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic two-endpoint arcs with a lower staff hidden at the break. */
public class HiddenStaffTieTest {
    private Class<?> nested(String name) {
        return Arrays.stream(OmrScoreInterpreter.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals(name)).findFirst().orElseThrow();
    }
    private ScoreNoteEvent event(int bar,int count,int index,int step,int clef,boolean tied) {
        return new ScoreNoteEvent(bar,.1f,step,index,count,bar==0?186f/480:396f/480,tied,0,0,2,1).withClef(clef);
    }
    private Object detected(ScoreNoteEvent event,int x,int y)throws Exception {
        var component=nested("Component").getDeclaredConstructors()[0];component.setAccessible(true);
        var head=component.newInstance(80,x-7,x+7,y-5,y+5,(float)x,(float)y);
        var note=nested("DetectedNote").getDeclaredConstructors()[0];note.setAccessible(true);
        return note.newInstance(event,head,16f);
    }
    private boolean tied(int oldCount,int newCount,int oldIndex,int step,int clef,int arcMode)throws Exception {
        var raster=new SystemBreakTieTest().setup(arcMode==6?1:arcMode,true);
        if(arcMode==6)raster.arc(562,750,186,-1,false);
        var source=List.of(detected(event(0,oldCount,oldIndex,0,30,false),arcMode==6?550:650,186),
                detected(event(1,newCount,0,step,clef,false),180,396));
        var mark=OmrScoreInterpreter.class.getDeclaredMethod("markTieContinuations",byte[].class,byte[].class,int.class,int.class,List.class);mark.setAccessible(true);
        var result=(List<?>)mark.invoke(null,raster.labels,raster.gray,800,480,source);
        var getter=nested("DetectedNote").getDeclaredMethod("event");getter.setAccessible(true);
        return ((ScoreNoteEvent)getter.invoke(result.get(1))).tiedFromPrevious();
    }
    @Test public void hiddenLowerStaffKeepsTopStaffTie()throws Exception {assertTrue(tied(2,1,0,0,30,0));}
    @Test public void returningLowerStaffKeepsTopStaffTie()throws Exception {assertTrue(tied(1,2,0,0,30,0));}
    @Test public void wideOutgoingSystemCurveKeepsTie()throws Exception {assertTrue(tied(2,1,0,0,30,6));}
    @Test public void missingEndpointCannotJoin()throws Exception {assertFalse(tied(2,1,0,0,30,1));}
    @Test public void differentPitchCannotJoin()throws Exception {assertFalse(tied(2,1,0,1,30,0));}
    @Test public void lowerStaffCannotBecomeTopStaff()throws Exception {assertFalse(tied(2,1,1,0,30,0));}
    @Test public void unknownClefCannotEstablishIdentity()throws Exception {assertFalse(tied(2,1,0,0,-1,0));}
    @Test public void pitchGuardKeepsVerifiedTieAcrossHiddenStaff() {
        var notes=List.of(event(0,2,0,0,30,false),event(1,1,0,0,30,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,2))).get(1).tiedFromPrevious());
    }
    @Test public void changedExplicitAccidentalStillRejectsTie() {
        var first=event(0,2,0,0,30,false);
        var second=new ScoreNoteEvent(1,.1f,0,0,1,.825f,true,0,0,1,1).withClef(30);
        assertFalse(ScoreTiePitchGuard.apply(List.of(first,second),List.of(new ScoreKeyChange(0,0))).get(1).tiedFromPrevious());
    }
}
