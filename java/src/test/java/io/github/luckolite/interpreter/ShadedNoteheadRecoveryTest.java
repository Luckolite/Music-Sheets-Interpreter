// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ellipses and broken-outline fixtures, never copied score pixels. */
public class ShadedNoteheadRecoveryTest {
    static final int W=240,H=220;
    byte[] gray=new byte[W*H],labels=new byte[W*H];
    void fixture(boolean down,int paper,int fill,boolean rectangle) {
        Arrays.fill(gray,(byte)paper);
        for(int y=60;y<=140;y+=20)for(int x=15;x<225;x++){gray[y*W+x]=30;labels[y*W+x]=4;}
        for(int y=81;y<=99;y++)for(int x=78;x<=102;x++) {
            float oval=(x-90)*(x-90)/144f+(y-90)*(y-90)/81f;
            if(rectangle||oval<=1)gray[y*W+x]=(byte)(rectangle
                    ?(x==78||x==102||y==81||y==99?30:fill):oval>.7f?30:fill);
        }
        // A little pale fill crosses the outer rail, as in a faded scan gap.
        if(!rectangle)for(int y=89;y<=91;y++)gray[y*W+79]=(byte)fill;
        int sx=down?78:102;
        for(int y=down?90:42;y<=(down?138:90);y++){gray[y*W+sx]=30;labels[y*W+sx]=1;}
    }
    List<ShadedNoteheadRecovery.Head> find(){return ShadedNoteheadRecovery.find(labels,gray,W,H,20,40,160);}
    @Test public void recoversGreyHeadWithBrokenUpstemOutline(){fixture(false,240,185,false);assertEquals(1,find().size());}
    @Test public void recoversGreyHeadWithDownstem(){fixture(true,240,185,false);assertEquals(1,find().size());}
    @Test public void whiteCounterIsNotGreyHead(){fixture(false,240,240,false);assertTrue(find().isEmpty());}
    @Test public void shadedPaperDoesNotBecomeAHead(){fixture(false,205,185,false);assertTrue(find().isEmpty());}
    @Test public void uncurvedRectangleIsExcluded(){fixture(false,240,185,true);assertTrue(find().isEmpty());}
    @Test public void needsSemanticStem(){fixture(false,240,185,false);Arrays.fill(labels,(byte)0);assertTrue(find().isEmpty());}
    @Test public void needsDarkPrintedStem(){fixture(false,240,185,false);for(int y=42;y<78;y++)gray[y*W+102]=(byte)240;assertTrue(find().isEmpty());}
    @Test public void narrowUnrelatedGreyStrokeIsExcluded(){fixture(false,240,185,false);for(int y=80;y<=100;y++)for(int x=78;x<89;x++)gray[y*W+x]=(byte)240;assertTrue(find().isEmpty());}
    @Test public void blankPageIsExcluded(){Arrays.fill(gray,(byte)240);assertTrue(find().isEmpty());}
    @Test public void badImageIsRejected(){assertTrue(ShadedNoteheadRecovery.find(labels,new byte[3],W,H,20,40,160).isEmpty());}
    @Test public void immutableInputs(){fixture(false,240,185,false);byte[] a=gray.clone(),b=labels.clone();find();assertArrayEquals(a,gray);assertArrayEquals(b,labels);}
    @Test public void tinySemanticFringeDoesNotBlockRecovery(){
        fixture(false,240,185,false);for(int y=88;y<=91;y++)labels[y*W+79]=2;
        var notes=OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
        assertEquals(1,notes.size());assertEquals(5,notes.get(0).staffStep());
        assertEquals(1f,notes.get(0).unbeamedDurationBeats(),.001f);
    }
    @Test public void translatedMarginFixturesNeverProduceOutOfBoundsRectangles(){
        fixture(false,240,185,false);byte[] sourceGray=gray.clone(),sourceLabels=labels.clone();
        int recovered=0;
        for(int shift=-95;shift<=145;shift+=3){
            Arrays.fill(gray,(byte)240);Arrays.fill(labels,(byte)0);
            for(int y=0;y<H;y++)for(int x=0;x<W;x++)if(x+shift>=0&&x+shift<W){
                gray[y*W+x+shift]=sourceGray[y*W+x];labels[y*W+x+shift]=sourceLabels[y*W+x];
            }
            for(var head:find()){
                recovered++;assertTrue(head.left()>=0&&head.right()<W);
                assertTrue(head.top()>=0&&head.bottom()<H);
                assertTrue(head.centerX()>=head.left()&&head.centerX()<=head.right());
                assertTrue(head.centerY()>=head.top()&&head.centerY()<=head.bottom());
            }
        }
        assertTrue(recovered>0);
    }
}
