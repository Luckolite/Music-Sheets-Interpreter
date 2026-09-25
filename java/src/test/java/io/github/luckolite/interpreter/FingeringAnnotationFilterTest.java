// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FingeringAnnotationFilterTest {
    static final int W=400,H=300;
    final byte[] gray=new byte[W*H];
    final List<MeasureRegion> measures=List.of(new MeasureRegion(0,1,0,1));
    final List<PlayingTechniqueDetector.Staff> staffs=List.of(new PlayingTechniqueDetector.Staff(100,164,16,0,1));
    public FingeringAnnotationFilterTest(){Arrays.fill(gray,(byte)255);}
    ScoreNoteEvent note(int step,int y){return new ScoreNoteEvent(0,.5f,step,0,1,y/(float)H,false,0,0,1,1);}
    PlayingTechniqueDetector.Word word(String text,int top,int bottom){return new PlayingTechniqueDetector.Word(text,180f/W,top/(float)H,220f/W,bottom/(float)H);}
    List<ScoreNoteEvent> apply(String text,int top,int bottom,ScoreNoteEvent n){return FingeringAnnotationFilter.apply(List.of(word(text,top,bottom)),staffs,measures,List.of(n),gray,W,H);}
    @Test public void aboveStaffFingeringGlyphsAreRemoved(){for(String text:List.of("L2","2","LR","3","H4"))assertTrue(text,apply(text,55,88,note(10,80)).isEmpty());}
    @Test public void topLineInkIsNotLedgerEvidence(){for(int x=168;x<234;x++)gray[100*W+x]=0;assertTrue(apply("3",66,104,note(8,100)).isEmpty());}
    @Test public void graceNoteIsPreserved(){assertEquals(1,apply("2",55,88,note(10,80).withArticulations(NoteOrnament.GRACE)).size());}
    @Test public void ordinaryStaffNoteIsPreserved(){assertEquals(1,apply("2",70,103,note(6,95)).size());}
    @Test public void belowStaffWordIsIgnored(){assertEquals(1,apply("2",110,143,note(10,130)).size());}
    @Test public void shortNumberIsInsufficient(){assertEquals(1,apply("2",75,85,note(10,80)).size());}
    @Test public void nonFingeringWordIsIgnored(){for(String text:List.of("12","8va","Allegro","R2","0"))assertEquals(text,1,apply(text,55,88,note(10,80)).size());}
    @Test public void realLedgerAcrossTextProtectsNote(){for(int x=165;x<236;x++)gray[80*W+x]=0;assertEquals(1,apply("2",55,88,note(10,80)).size());}
    @Test public void ledgerOnlyOnOneSideIsInsufficient(){for(int x=165;x<180;x++)gray[80*W+x]=0;assertTrue(apply("2",55,88,note(10,80)).isEmpty());}
    @Test public void absentWordsPreserveListIdentity(){var notes=List.of(note(10,80));assertSame(notes,FingeringAnnotationFilter.apply(List.of(),staffs,measures,notes,gray,W,H));}
    @Test public void sourcePixelsArePreserved(){var before=gray.clone();apply("2",55,88,note(10,80));assertArrayEquals(before,gray);}
}
