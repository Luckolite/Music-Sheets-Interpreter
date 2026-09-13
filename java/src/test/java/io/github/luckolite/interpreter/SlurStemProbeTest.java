// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original quarter-note stems beside a quadratic slur. */
public class SlurStemProbeTest {
    private static final int W=260,H=200;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private boolean down,left;
    private int y(int v){return down?180-v:v;}
    private int x(int v){return left?200-v:v;}
    private void ink(int xx,int yy,int label){gray[y(yy)*W+x(xx)]=0;labels[y(yy)*W+x(xx)]=(byte)label;}
    private void setup(boolean down,boolean left) {
        this.down=down;this.left=left;Arrays.fill(gray,(byte)255);
        for(int line=10;line<=90;line+=20)for(int yy=line;yy<line+2;yy++)for(int xx=10;xx<220;xx++) {
            gray[y(yy)*W+xx]=0;labels[y(yy)*W+xx]=4;
        }
        int center=down?110:90;if(left)center=200-center;
        for(int yy=99;yy<=111;yy++)for(int xx=center-10;xx<=center+10;xx++)
            if((xx-center)*(xx-center)/100f+(yy-105)*(yy-105)/36f<=1)ink(xx,yy,2);
        for(int yy=50;yy<=105;yy++)for(int xx=99;xx<=101;xx++)ink(xx,yy,1);
    }
    private void slur() {
        for(int i=0;i<=300;i++) {
            float t=i/300f,u=1-t;
            int xx=Math.round(u*u*111+2*u*t*148+t*t*200);
            int yy=Math.round(u*u*84+2*u*t*46+t*t*74);
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)ink(xx+dx,yy+dy,5);
        }
    }
    private int beams()throws Exception {
        var hcType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var hc=hcType.getDeclaredConstructors()[0];hc.setAccessible(true);
        int center=down?110:90;
        var head=hc.newInstance(180,center-10,center+10,down?69:99,down?81:111,(float)center,(float)y(105));
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);
        var staff=sc.newInstance(down?90f:10f,down?170f:90f,20f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hcType,st);m.setAccessible(true);
        return (int)m.invoke(null,labels,gray,W,H,head,staff);
    }
    @Test public void aSlurBesideAnUpStemDoesNotCreateAnEighth()throws Exception {setup(false,false);slur();assertEquals(0,beams());}
    @Test public void aSlurBesideADownStemDoesNotCreateAnEighth()throws Exception {setup(true,false);slur();assertEquals(0,beams());}
    @Test public void aLeftSideSlurDoesNotCreateABeam()throws Exception {setup(false,true);slur();assertEquals(0,beams());}
    @Test public void anAttachedBeamBesideTheSameSlurIsRetained()throws Exception {
        setup(false,false);slur();for(int yy=50;yy<=58;yy++)for(int xx=100;xx<185;xx++)ink(xx,yy,1);assertEquals(1,beams());
    }
    @Test public void detectionDoesNotChangeTheImage()throws Exception {
        setup(false,false);slur();var g=gray.clone();var l=labels.clone();beams();assertArrayEquals(g,gray);assertArrayEquals(l,labels);
    }
}
