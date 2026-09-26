// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact returning shoulders extending under the notehead edges. */
public class OverlappingSystemTieTest {
    SystemBreakTieTest fixture(boolean above,boolean outgoing,boolean incoming,boolean straight){
        var p=new SystemBreakTieTest();Arrays.fill(p.gray,(byte)255);
        for(int top:new int[]{90,300})for(int line=0;line<5;line++)for(int x=40;x<780;x++)p.ink(x,top+line*16,4);
        p.head(650,186);p.head(180,396);int side=above?-1:1;
        if(outgoing)p.arc(649,685,186,side,straight);
        if(incoming)p.arc(146,182,396,side,straight);
        return p;
    }
    @Test public void underHeadShouldersBelowJoin(){fixture(false,true,true,false).expected(true);}
    @Test public void underHeadShouldersAboveJoin(){fixture(true,true,true,false).expected(true);}
    @Test public void outgoingShoulderAloneCannotJoin(){fixture(false,true,false,false).expected(false);}
    @Test public void incomingShoulderAloneCannotJoin(){fixture(false,false,true,false).expected(false);}
    @Test public void straightStrokesDoNotBecomeTies(){fixture(false,true,true,true).expected(false);}
    @Test public void sourceInkIsPreserved(){var p=fixture(false,true,true,false);byte[] g=p.gray.clone(),l=p.labels.clone();p.notes();assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);}
}
