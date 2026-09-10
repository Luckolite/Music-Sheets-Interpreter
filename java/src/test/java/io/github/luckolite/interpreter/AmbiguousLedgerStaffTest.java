// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ledger heads between nearby rows, with a first-ending bracket above. */
public class AmbiguousLedgerStaffTest {
    private static final int W=400,H=420;
    private static Object construct(String name,Object...args)throws Exception {
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];
        c.setAccessible(true);return c.newInstance(args);
    }
    private static Object call(String name,Object...args)throws Exception {
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length) {
            m.setAccessible(true);return m.invoke(null,args);
        }
        throw new NoSuchMethodException(name);
    }
    private static void set(Object object,String name,int value)throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(object,value);
    }
    private static final class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];
        final Object upper,lower,head;
        Page(boolean connected,boolean upward)throws Exception {
            Arrays.fill(gray,(byte)255);
            upper=construct("Staff",108f,180f,18f);lower=construct("Staff",284f,356f,18f);
            if(connected){set(upper,"count",2);set(lower,"count",2);set(lower,"index",1);}
            head=construct("Component",220,169,191,221,239,180f,230f);
            for(int top:new int[]{108,284})for(int line=0;line<5;line++)for(int x=20;x<380;x++)ink(x,top+line*18,(byte)4);
            for(int x=20;x<380;x++)ink(x,219,(byte)5);
            for(int y:new int[]{230,248,266})for(int x=166;x<=194;x++)ink(x,y,(byte)5);
            for(int y=221;y<=239;y++)for(int x=169;x<=191;x++)
                if((x-180)*(x-180)/121f+(y-230)*(y-230)/81f<=1)ink(x,y,(byte)2);
            if(upward)for(int y=162;y<222;y++)ink(190,y,(byte)1);
            else for(int y=239;y<300;y++)ink(169,y,(byte)1);
        }
        private void ink(int x,int y,byte label){labels[y*W+x]=label;gray[y*W+x]=0;}
    }
    @Test public void highSoloLedgerHeadBelongsToLowerRowDespiteEndingBracket()throws Exception {
        var p=new Page(false,false);
        Object owner=call("staffForHead",p.labels,p.gray,W,H,List.of(p.upper,p.lower),p.head);
        assertSame(p.lower,owner);
        float[] pitch=(float[])call("localStaffPitch",p.labels,p.gray,W,H,owner,p.head);
        assertEquals("E6, fourteen diatonic steps above treble E4",14,Math.round((pitch[0]-230)*2/pitch[1]));
    }
    @Test public void shortLedgerRulesRetainPixelRoundedMargins()throws Exception {
        var p=new Page(false,false);
        assertEquals(2,call("innerLedgerCount",p.gray,W,H,p.head,p.lower));
        assertEquals("The long ending bracket is not a ledger",0,call("innerLedgerCount",p.gray,W,H,p.head,p.upper));
    }
    @Test public void ledgerChainOutweighsOpposingStemBetweenConnectedParts()throws Exception {
        var p=new Page(true,true);
        assertSame(p.lower,call("staffForHead",p.labels,p.gray,W,H,List.of(p.upper,p.lower),p.head));
    }
    @Test public void noteInsideMiddleSoloStaffRetainsItsOwner()throws Exception {
        var p=new Page(false,false);
        Object middle=construct("Staff",202f,274f,18f);
        assertSame(middle,call("staffForHead",p.labels,p.gray,W,H,
                List.of(p.upper,middle,p.lower),p.head));
    }

    @Test public void smallStaffLedgerNeedsOnlyOneVisiblePixelBeyondHead()throws Exception {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        Object staff=construct("Staff",100f,120f,5f);
        Object head=construct("Component",24,177,183,82,88,180f,85f);
        for(int y:new int[]{90,95})for(int x=176;x<=184;x++)gray[y*W+x]=0;
        assertEquals(2,call("innerLedgerCount",gray,W,H,head,staff));
    }

}
