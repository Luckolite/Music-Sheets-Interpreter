// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;import static org.junit.Assert.*;
/** Original faded hook and bright-paper contrast controls. */
public class FainterCurvedFlagTest extends PaleFlagRootTest {
 void fade(){for(int i=0;i<g.length;i++)if((g[i]&255)==35)g[i]=(byte)190;}
 @Test public void lowContrastFlagOnBrightPaperKeepsIndependentCurvature()throws Exception{setup(true,true,true);fade();assertEquals(1,beams());}
 @Test public void lowContrastDetachedHookDoesNotBecomeFlag()throws Exception{setup(false,true,true);fade();assertEquals(0,beams());}
 @Test public void shortPaleCornerDoesNotBecomeFlag()throws Exception{setup(false,false,true);for(int y=80;y<=86;y++)rect(110,110+(y-80),y,y,190);assertEquals(0,beams());}
 @Test public void shadowedPaperDoesNotReceiveAbsoluteThresholdBoost()throws Exception{setup(true,true,true);fade();for(int i=0;i<g.length;i++)if((g[i]&255)==255)g[i]=(byte)195;assertEquals(0,beams());}
}
