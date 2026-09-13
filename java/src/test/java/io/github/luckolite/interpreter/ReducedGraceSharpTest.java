// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original ellipses and a miniature sharp beside a dot-sized grace head. */
public class ReducedGraceSharpTest {
    private GracePrefixGeometryTest.Page page(boolean sharp,boolean bars) {
        var p=GracePrefixGeometryTest.possibleDot(false);
        if(sharp) {
            for(int y=133;y<=155;y++)for(int x:new int[]{102,103,108,109})p.pixel(x,y,3);
            if(bars)for(int y:new int[]{138,139,140,148,149,150})for(int x=99;x<=112;x++)p.pixel(x,y,3);
        }
        return p;
    }
    @Test public void miniatureSharpProtectsItsGraceHead(){assertNotNull(page(true,true).at(120));}
    @Test public void reducedAccidentalKeepsTheSharpPitch(){var n=page(true,true).at(120);assertNotNull(n);assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,n.writtenAccidental());assertEquals(4,n.staffStep());}
    @Test public void reducedHeadUsesGraceTiming(){var n=page(true,true).at(120);assertNotNull(n);assertTrue((n.articulations()&NoteOrnament.GRACE)!=0);}
    @Test public void ordinaryStemlessDotIsStillRemoved(){assertNull(page(false,false).at(120));}
    @Test public void disconnectedSpinesDoNotProveASharp(){assertNull(page(true,false).at(120));}
    @Test public void distantPrincipalDoesNotMakeAnOrnament(){var p=page(true,true);for(int y=70;y<=160;y++)for(int x=150;x<=180;x++){p.l[y*GracePrefixGeometryTest.W+x]=0;p.g[y*GracePrefixGeometryTest.W+x]=(byte)255;}p.main(250,128);assertNull(p.at(120));}
    @Test public void principalNeedsItsOwnStem(){var p=page(true,true);for(int y=80;y<121;y++){p.l[y*GracePrefixGeometryTest.W+175]=0;p.g[y*GracePrefixGeometryTest.W+175]=(byte)255;}assertNull(p.at(120));}
    @Test public void aSharpAtAnotherPitchDoesNotProtectTheDot(){var p=GracePrefixGeometryTest.possibleDot(false);for(int y=109;y<=131;y++)for(int x:new int[]{102,103,108,109})p.pixel(x,y,3);for(int y:new int[]{114,115,116,124,125,126})for(int x=99;x<=112;x++)p.pixel(x,y,3);assertNull(p.at(120));}
    @Test public void sourcePixelsArePreserved(){var p=page(true,true);var labels=p.l.clone();var gray=p.g.clone();p.notes();assertArrayEquals(labels,p.l);assertArrayEquals(gray,p.g);}
    @Test public void ordinarySizedHeadDoesNotBecomeAGrace(){var p=page(true,true);p.head(120,144,11,7,false);var n=p.at(120);assertNotNull(n);assertEquals(0,n.articulations()&NoteOrnament.GRACE);}
    @Test public void aSmallFlatDoesNotMasqueradeAsASharp(){var p=page(false,false);for(int y=133;y<=155;y++)for(int x=102;x<=104;x++)p.pixel(x,y,3);for(int y=145;y<=151;y++)for(int x=109;x<=111;x++)p.pixel(x,y,3);for(int y:new int[]{145,146,151,152})for(int x=103;x<=110;x++)p.pixel(x,y,3);var n=p.at(120);if(n!=null)assertNotEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,n.writtenAccidental());}
    @Test public void principalInAnotherBarDoesNotEstablishAGrace(){var p=page(true,true);var measures=java.util.List.of(new MeasureRegion(0,.21f,.05f,.95f),new MeasureRegion(.21f,1,.05f,.95f));var ns=OmrScoreInterpreter.analyze(p.l,p.g,GracePrefixGeometryTest.W,GracePrefixGeometryTest.H,measures).notes();assertTrue(ns.stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));}
}
