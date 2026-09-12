// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original printed bowls and extended strokes; no score pixels. */
public class ClippedFlatRecoveryTest {
    private static final int W=100,H=100;
    private final byte[] gray=new byte[W*H];
    public ClippedFlatRecoveryTest() {
        Arrays.fill(gray,(byte)255);
        ink(30,32,25,64,0);
        ink(30,41,49,50,0);ink(30,41,60,61,0);ink(40,41,49,61,0);
    }
    private void ink(int left,int right,int top,int bottom,int value) {
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)gray[y*W+x]=(byte)value;
    }
    private Object make(String name,Object...args)throws Exception {
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];
        c.setAccessible(true);return c.newInstance(args);
    }
    private boolean detect()throws Exception {
        Object seed=make("Component",55,30,41,49,64,34f,56f);
        Object candidate=make("AccidentalCandidate",seed,(byte)3);
        Object head=make("Component",120,53,69,49,61,61f,55f);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals("rawFlatFromBowl")) {
            m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,List.of(candidate),head,16f);
        }
        throw new IllegalStateException();
    }
    @Test public void completeFlatStillRecovers()throws Exception {assertTrue(detect());}
    @Test public void aStrokeContinuingBelowTheCropCannotProveAFlat()throws Exception {
        ink(30,32,65,88,0);assertFalse(detect());
    }
    @Test public void aStrokeContinuingAboveTheCropCannotProveAFlat()throws Exception {
        ink(30,32,0,24,0);assertFalse(detect());
    }
    @Test public void fadedContinuationStillBelongsToTheClippedMark()throws Exception {
        ink(30,32,65,68,0);ink(30,32,69,88,205);assertFalse(detect());
    }
    @Test public void staffRulesDoNotHideAClippedStroke()throws Exception {
        ink(0,99,55,56,0);ink(30,32,65,88,0);assertFalse(detect());
    }
    @Test public void staffRulesDoNotRemoveACompleteFlat()throws Exception {
        ink(0,99,55,56,0);assertTrue(detect());
    }
    @Test public void disconnectedInkBeyondTheCropDoesNotRejectAFlat()throws Exception {
        ink(30,32,73,88,0);assertTrue(detect());
    }
    @Test public void detectionPreservesSourcePixels()throws Exception {
        ink(30,32,65,88,0);var before=gray.clone();detect();assertArrayEquals(before,gray);
    }
}
