// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloping rules; matching pitch must be independently printed at both ends. */
public class LocalTieStaffAlignmentTest {
    static final int W=800,H=340;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    LocalTieStaffAlignmentTest page(int lines,boolean semantic) {
        Arrays.fill(gray,(byte)245);
        for(int x=20;x<780;x++)for(int line=0;line<lines;line++) {
            int y=Math.round(100+line*16+x*.04f);
            for(int dy=-1;dy<=1;dy++){gray[(y+dy)*W+x]=20;if(semantic)labels[(y+dy)*W+x]=4;}
        }
        return this;
    }
    boolean same(float ay,float by){return LocalTieStaffAlignment.same(labels,gray,W,H,
            250,ay,242,258,16,450,by,442,458,16,0);}
    @Test public void sameRuleMayRiseBeyondFlatHeadTolerance(){page(5,true);assertTrue(same(174,182));}
    @Test public void neighbouringSpaceDoesNotBecomeSamePitch(){page(5,true);assertFalse(same(174,174));}
    @Test public void adjacentRuleDoesNotBecomeSamePitch(){page(5,true);assertFalse(same(174,166));}
    @Test public void fourRulesCannotEstablishTheMissingLevel(){page(4,true);assertFalse(same(174,182));}
    @Test public void aSixthRuleMakesThePhaseAmbiguous(){page(6,true);assertFalse(same(174,182));}
    @Test public void unsupportedRawInkCannotInventAStaff(){page(5,false);assertFalse(same(174,182));}
    @Test public void invalidInputAbstains(){page(5,true);assertFalse(same(Float.NaN,182));assertFalse(LocalTieStaffAlignment.same(null,gray,W,H,250,174,242,258,16,450,182,442,458,16,0));}
    @Test public void pixelsAreUnchanged(){page(5,true);var l=labels.clone();var g=gray.clone();same(174,182);assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
    @Test public void candidateSearchUsesLocalRulesWithoutRelaxingFlatFallback()throws Exception {
        page(5,true);
        var root=OmrScoreInterpreter.class;
        var hc=Class.forName(root.getName()+"$Component");var dc=Class.forName(root.getName()+"$DetectedNote");
        var ch=hc.getDeclaredConstructors()[0];ch.setAccessible(true);
        var cd=dc.getDeclaredConstructors()[0];cd.setAccessible(true);
        var first=new ScoreNoteEvent(0,.7f,0,0,1,174f/H,false,0,0,2,2,1,0,0,30);
        var last=new ScoreNoteEvent(1,.2f,0,0,1,182f/H,false,0,0,2,2,1,0,0,30);
        var notes=List.of(cd.newInstance(first,ch.newInstance(160,242,258,169,179,250f,174f),16f),
                cd.newInstance(last,ch.newInstance(160,442,458,177,187,450f,182f),16f));
        var flat=root.getDeclaredMethod("previousSamePitch",List.class,int.class,int.class);flat.setAccessible(true);
        var local=root.getDeclaredMethod("previousSamePitch",List.class,int.class,int.class,byte[].class,byte[].class,int.class);local.setAccessible(true);
        assertEquals(-1,flat.invoke(null,notes,1,W));
        assertEquals(0,local.invoke(null,notes,1,W,labels,gray,H));
    }
}
