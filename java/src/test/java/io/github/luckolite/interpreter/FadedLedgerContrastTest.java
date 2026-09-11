// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;
public class FadedLedgerContrastTest {
 private RemoteLedgerEvidenceTest.Page page(boolean above,boolean complete,int headInk,int ruleInk,int paper) {
  var p=new RemoteLedgerEvidenceTest.Page(above,false,complete,true,false);
  for(int i=0;i<p.gray.length;i++)if((p.gray[i]&255)==255)p.gray[i]=(byte)paper;
  else p.gray[i]=(byte)(p.labels[i]==4?ruleInk:headInk);
  return p;
 }
 @Test public void fadedUpperLedgerKeepsPrintedPitch(){var n=page(true,true,110,190,250).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
 @Test public void fadedLowerLedgerKeepsPrintedPitch(){var n=page(false,true,110,190,250).remote();assertNotNull(n);assertEquals(-8,n.staffStep());}
 @Test public void uniformlyFadedInkKeepsPrintedPitch(){var n=page(true,true,180,180,250).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
 @Test public void fadedUnderlinesStillLackLedgerSeries(){assertNull(page(true,false,110,190,250).remote());}
 @Test public void lowerFadedUnderlinesStillLackLedgerSeries(){assertNull(page(false,false,110,190,250).remote());}
 @Test public void paleRulesBesideBlackLetterKeepOriginalRejection(){assertNull(page(true,false,0,190,255).remote());}
 @Test public void noPrintedRuleCannotBeRecoveredFromLabels(){var p=page(true,true,110,250,250);assertNull(p.remote());}
 @Test public void paleFragmentBesideCrispInkDoesNotGainLedgerSupport(){
  var p=page(true,true,110,190,250);
  // Original pale oval and rules beside a crisp rectangular instruction stroke.
  for(int y=40;y<=93;y++)for(int x=75;x<=83;x++)p.gray[y*RemoteLedgerEvidenceTest.W+x]=0;
  assertNull(p.remote());
 }
 @Test public void callerPixelsAndLabelsArePreserved(){var p=page(true,true,110,190,250);byte[] a=p.gray.clone(),b=p.labels.clone();p.remote();assertArrayEquals(a,p.gray);assertArrayEquals(b,p.labels);}
}
