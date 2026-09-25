// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
public class LedgerSpacePhaseTest {
    @Test public void lowerSpaceHasNoThirdLedger(){assertEquals(-5,LedgerSpacePhase.resolve(-5.6f,214,20,Float.NaN,200,180));}
    @Test public void asymmetricLowerOvalCanSitNearTheOuterQuarter(){assertEquals(-3,LedgerSpacePhase.resolve(-3.78f,218,20,Float.NaN,200,180));}
    @Test public void upperSpaceIsSymmetric(){assertEquals(11,LedgerSpacePhase.resolve(11.6f,186,20,Float.NaN,200,220));}
    @Test public void existingOuterRuleRetainsLinePitch(){assertEquals(-6,LedgerSpacePhase.resolve(-5.6f,214,20,214,200,180));}
    @Test public void oneLedgerCannotEstablishSpace(){assertEquals(-6,LedgerSpacePhase.resolve(-5.6f,214,20,Float.NaN,200,Float.NaN));}
    @Test public void wrongInnerSpacingCannotEstablishSpace(){assertEquals(-6,LedgerSpacePhase.resolve(-5.6f,214,20,Float.NaN,200,170));}
    @Test public void lineCenteredHeadKeepsPitch(){assertEquals(-6,LedgerSpacePhase.resolve(-5.9f,214,20,Float.NaN,200,180));}
    @Test public void existingSpaceKeepsPitch(){assertEquals(-5,LedgerSpacePhase.resolve(-5.2f,214,20,Float.NaN,200,180));}
    @Test public void insideStaffIsNotLedgerEvidence(){assertEquals(4,LedgerSpacePhase.resolve(3.6f,186,20,Float.NaN,200,220));}
}
