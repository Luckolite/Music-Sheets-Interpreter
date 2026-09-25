// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original connected italic dynamic strokes, never a score crop or font asset. */
public final class ConnectedItalicDynamicTest {
    private static final int W=240,H=200;
    private void rect(byte[] p,int l,int r,int t,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)p[y*W+x]=(byte)value;}
    private byte[] page(boolean connected,boolean stem) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)190);
        for(int y=101;y<=111;y++)for(int x=76;x<=84;x++)if(Math.pow((x-80)/4.,2)+Math.pow((y-106)/5.,2)<=1)p[y*W+x]=20;
        for(int y=87;y<=126;y++){int x=107-Math.round((y-87)*.48f);rect(p,x-2,x+2,y,y,20);}
        rect(p,103,113,86,91,20);rect(p,85,92,122,126,20);
        rect(p,connected?79:93,118,99,102,20);
        rect(p,103,118,109,112,20);
        for(int y=102;y<=109;y++){int x=117-(y-102)*2;rect(p,x-1,x+1,y,y,20);}
        if(stem)rect(p,84,85,65,106,20);
        return p;
    }
    private boolean word(byte[] p){return ConnectedItalicDynamic.matches(p,W,H,76,101,84,111,14);}
    @Test public void connectedTinyOvalBelongsToItalicDynamic(){assertTrue(word(page(true,false)));}
    @Test public void detachedNoteBesideDynamicIsPreserved(){assertFalse(word(page(false,false)));}
    @Test public void attachedNotationStemIsPreserved(){assertFalse(word(page(true,true)));}
    @Test public void ordinaryFullSizedHeadIsPreserved(){assertFalse(ConnectedItalicDynamic.matches(page(true,false),W,H,72,99,91,113,14));}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(true,false),before=p.clone();word(p);assertArrayEquals(before,p);}
}
