// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CurvedLedgerOwnerTest {
    @Test public void distantBeamBandsCannotStealHeadNearCurvedUpperStaff()throws Exception {
        int w=360,h=380;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        Class<?> c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component"),s=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var cc=c.getDeclaredConstructors()[0];cc.setAccessible(true);var sc=s.getDeclaredConstructors()[0];sc.setAccessible(true);
        Object head=cc.newInstance(160,170,186,175,185,178f,180f),upper=sc.newInstance(100f,156f,14f),lower=sc.newInstance(260f,316f,14f);
        var track=s.getDeclaredField("pitchTrack");track.setAccessible(true);
        track.set(upper,StaffPitchTrack.linear(w,172,14,0));track.set(lower,StaffPitchTrack.linear(w,332,14,0));
        // Short beam fragments coincide with the lower staff's old ledger rows.
        for(int y:new int[]{232,246})for(int x=164;x<=193;x++)gray[y*w+x]=0;
        for(int y=133;y<=180;y++)for(int x=185;x<=186;x++){gray[y*w+x]=0;labels[y*w+x]=1;}
        for(int y=175;y<=185;y++)for(int x=170;x<=186;x++){gray[y*w+x]=0;labels[y*w+x]=2;}
        var m=OmrScoreInterpreter.class.getDeclaredMethod("staffForHead",byte[].class,byte[].class,int.class,int.class,List.class,c);m.setAccessible(true);
        assertSame(upper,m.invoke(null,labels,gray,w,h,List.of(upper,lower),head));
    }
}
