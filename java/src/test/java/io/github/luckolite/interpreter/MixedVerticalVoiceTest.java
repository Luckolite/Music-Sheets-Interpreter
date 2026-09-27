// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Procedural touching ovals on two independent shafts. */
public class MixedVerticalVoiceTest {
    static final int W=220,H=220;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public MixedVerticalVoiceTest(){Arrays.fill(gray,(byte)255);}
    void oval(int cy,boolean hollow){
        for(int y=cy-7;y<=cy+7;y++)for(int x=90;x<=110;x++){
            double d=Math.pow((x-100)/10d,2)+Math.pow((y-cy)/7d,2);
            if(d<=1){labels[y*W+x]=2;gray[y*W+x]=(byte)(hollow&&d<.42?255:0);}
        }
    }
    void stems(){for(int y=70;y<=136;y++){labels[y*W+110]=1;gray[y*W+110]=0;}for(int y=120;y<=175;y++){labels[y*W+90]=1;gray[y*W+90]=0;}}
    Object head(int cy)throws Exception{
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];c.setAccessible(true);
        return c.newInstance(190,90,110,cy-7,cy+7,100f,(float)cy);
    }
    boolean mixed()throws Exception{
        Object a=head(120),b=head(136);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("mixedStemmedVerticalVoices",byte[].class,byte[].class,int.class,int.class,a.getClass(),a.getClass(),float.class);
        m.setAccessible(true);return (boolean)m.invoke(null,labels,gray,W,H,a,b,16f);
    }
    @Test public void filledAboveHeldVoiceIsRecognized()throws Exception{stems();oval(120,false);oval(136,true);assertTrue(mixed());}
    @Test public void heldAboveFilledVoiceIsRecognized()throws Exception{stems();oval(120,true);oval(136,false);assertTrue(mixed());}
    @Test public void twoFilledBlobsDoNotUseMixedVoiceAllowance()throws Exception{stems();oval(120,false);oval(136,false);assertFalse(mixed());}
    @Test public void independentShaftEvidenceIsRequired()throws Exception{oval(120,false);oval(136,true);assertFalse(mixed());}
}
