// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric naturals; a semantic head island overlaps the upper connector. */
public class NaturalCrossbarOwnershipTest {
    static final int W=120,H=110;
    byte[] gray=new byte[W*H],labels=new byte[W*H];
    void rect(int x,int y,int w,int h,int color){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=(byte)color;}
    void fixture(boolean lowerSpine,boolean lowerBar) {
        Arrays.fill(gray,(byte)255);
        for(int y=20;y<=76;y+=14)for(int x=5;x<115;x++){gray[y*W+x]=0;labels[y*W+x]=4;}
        rect(35,18,2,28,0);rect(46,29,2,lowerSpine?28:16,0);
        rect(35,30,13,2,0);if(lowerBar)rect(35,43,13,2,0);
    }
    Object make(String name,Object...args)throws Exception {
        var ctor=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];
        ctor.setAccessible(true);return ctor.newInstance(args);
    }
    List<?> rejected(boolean withSeed,boolean fullCandidate,float followingX)throws Exception {
        Object candidate=make("Component",50,42,fullCandidate?59:51,26,36,46f,31f);
        Object following=make("Component",100,(int)followingX-7,(int)followingX+7,33,43,followingX,38f);
        Object seed=make("Component",80,35,45,29,46,40f,37.5f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("findStaffs",byte[].class,byte[].class,int.class,int.class,List.class);
        method.setAccessible(true);
        Object staffs=method.invoke(null,labels,gray,W,H,List.of(new MeasureRegion(.04f,.97f,.10f,.80f)));
        var fn=OmrScoreInterpreter.class.getDeclaredMethod("naturalCrossbarHeads",byte[].class,int.class,int.class,List.class,List.class,List.class);
        fn.setAccessible(true);
        return (List<?>)fn.invoke(null,gray,W,H,List.of(candidate,following),staffs,
                withSeed?List.of(make("AccidentalCandidate",seed,(byte)3)):List.of());
    }
    @Test public void completeNaturalOwnsItsSmallUpperConnector()throws Exception{fixture(true,true);assertEquals(1,rejected(true,false,70).size());}
    @Test public void flatDoesNotAcquireNaturalOwnership()throws Exception{fixture(false,true);assertTrue(rejected(true,false,70).isEmpty());}
    @Test public void singleFlagConnectorIsNotEnough()throws Exception{fixture(true,false);assertTrue(rejected(true,false,70).isEmpty());}
    @Test public void accidentalSemanticSeedIsRequired()throws Exception{fixture(true,true);assertTrue(rejected(false,false,70).isEmpty());}
    @Test public void independentlyFullSizedHeadIsNotRemoved()throws Exception{fixture(true,true);assertTrue(rejected(true,true,70).isEmpty());}
    @Test public void distantRealNoteDoesNotAnchorOwnership()throws Exception{fixture(true,true);assertTrue(rejected(true,false,110).isEmpty());}
    @Test public void decoderRejectsConnectorWithoutDroppingItsRealFollowingNote() {
        fixture(true,true);
        for(int y=29;y<=46;y++)for(int x=35;x<=45;x++)labels[y*W+x]=3;
        for(int y=26;y<=36;y++)for(int x=42;x<=51;x++)
            if((x-46.5f)*(x-46.5f)/20.25f+(y-31)*(y-31)/25f<=1)labels[y*W+x]=2;
        for(int y=32;y<=44;y++)for(int x=61;x<=79;x++)
            if((x-70)*(x-70)/81f+(y-38)*(y-38)/36f<=1){gray[y*W+x]=0;labels[y*W+x]=2;}
        for(int y=38;y<=70;y++){gray[y*W+61]=0;labels[y*W+61]=1;}
        var notes=OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.04f,.97f,.10f,.80f)));
        assertEquals(1,notes.size());
        assertTrue(notes.get(0).positionInMeasure()>.5f);
    }
}
