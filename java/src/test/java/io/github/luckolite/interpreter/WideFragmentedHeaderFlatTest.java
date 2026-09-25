// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original wide clef frame and interrupted semantic flat spine. */
public class WideFragmentedHeaderFlatTest {
    HeaderFlatHeadTest.Page page(boolean upper) {
        var p=new HeaderFlatHeadTest.Page(false);
        for(int y=98;y<=190;y++)for(int x=20;x<=68;x++)
            if(x<23||x>65||y<101||y>187)p.pixel(x,y,3);
        if(upper)for(int y=68;y<=111;y++)for(int x=42;x<=47;x++)p.pixel(x,y,3);
        p.flat(0,true);return p;
    }
    @Test public void wideLowerClefBodyJoinsItsThinUpperStem(){assertFalse(page(true).headAt(92));}
    @Test public void lowerFrameAloneDoesNotEstablishTheHeader(){assertTrue(page(false).headAt(92));}
    @Test public void interruptedSemanticFlatKeepsItsCompleteRawProof(){
        var p=page(true);
        for(int y=127;y<=142;y++)for(int x=82;x<=84;x++)p.labels[y*420+x]=0;
        assertFalse(p.headAt(92));
    }
    @Test public void realNearbyNoteSurvivesTheWiderClefJoin(){var p=page(true);assertTrue(p.headAt(280));}
}
