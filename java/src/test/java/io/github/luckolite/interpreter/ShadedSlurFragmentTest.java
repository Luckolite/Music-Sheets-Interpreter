// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShadedSlurFragmentTest {
    private RawSlurFragmentTest.Page shaded(boolean upper,boolean stem,boolean oval) {
        var p=new RawSlurFragmentTest.Page(upper,true,stem,oval);
        for(int i=0;i<p.gray.length;i++)if((p.gray[i]&255)==255)p.gray[i]=(byte)150;
        return p;
    }
    @Test public void shadedLowerTieDoesNotBecomeANote(){assertNull(shaded(false,false,false).island());}
    @Test public void shadedUpperTieDoesNotBecomeANote(){assertNull(shaded(true,false,false).island());}
    @Test public void shadedStemmedSmallHeadIsPreserved(){assertNotNull(shaded(false,true,false).island());}
    @Test public void shadedHollowLedgerHeadIsPreserved(){assertNotNull(shaded(false,false,true).island());}
    @Test public void faintStemOnShadedPaperIsPreserved(){
        var p=shaded(false,true,false);
        for(int y=136;y<=184;y++)p.gray[y*RawSlurFragmentTest.W+165]=100;
        assertNotNull(p.island());
    }
    private boolean compact(boolean oval,boolean filled)throws Exception {
        int w=160,h=140;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)150);
        int area=0,minX=w,maxX=0,minY=h,maxY=0;long sx=0,sy=0;
        for(int x=60;x<=82;x++)for(int y=57;y<=72;y++) {
            double t=(x-60)/22d,rr=Math.pow((x-71)/11d,2)+Math.pow((y-64)/5d,2);
            boolean ink=oval?rr<=1&&(filled||rr>=.50):Math.abs(y-(60+6*4*t*(1-t)))<=1;
            if(ink){gray[y*w+x]=10;if(x>=69&&x<=77){area++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);sx+=x;sy+=y;}}
        }
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);Object head=constructor.newInstance(area,minX,maxX,minY,maxY,sx/(float)area,sy/(float)area);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("flatStemlessFragment",byte[].class,int.class,int.class,type,float.class);method.setAccessible(true);return (boolean)method.invoke(null,gray,w,h,head,16.5f);
    }
    @Test public void compactCompleteTieIsRejected()throws Exception {assertTrue(compact(false,false));}
    @Test public void compactFilledOvalIsPreserved()throws Exception {assertFalse(compact(true,true));}
    @Test public void compactHollowOvalIsPreserved()throws Exception {assertFalse(compact(true,false));}
}
