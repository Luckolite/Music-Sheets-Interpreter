// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff, note, and detached finger-numeral geometry. */
public class DetachedFingeringHeadTest {
    static final int W=250,H=230,G=16;
    final byte[] gray=new byte[W*H];
    public DetachedFingeringHeadTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=15;x<235;x++)gray[y*W+x]=0;
        for(int y=104;y<=120;y++)for(int x=90;x<=110;x++)
            if(Math.pow((x-100)/10d,2)+Math.pow((y-112)/8d,2)<=1)gray[y*W+x]=0;
        for(int y=60;y<=112;y++)gray[y*W+110]=0;
        for(int y=155;y<=165;y++)for(int x=94;x<=106;x++)
            if(Math.pow((x-100)/6d,2)+Math.pow((y-160)/5d,2)<=1)gray[y*W+x]=0;
    }
    int rejected(boolean upper,boolean lowerStem,int fingerX)throws Exception {
        if(lowerStem)for(int y=112;y<=160;y++)gray[y*W+110]=0;
        var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=component.getDeclaredConstructors()[0];cc.setAccessible(true);
        var note=cc.newInstance(300,90,110,104,120,100f,112f);
        var numeral=cc.newInstance(80,fingerX-6,fingerX+6,155,165,(float)fingerX,160f);
        var staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staff.getDeclaredConstructors()[0];sc.setAccessible(true);
        var line=sc.newInstance(80f,144f,16f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("detachedFingeringHeads",byte[].class,
                int.class,int.class,List.class,List.class);method.setAccessible(true);
        return ((List<?>)method.invoke(null,gray,W,H,upper?List.of(note,numeral):List.of(numeral),List.of(line))).size();
    }
    @Test public void detachedFingerNumeralBeneathStemmedNoteIsRejected()throws Exception {assertEquals(1,rejected(true,false,100));}
    @Test public void numeralWithoutOwnerIsPreserved()throws Exception {assertEquals(0,rejected(false,false,100));}
    @Test public void independentlyStemmedLowNoteIsPreserved()throws Exception {assertEquals(0,rejected(true,true,100));}
    @Test public void offsetLowNoteIsPreserved()throws Exception {assertEquals(0,rejected(true,false,119));}
}
