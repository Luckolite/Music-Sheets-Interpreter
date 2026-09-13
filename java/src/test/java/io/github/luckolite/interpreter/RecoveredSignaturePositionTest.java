// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sharp and staff-rule geometry with two labels referring to the same ink. */
public class RecoveredSignaturePositionTest {
    private void alias(JoinedSignatureSharpTest p,int top) {
        int w=JoinedSignatureSharpTest.W;
        for(int y=top+15;y<=top+17;y++)for(int x=20;x<620;x++)p.gray[y*w+x]=0;
        for(int y=top+15;y<=top+17;y++)for(int x=76;x<=100;x++)p.labels[y*w+x]=5;
    }
    @Test public void aRuleFragmentCannotCountTheSameSharpTwice() {
        var p=new JoinedSignatureSharpTest();p.row(100,1,0,false);alias(p,100);
        assertEquals(List.of(1),p.keys());
    }
    @Test public void repeatedHeaderAliasCannotChangeTheKey() {
        var p=new JoinedSignatureSharpTest();p.row(100,1,0,false);p.row(310,1,0,false);alias(p,310);
        assertEquals(List.of(1),p.keys());
    }
    @Test public void separatePrintedSharpsRemainSeparate() {
        var p=new JoinedSignatureSharpTest();p.row(100,2,0,false);alias(p,100);
        assertEquals(List.of(2),p.keys());
    }
    @Test public void actualIncreaseInSharpsSurvives() {
        var p=new JoinedSignatureSharpTest();p.row(100,1,0,false);p.row(310,2,0,false);alias(p,310);
        assertEquals(List.of(1,2),p.keys());
    }
    @Test public void sourceAndSemanticArraysStayUnchanged() {
        var p=new JoinedSignatureSharpTest();p.row(100,1,0,false);alias(p,100);
        byte[] labels=p.labels.clone(),gray=p.gray.clone();p.keys();assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);
    }
}
