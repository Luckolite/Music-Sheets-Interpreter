// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

public class TieCurvatureRegressionTest {
    private float curvature(float[] profile) throws Exception {
        var method=OmrScoreInterpreter.class.getDeclaredMethod("arcCurvature",float[].class,int.class,int.class,int.class);
        method.setAccessible(true);return (float)method.invoke(null,profile,0,0,profile.length-1);
    }
    @Test public void partialSecondBeamsDoNotCountAsTiesInEitherDirection() throws Exception {
        for(int direction:new int[]{-1,1})for(int boundary:new int[]{30,45,55,70}) {
            float[] profile=new float[101];
            for(int x=0;x<=100;x++)profile[x]=100+(x>=boundary?8*direction:0);
            assertEquals(0,curvature(profile),.0001);
        }
    }
    @Test public void flatAndSlopedBeamsHaveNoReturningArc() throws Exception {
        for(float slope:new float[]{0,.15f,-.15f}) {
            float[] profile=new float[101];for(int x=0;x<=100;x++)profile[x]=100+slope*x;
            assertEquals(0,curvature(profile),.0001);
        }
    }
    @Test public void realTiesReturnToBothEndpointsAboveAndBelowTheHeads() throws Exception {
        for(int direction:new int[]{-1,1}) {
            float[] profile=new float[101];
            for(int x=0;x<=100;x++)profile[x]=100+direction*6*(1-(x-50)*(x-50)/2500f);
            assertTrue(curvature(profile)>2);
        }
    }
}
