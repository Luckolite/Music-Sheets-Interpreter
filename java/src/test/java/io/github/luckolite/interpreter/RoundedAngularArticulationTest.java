// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original thick-line geometric symbols, independent of any score scan or font. */
public class RoundedAngularArticulationTest {
    static final int W=1600,H=1000;
    byte[] gray=new byte[W*H];
    public RoundedAngularArticulationTest(){Arrays.fill(gray,(byte)255);}
    void line(double ax,double ay,double bx,double by,double radius) {
        double vx=bx-ax,vy=by-ay,n=vx*vx+vy*vy;
        for(int y=(int)Math.floor(Math.min(ay,by)-radius);y<=Math.ceil(Math.max(ay,by)+radius);y++)
            for(int x=(int)Math.floor(Math.min(ax,bx)-radius);x<=Math.ceil(Math.max(ax,bx)+radius);x++){
                double t=Math.max(0,Math.min(1,((x-ax)*vx+(y-ay)*vy)/n));
                if(Math.hypot(x-ax-t*vx,y-ay-t*vy)<=radius)gray[y*W+x]=70;
            }
    }
    int read(){return NoteArticulationDetector.detect(new byte[W*H],gray,W,H,List.of(
            new NoteArticulationDetector.Anchor(300,220,16,0)))[0];}
    void accent(){line(289,182,309,189,1.6);line(309,189,289,195,1.6);}
    void caret(int y){line(293,y+12,300,y,1.6);line(300,y,307,y+12,1.6);}
    @Test public void thickRoundedAccent(){accent();assertEquals(NoteArticulation.ACCENT,read());}
    @Test public void compactRoundedAccentNeedsArmCentres(){line(293,182,307,186,1.75);line(307,186,293,190,1.75);assertEquals(NoteArticulation.ACCENT,read());}
    @Test public void compactAsymmetricAccentNeedsIndependentSlopes(){line(293,182,307,185,1.25);line(307,185,293,190,1.25);assertEquals(NoteArticulation.ACCENT,read());}
    @Test public void thickAsymmetricAccent(){line(289,181,309,190,1.6);line(309,190,290,196,1.6);assertEquals(NoteArticulation.ACCENT,read());}
    @Test public void thickRoundedMarcato(){caret(180);assertEquals(NoteArticulation.MARCATO,read());}
    @Test public void roundedFallbackNeverRelaxesHeadDemotion(){line(296,194,300,180,1.75);line(300,180,304,194,1.75);assertEquals(NoteArticulation.MARCATO,read());assertFalse(NoteArticulationDetector.marcatoAtHead(gray,W,H,295,179,305,195,16,true));}
    @Test public void marcatoJustBeyondFiveAndHalfSpaces(){caret(122);assertEquals(NoteArticulation.MARCATO,read());}
    @Test public void veryRemoteMarcatoIsUnowned(){caret(112);assertEquals(0,read());}
    @Test public void enlargedReachDoesNotAdmitAccent(){line(289,120,309,127,1.6);line(309,127,289,133,1.6);assertEquals(0,read());}
    @Test public void enlargedReachDoesNotAdmitTenuto(){line(292,128,308,128,1);assertEquals(0,read());}
    @Test public void reverseCaretIsBowingNotMarcato(){line(293,180,300,192,1.6);line(300,192,307,180,1.6);assertEquals(0,read());}
    @Test public void singleSlashIsNotAngular(){line(289,181,309,195,1.6);assertEquals(0,read());}
    @Test public void squareBowIsNotAngular(){line(292,194,292,180,1.2);line(292,180,308,180,1.2);line(308,180,308,194,1.2);assertEquals(0,read());}
    @Test public void archedSlurIsNotAngular(){for(int x=283;x<317;x++){double y=179+12*Math.pow((x-300)/17.,2);line(x,y,x+1,179+12*Math.pow((x+1-300)/17.,2),1.2);}assertEquals(0,read());}
    @Test public void filledTriangleIsNotAngular(){for(int y=180;y<=196;y++)for(int x=289;x<=309;x++)if(x<=309-2.5*Math.abs(y-188))gray[y*W+x]=70;assertEquals(0,read());}
    @Test public void semanticStemCannotBecomeAccent(){accent();byte[] labels=new byte[W*H];for(int i=0;i<gray.length;i++)if(gray[i]==70)labels[i]=OmrMeasurePostProcessor.STEM_OR_REST;assertEquals(0,NoteArticulationDetector.detect(labels,gray,W,H,List.of(new NoteArticulationDetector.Anchor(300,220,16,0)))[0]);}
    @Test public void closerStaffOwnsAngularMark(){accent();assertArrayEquals(new int[]{0,NoteArticulation.ACCENT},NoteArticulationDetector.detect(new byte[W*H],gray,W,H,List.of(new NoteArticulationDetector.Anchor(300,280,16,1),new NoteArticulationDetector.Anchor(300,220,16,0))));}
}
