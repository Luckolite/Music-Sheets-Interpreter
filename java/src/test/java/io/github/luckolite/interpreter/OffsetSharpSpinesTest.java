// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rectangular glyphs distinguish offset sharp spines from natural junctions. */
public class OffsetSharpSpinesTest {
    private static final int W=100,H=110;
    private final byte[] ink=new byte[W*H];
    private void box(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)ink[yy*W+xx]=3;}
    private void sharp(int offset){box(28,30,2,43);box(36,30+offset,2,43);box(25,43,16,5);box(25,59,16,5);}
    private void natural(boolean shortTail){box(28,30,2,shortTail?35:34);box(36,43,2,shortTail?24:31);box(28,43,10,4);box(28,59,10,5);}
    private Object component()throws Exception {
        int area=0,l=W,r=-1,t=H,b=-1;long sx=0,sy=0;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++)if(ink[y*W+x]==3){area++;l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);sx+=x;sy+=y;}
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(area,l,r,t,b,sx/(float)area,sy/(float)area);
    }
    private Object candidate(Object c)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");var ctor=type.getDeclaredConstructor(c.getClass(),byte.class);ctor.setAccessible(true);return ctor.newInstance(c,(byte)3);
    }
    private boolean shape(String method)throws Exception {
        Object g=component(),a=candidate(g);var m=OmrScoreInterpreter.class.getDeclaredMethod(method,byte[].class,int.class,int.class,a.getClass(),float.class);m.setAccessible(true);return (boolean)m.invoke(null,ink,W,H,a,17.25f);
    }
    private int written()throws Exception {
        Object g=component(),a=candidate(g);var ctor=g.getClass().getDeclaredConstructors()[0];ctor.setAccessible(true);Object head=ctor.newInstance(180,52,68,47,59,60f,53f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("detectWrittenAccidental",byte[].class,int.class,int.class,List.class,g.getClass(),float.class);m.setAccessible(true);return (int)m.invoke(null,ink,W,H,List.of(a),head,17.25f);
    }
    @Test public void smallEndpointOffsetDoesNotTurnSharpIntoNatural()throws Exception {sharp(4);assertFalse(shape("isNaturalGlyph"));}
    @Test public void offsetSharpKeepsItsSharpClassification()throws Exception {sharp(4);assertTrue(shape("isSharpGlyph"));assertEquals(1,written());}
    @Test public void oppositeEndpointOffsetStillMeansSharp()throws Exception {sharp(-4);assertTrue(shape("isSharpGlyph"));assertFalse(shape("isNaturalGlyph"));}
    @Test public void equalSharpSpinesDoNotBecomeNatural()throws Exception {sharp(0);assertFalse(shape("isNaturalGlyph"));assertEquals(1,written());}
    @Test public void naturalRightSpineBeginsAtUpperConnector()throws Exception {natural(false);assertTrue(shape("isNaturalGlyph"));assertEquals(0,written());}
    @Test public void naturalWithShortLowerExtensionIsRetained()throws Exception {natural(true);assertTrue(shape("isNaturalGlyph"));}
    @Test public void oneConnectorCannotProveNatural()throws Exception {natural(false);for(int y=59;y<64;y++)for(int x=30;x<36;x++)ink[y*W+x]=0;assertFalse(shape("isNaturalGlyph"));}
    @Test public void classificationPreservesSourceMask()throws Exception {sharp(4);byte[] before=ink.clone();shape("isNaturalGlyph");assertArrayEquals(before,ink);}
}
