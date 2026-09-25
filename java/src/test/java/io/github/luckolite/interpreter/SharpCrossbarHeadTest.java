// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sharp crossbars incorrectly labeled as two small heads. */
public final class SharpCrossbarHeadTest {
    static final int W=JoinedSignatureSharpTest.W,H=JoinedSignatureSharpTest.H;
    private JoinedSignatureSharpTest page(boolean second,boolean spines,boolean noteStem) {
        var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);
        if(spines)f.sharp(400,132);
        for(int cy:new int[]{127,137})for(int y=cy-1;y<=cy+1;y++)for(int x=391;x<=406;x++) {
            f.gray[y*W+x]=0;f.labels[y*W+x]=0;
            if(x>=394&&x<=405&&(cy==127||second))f.labels[y*W+x]=2;
        }
        if(noteStem)for(int y=80;y<=138;y++)f.gray[y*W+405]=0;
        return f;
    }
    private int rejected(JoinedSignatureSharpTest f)throws Exception {
        Class<?> c=OmrScoreInterpreter.class;
        Method staff=c.getDeclaredMethod("findStaffs",byte[].class,byte[].class,int.class,int.class,List.class);staff.setAccessible(true);
        Method component=c.getDeclaredMethod("findComponents",byte[].class,int.class,int.class,byte.class);component.setAccessible(true);
        Method reject=c.getDeclaredMethod("sharpCrossbarHeads",byte[].class,int.class,int.class,List.class,List.class);reject.setAccessible(true);
        var staffs=staff.invoke(null,f.labels,f.gray,W,H,f.measures);
        var heads=component.invoke(null,f.labels,W,H,(byte)2);
        return ((List<?>)reject.invoke(null,f.gray,W,H,heads,staffs)).size();
    }
    @Test public void pairedCrossbarsBelongToCompleteSharp()throws Exception{assertEquals(2,rejected(page(true,true,false)));}
    @Test public void oneSmallHeadIsInsufficient()throws Exception{assertEquals(0,rejected(page(false,true,false)));}
    @Test public void horizontalStrokesWithoutSpinesRemain()throws Exception{assertEquals(0,rejected(page(true,false,false)));}
    @Test public void independentLongNoteStemProtectsHeads()throws Exception{assertEquals(0,rejected(page(true,true,true)));}
    @Test public void ordinaryStemmedNoteRemains()throws Exception{var f=new JoinedSignatureSharpTest();f.row(100,0,0,false);assertEquals(0,rejected(f));}
    @Test public void pixelsAreNotChanged()throws Exception{var f=page(true,true,false);var g=f.gray.clone();var l=f.labels.clone();rejected(f);assertArrayEquals(g,f.gray);assertArrayEquals(l,f.labels);}
}
