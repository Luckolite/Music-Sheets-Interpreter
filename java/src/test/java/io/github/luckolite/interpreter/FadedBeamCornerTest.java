// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic dark beam attached to a pale owner stem, without score pixels. */
public final class FadedBeamCornerTest {
    private List<ScoreNoteEvent> notes(boolean fullHead,boolean missingStem) {
        int w=320,h=280;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=15;x<305;x++){gray[y*w+x]=0;labels[y*w+x]=4;}
        for(int y=88;y<=104;y++)for(int x=81;x<=103;x++)if((x-92)*(x-92)/121d+(y-96)*(y-96)/64d<=1){gray[y*w+x]=0;labels[y*w+x]=2;}
        for(int y=96;y<=150;y++)for(int x=81;x<=83;x++)if(!missingStem||y<114||y>124){gray[y*w+x]=(byte)195;labels[y*w+x]=1;}
        for(int x=81;x<=136;x++){int cy=Math.round(147-(x-81)*.4f);for(int y=cy-2;y<=cy+2;y++){gray[y*w+x]=0;labels[y*w+x]=1;}}
        if(fullHead)for(int y=140;y<=152;y++)for(int x=80;x<=94;x++)if((x-87)*(x-87)/49d+(y-146)*(y-146)/36d<=1)gray[y*w+x]=0;
        for(int y=143;y<=150;y++)for(int x=81;x<=91;x++)if((gray[y*w+x]&255)<165)labels[y*w+x]=2;
        return OmrScoreInterpreter.analyze(labels,gray,w,h,List.of(new MeasureRegion(.02f,.98f,.02f,.98f))).notes();
    }
    @Test public void paleStemStillOwnsNarrowBeamCorner(){assertEquals(1,notes(false,false).size());}
    @Test public void realOvalAtCornerIsPreserved(){assertEquals(2,notes(true,false).size());}
    @Test public void brokenStemDoesNotProveOwnership(){assertEquals(2,notes(false,true).size());}
}
