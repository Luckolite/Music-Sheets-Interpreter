// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic fragments with independent printed dots and a descending tail. */
public class NarrowBassClefTest {
    private Object construct(String name,Object... args)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);return ctor.newInstance(args);
    }
    private boolean recognizes(int scale,boolean upper,boolean lower,boolean tail,int lowerOffset,
            int width,int bodyTop)throws Exception {
        int w=320*scale,h=200*scale;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int line=0;line<5;line++)rect(gray,w,scale,20,80+16*line,280,1);
        rect(gray,w,scale,100,bodyTop,width,26);
        if(upper)rect(gray,w,scale,126,86,5,5);
        if(lower)rect(gray,w,scale,126+lowerOffset,102,5,5);
        if(tail)rect(gray,w,scale,102,117,5,14);
        // The model retained only the right body fragment; printed evidence is checked separately.
        var body=construct("Component",150*scale*scale,100*scale,(100+width)*scale-1,
                bodyTop*scale,(bodyTop+26)*scale-1,(100+width*.5f)*scale,(bodyTop+13f)*scale);
        var staff=construct("Staff",80f*scale,144f*scale,16f*scale);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("rawBassClef",body.getClass(),
                byte[].class,int.class,int.class,staff.getClass());method.setAccessible(true);
        return (boolean)method.invoke(null,body,gray,w,h,staff);
    }
    private void rect(byte[] image,int width,int scale,int x,int y,int w,int h) {
        for(int yy=y*scale;yy<(y+h)*scale;yy++)for(int xx=x*scale;xx<(x+w)*scale;xx++)image[yy*width+xx]=0;
    }
    @Test public void narrowBodyWithBothPrintedDotsAndTailIsStillABassClef()throws Exception {
        assertTrue(recognizes(1,true,true,true,0,18,82));
        assertTrue(recognizes(2,true,true,true,0,18,82));
    }
    @Test public void missingEitherDotCannotChangeTheClef()throws Exception {
        assertFalse(recognizes(1,false,true,true,0,18,82));
        assertFalse(recognizes(1,true,false,true,0,18,82));
    }
    @Test public void punctuationBesideABodyWithoutTailIsRejected()throws Exception {
        assertFalse(recognizes(1,true,true,false,0,18,82));
    }
    @Test public void DotsMustAlignVertically()throws Exception {
        assertFalse(recognizes(1,true,true,true,8,18,82));
    }
    @Test public void isolatedNarrowStrokeCannotEstablishAClef()throws Exception {
        assertFalse(recognizes(1,true,true,true,0,9,82));
    }
    @Test public void bodyMustStartNearTheTopStaffRule()throws Exception {
        assertFalse(recognizes(1,true,true,true,0,18,98));
    }
}
