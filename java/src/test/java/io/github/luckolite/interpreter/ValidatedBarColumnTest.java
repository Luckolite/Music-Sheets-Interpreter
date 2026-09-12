// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff and stem geometry with a two-pixel semantic edge displacement. */
public class ValidatedBarColumnTest {
    static final int W=500,H=300;
    static class Drawing {
        byte[] labels=new byte[W*H],gray=new byte[W*H];
        Drawing(){Arrays.fill(gray,(byte)255);for(int y=170;y<=234;y+=16)rect(30,y,470,y,4,true);}
        void rect(int left,int top,int right,int bottom,int label,boolean printed){for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){labels[y*W+x]=(byte)label;if(printed)gray[y*W+x]=0;}}
        void head(int x,int y){for(int yy=y-4;yy<=y+4;yy++)for(int xx=x-7;xx<=x+7;xx++)if((xx-x)*(xx-x)/49d+(yy-y)*(yy-y)/16d<=1)rect(xx,yy,xx,yy,2,true);}
        void ordinaryNotes(){head(100,202);rect(107,153,107,202,1,true);head(350,218);rect(357,171,357,218,1,true);rect(400,168,400,236,1,true);}
        void displacedStem(boolean mirrored){
            int raw=245,semantic=mirrored?243:247,halo=mirrored?246:244;
            for(int y=120;y<=234;y++)gray[y*W+raw]=0;
            rect(semantic,120,semantic,234,1,false);
            rect(halo,174,halo,229,OmrMeasurePostProcessor.SYMBOL,false);
            head(mirrored?237:253,120);
        }
        List<MeasureRegion> measures(){return OmrMeasurePostProcessor.process(labels,gray,W,H);}
        List<Integer> boundaries() throws Exception {
            var m=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",byte[].class,byte[].class,int.class,int.class,int[].class,float.class,int.class,int.class,float.class);
            m.setAccessible(true);
            return (List<Integer>)m.invoke(null,labels,gray,W,H,new int[]{170,186,202,218,234},16f,30,470,0f);
        }
    }
    @Test public void aHighHeadVetoesTheColumnThatValidatedItsStem() throws Exception {var d=new Drawing();d.ordinaryNotes();d.displacedStem(false);assertEquals(List.of(30,400,470),d.boundaries());}
    @Test public void theOppositeSemanticEdgeAlsoKeepsTheMeasureWhole() throws Exception {var d=new Drawing();d.ordinaryNotes();d.displacedStem(true);assertEquals(List.of(30,400,470),d.boundaries());}
    @Test public void anUnattachedPrintedBarIsRetained(){var d=new Drawing();d.ordinaryNotes();d.rect(245,168,245,236,1,true);assertEquals(3,d.measures().size());}
    @Test public void analysisPreservesInputMasks(){var d=new Drawing();d.ordinaryNotes();d.displacedStem(false);var l=d.labels.clone();var g=d.gray.clone();d.measures();assertArrayEquals(l,d.labels);assertArrayEquals(g,d.gray);}
}
