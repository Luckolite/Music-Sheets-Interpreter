// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original header bars with small spine dropouts; independent note controls. */
public final class FadedHeaderCrossbarTest {
    private HeaderCrossbarOwnershipTest faded(boolean longGap,boolean clef,boolean following) {
        var f=new HeaderCrossbarOwnershipTest().page(clef,true,following,0);
        for(int cy:new int[]{92,108})for(int y=cy-3;y<=cy+3;y++)for(int x=88;x<=101;x++)
            f.ink(x,y,2,0);
        for(int y=99;y<=(longGap?106:102);y++)for(int x=93;x<=94;x++)f.ink(x,y,0,255);
        return f;
    }
    @Test public void shortSpineDropoutDoesNotCreateTwoNotes(){assertTrue(faded(false,true,true).removed());}
    @Test public void longGapDoesNotProveOneGlyph(){assertFalse(faded(true,true,true).removed());}
    @Test public void wideCrossbarsStillNeedIndependentClef(){assertFalse(faded(false,false,true).removed());}
    @Test public void wideCrossbarsStillNeedFollowingNote(){assertFalse(faded(false,true,false).removed());}
    @Test public void trueStemmedSmallChordIsPreserved(){var f=faded(false,true,true);for(int y=44;y<=108;y++)f.ink(101,y,1,0);assertFalse(f.removed());}
    @Test public void pixelsStayUnchanged(){var f=faded(false,true,true);var l=f.labels.clone();var g=f.gray.clone();f.normalized();assertArrayEquals(l,f.labels);assertArrayEquals(g,f.gray);}
}
