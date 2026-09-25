// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curved flag silhouettes on dark paper. */
public final class ShadedCurvedFlagTest {
    private static final int W=300,H=180;
    private byte[] image(boolean up,boolean hook) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)160);
        if(hook)for(int d=0;d<=34;d++) {
            int x=100+Math.round(20*(float)Math.sin(Math.PI*d/34)),y=up?80+d:120-d;
            for(int dx=0;dx<3;dx++)p[y*W+x+dx]=20;
        }
        return p;
    }
    private boolean flag(byte[] p,boolean up)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var head=ctor.newInstance(100,99,119,49,61,110f,55f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasCurvedFlag",byte[].class,byte[].class,
                int.class,int.class,type,float.class,int.class,int.class,boolean.class);
        method.setAccessible(true);return (boolean)method.invoke(null,new byte[W*H],p,W,H,head,16f,100,up?80:120,up);
    }
    @Test public void upStemFlagSurvivesShadedPaper()throws Exception {assertTrue(flag(image(true,true),true));}
    @Test public void downStemFlagSurvivesShadedPaper()throws Exception {assertTrue(flag(image(false,true),false));}
    @Test public void paperAloneDoesNotCreateAFlag()throws Exception {assertFalse(flag(image(false,false),false));}
    @Test public void sourcePixelsArePreserved()throws Exception {byte[] p=image(true,true),before=p.clone();flag(p,true);assertArrayEquals(before,p);}
}
