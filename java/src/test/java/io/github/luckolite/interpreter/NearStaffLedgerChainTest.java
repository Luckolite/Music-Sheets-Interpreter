// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original neighboring staves and unrelated chord-ledger ink. */
public class NearStaffLedgerChainTest {
    @Test public void unrelatedLedgersDoNotStealNearbySpaceNote()throws Exception{
        int w=260,h=260;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y:new int[]{112,128,144})for(int x=86;x<=124;x++)gray[y*w+x]=0;
        for(int y=90;y<=140;y++)gray[y*w+94]=0;
        var ct=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];ct.setAccessible(true);
        Object head=ct.newInstance(200,94,116,83,97,105f,90f);
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff").getDeclaredConstructors()[0];st.setAccessible(true);
        Object upper=st.newInstance(0f,64f,16f),lower=st.newInstance(160f,224f,16f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("printedLedgerOwner",byte[].class,int.class,int.class,List.class,head.getClass());m.setAccessible(true);
        assertNull(m.invoke(null,gray,w,h,List.of(upper,lower),head));
    }
}
