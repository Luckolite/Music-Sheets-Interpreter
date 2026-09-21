// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tilted ordinary noteheads, never extracted from a score. */
public class HollowChordDiamondTest {
    @Test public void spaceBetweenStackedChordOvalsIsNotATouchDiamond() {
        int w=180,h=180;
        for(int gap:new int[]{10,12,14,16,20})for(double angle:new double[]{-.35,-.5,-.65}) {
            byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
            for(double center:new double[]{90-gap*.5,90+gap*.5})
            for(int y=50;y<130;y++)for(int x=50;x<130;x++) {
                double dx=x-90,dy=y-center;
                double u=dx*Math.cos(angle)+dy*Math.sin(angle),v=-dx*Math.sin(angle)+dy*Math.cos(angle);
                double radius=u*u/(gap*gap*.49)+v*v/(gap*gap*.1225);
                if(radius>=.40&&radius<=1.15)gray[y*w+x]=0;
            }
            assertFalse("stacked gap="+gap+" angle="+angle,ArtificialHarmonics.diamond(gray,w,h,90,90,gap));
        }
    }
    @Test public void ordinarySlantedOvalsCannotRaiseAChordTwoOctaves() {
        int w=180,h=180;
        for(int gap:new int[]{10,12,14,16,20})for(double angle:new double[]{-.35,-.5,-.65}) {
            byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
            for(int y=50;y<130;y++)for(int x=50;x<130;x++) {
                double dx=x-90,dy=y-90;
                double u=dx*Math.cos(angle)+dy*Math.sin(angle),v=-dx*Math.sin(angle)+dy*Math.cos(angle);
                double radius=u*u/(gap*gap*.49)+v*v/(gap*gap*.1225);
                if(radius>=.40&&radius<=1.15)gray[y*w+x]=0;
            }
            assertFalse("gap="+gap+" angle="+angle,ArtificialHarmonics.diamond(gray,w,h,90,90,gap));
        }
    }
}
