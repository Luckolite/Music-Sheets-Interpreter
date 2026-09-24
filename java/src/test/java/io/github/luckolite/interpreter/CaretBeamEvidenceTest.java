// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural caret, distinct from parallel beam/flag strokes. */
public class CaretBeamEvidenceTest {
    private boolean matches(int kind)throws Exception {
        int w=100,h=100;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=30;y<=46;y++)for(int x=30;x<=70;x++) {
            int spread=Math.round((y-30)*.6f);
            boolean ink=kind==0?(Math.abs(x-(50-spread))<=1||Math.abs(x-(50+spread))<=1)
                    :kind==1?(y>=30&&y<=33||y>=43&&y<=46):x>=47&&x<=53;
            if(ink)gray[y*w+x]=0;
        }
        // Staff rules through the tip and feet do not erase the open angular shape.
        if(kind==0)for(int y:new int[]{30,46})for(int x=0;x<w;x++)gray[y*w+x]=0;
        var m=OmrScoreInterpreter.class.getDeclaredMethod("caretAboveBeam",byte[].class,int.class,int.class,
                float.class,int.class,float.class);m.setAccessible(true);
        return (boolean)m.invoke(null,gray,w,h,50f,46,20f);
    }
    @Test public void staffCrossedCaretIsRecognized()throws Exception {assertTrue(matches(0));}
    @Test public void parallelBeamsAreNotCarets()throws Exception {assertFalse(matches(1));}
    @Test public void isolatedVerticalStrokeIsNotCaret()throws Exception {assertFalse(matches(2));}
}
