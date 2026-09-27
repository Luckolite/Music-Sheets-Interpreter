// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original thick diagonal crosses and negative geometric controls. */
public class HeavyDoubleSharpTest {
    byte[] cross(boolean split) {
        int w=18;byte[] a=new byte[w*w];
        for(int y=0;y<w;y++)for(int x=0;x<w;x++)
            if(Math.abs(x-y)<=4||Math.abs(x+y-17)<=4)a[y*w+x]=(byte)(split&&x<8?3:5);
        return a;
    }
    @Test public void heavyArmsKeepNarrowNotches(){assertTrue(DoubleSharpGlyph.matches(cross(false),18,18,0,0,17,17,(byte)5,15));}
    @Test public void mixedAccidentalLabelsAreOneGlyph(){assertTrue(DoubleSharpGlyph.matches(cross(true),18,18,0,0,17,17,(byte)0,15));}
    @Test public void backgroundIsNotUnionInk(){assertFalse(DoubleSharpGlyph.matches(new byte[324],18,18,0,0,17,17,(byte)0,15));}
    @Test public void filledSquareIsNotDoubleSharp(){byte[] p=new byte[324];java.util.Arrays.fill(p,(byte)5);assertFalse(DoubleSharpGlyph.matches(p,18,18,0,0,17,17,(byte)5,15));}
    @Test public void missingLowerArmsCannotPass(){byte[] p=cross(false);for(int y=12;y<18;y++)for(int x=0;x<18;x++)p[y*18+x]=0;assertFalse(DoubleSharpGlyph.matches(p,18,18,0,0,17,17,(byte)5,15));}
    @Test public void rawWaistSurvivesStemClassification(){byte[] g=new byte[324];var p=cross(false);for(int i=0;i<g.length;i++)g[i]=(byte)(p[i]==0?255:0);assertTrue(DoubleSharpGlyph.matchesRaw(g,18,18,0,0,17,17,15));}
    byte[] compact(boolean rule,boolean lower) {
        byte[] g=new byte[40*40];java.util.Arrays.fill(g,(byte)255);
        for(int y=0;y<9;y++)for(int x=0;x<9;x++)if(Math.abs(x-y)<=1||Math.abs(x+y-8)<=1)
            if(lower||y<6)g[(15+y)*40+15+x]=0;
        if(rule)for(int x=3;x<35;x++)for(int y=18;y<=20;y++)g[y*40+x]=0;
        return g;
    }
    @Test public void staffObscuredCompactWaistStillHasFourArms(){assertTrue(DoubleSharpGlyph.matchesRaw(compact(true,true),40,40,15,15,23,23,13));}
    @Test public void staffRuleCannotSupplyMissingLowerArms(){assertFalse(DoubleSharpGlyph.matchesRaw(compact(true,false),40,40,15,15,23,23,13));}
    @Test public void rawRecognitionPreservesInput(){var g=compact(true,true);var before=g.clone();DoubleSharpGlyph.matchesRaw(g,40,40,15,15,23,23,13);assertArrayEquals(before,g);}
    @Test public void croppedSpineCannotMasqueradeAsCompactCross(){var g=compact(true,true);for(int y=6;y<=15;y++)g[y*40+15]=0;assertFalse(DoubleSharpGlyph.matchesRaw(g,40,40,15,15,23,23,13));}
    @Test public void shadedPaperCannotSupplyCrossArms(){var g=compact(true,false);for(int i=0;i<g.length;i++)g[i]=(byte)(g[i]==0?65:175);assertFalse(DoubleSharpGlyph.matchesRaw(g,40,40,15,15,23,23,13));}
}
