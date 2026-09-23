// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original 25-pixel rules at a fractional staff spacing, with short-rule controls. */
public class PixelRoundedLedgerTest {
    private static final int W=180,H=180;

    private static byte[] ink(int left,int right) {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=94;y++)for(int x=80;x<=100;x++)
            if(Math.pow((x-90)/10.0,2)+Math.pow((y-87)/7.0,2)<=1)gray[y*W+x]=0;
        for(int x=left;x<=right;x++)gray[87*W+x]=0;
        return gray;
    }

    private static boolean supported(int left,int right)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object head=constructor.newInstance(220,80,100,80,94,90f,87f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasLedgerInk",
                byte[].class,int.class,int.class,type,float.class,boolean.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,ink(left,right),W,H,head,16.75f,false);
    }

    @Test public void printedTwentyFivePixelRuleSupportsHead()throws Exception {
        assertTrue(supported(78,102));
    }
    @Test public void shorterRuleDoesNotSupportHead()throws Exception {
        assertFalse(supported(78,101));
    }
    @Test public void oneSidedUnderlineDoesNotSupportStemlessHead()throws Exception {
        assertFalse(supported(80,104));
    }
}
