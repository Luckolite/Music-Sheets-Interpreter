// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original asymmetric sharp and natural drawings; neither is copied from a score. */
public class NaturalConnectorPairTest {
    private static final int W=100,H=120;
    private final byte[] ink=new byte[W*H];
    private void box(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)ink[yy*W+xx]=3;}
    private void sharp(boolean shortLowerLeft) {
        box(28,30,2,shortLowerLeft?34:43);
        box(36,shortLowerLeft?34:39,2,43);
        box(25,43,16,5);box(25,59,16,5);
    }
    private void natural(){box(28,30,2,34);box(36,43,2,31);box(28,43,10,4);box(28,59,10,5);}
    private Object candidate()throws Exception {
        int area=0,l=W,r=-1,t=H,b=-1;long sx=0,sy=0;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++)if(ink[y*W+x]==3){area++;l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);sx+=x;sy+=y;}
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var c=type.getDeclaredConstructors()[0];c.setAccessible(true);
        Object g=c.newInstance(area,l,r,t,b,sx/(float)area,sy/(float)area);
        var a=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate").getDeclaredConstructor(type,byte.class);a.setAccessible(true);return a.newInstance(g,(byte)3);
    }
    private boolean isNatural()throws Exception {
        Object a=candidate();var m=OmrScoreInterpreter.class.getDeclaredMethod("isNaturalGlyph",byte[].class,int.class,int.class,a.getClass(),float.class);m.setAccessible(true);return (boolean)m.invoke(null,ink,W,H,a,17.5f);
    }
    @Test public void shortUpperRightSharpExtensionDoesNotProveNatural()throws Exception {sharp(false);assertFalse(isNatural());}
    @Test public void shortLowerLeftSharpExtensionDoesNotProveNatural()throws Exception {sharp(true);assertFalse(isNatural());}
    @Test public void naturalWithBothJunctionsIsRetained()throws Exception {natural();assertTrue(isNatural());}
    @Test public void shiftingTheNaturalRetainsItsTwoJunctions()throws Exception {
        natural();byte[] old=ink.clone();java.util.Arrays.fill(ink,(byte)0);
        for(int y=0;y<H-7;y++)for(int x=0;x<W-9;x++)ink[(y+7)*W+x+9]=old[y*W+x];assertTrue(isNatural());
    }
}
