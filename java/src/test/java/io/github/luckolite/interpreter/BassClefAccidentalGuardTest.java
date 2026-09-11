// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class BassClefAccidentalGuardTest {
    private static final int W=300,H=200;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    private Object construct(String name,Object... args)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);return ctor.newInstance(args);
    }
    private boolean check(Object body,boolean classified)throws Exception {
        var staff=construct("Staff",80f,144f,16f);
        var types=classified?new Class<?>[]{body.getClass(),byte[].class,byte[].class,int.class,int.class,staff.getClass()}
                :new Class<?>[]{body.getClass(),byte[].class,int.class,int.class,staff.getClass()};
        var method=OmrScoreInterpreter.class.getDeclaredMethod("rawBassClef",types);method.setAccessible(true);
        return (boolean)(classified?method.invoke(null,body,labels,gray,W,H,staff):method.invoke(null,body,gray,W,H,staff));
    }
    private void rect(int x,int y,int w,int h,int label) {
        for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++){labels[yy*W+xx]=(byte)label;gray[yy*W+xx]=0;}
    }
    private void prepare() {
        Arrays.fill(gray,(byte)255);
        for(int i=0;i<5;i++)rect(20,80+i*16,260,1,4);
        // Rounded terminals of a neighboring numeral occupy the same rows as bass-clef dots.
        rect(126,86,5,5,5);rect(126,102,5,5,5);
    }
    @Test public void aSharpAndNeighboringNumeralCannotChangeTheClef()throws Exception {
        prepare();rect(104,82,3,48,3);rect(112,82,3,48,3);
        rect(100,94,19,3,3);rect(100,106,19,3,3);
        var body=construct("Component",366,100,118,82,129,109f,106f);
        assertTrue(check(body,false));assertFalse(check(body,true));
    }
    @Test public void anActualNarrowBassBodyStillUsesItsPrintedDotsAndTail()throws Exception {
        prepare();rect(100,82,18,26,3);rect(102,117,5,14,5);
        var body=construct("Component",150,100,117,82,107,109f,95f);
        assertTrue(check(body,true));
    }
    private Object flatBesideNumeral(int top)throws Exception {
        prepare();rect(100,82,3,48,3);rect(100,108,19,3,3);
        rect(116,108,3,18,3);rect(100,123,19,3,3);
        for(int y=82;y<top;y++)for(int x=100;x<103;x++)labels[y*W+x]=5;
        int area=0;long sx=0,sy=0;
        for(int y=top;y<=129;y++)for(int x=100;x<=118;x++)if(labels[y*W+x]==3){area++;sx+=x;sy+=y;}
        return construct("Component",area,100,118,top,129,sx/(float)area,sy/(float)area);
    }
    @Test public void aFlatAndNeighboringNumeralCannotChangeTheClef()throws Exception {
        var body=flatBesideNumeral(82);assertTrue(check(body,false));assertFalse(check(body,true));
    }
    @Test public void aFlatWithShorterClassifiedSpineStillPreservesTheClef()throws Exception {
        var body=flatBesideNumeral(88);assertTrue(check(body,false));assertFalse(check(body,true));
    }
}
