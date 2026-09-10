// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Original thin staff rules: no score scans or model output. */
public class StaffSkewRegressionTest {
    private static final int WIDTH=2000, HEIGHT=520;

    private byte[] rules(float slope) {
        byte[] labels=new byte[WIDTH*HEIGHT];
        for(int top:new int[]{101,241,381})for(int line=0;line<5;line++)
            for(int x=80;x<WIDTH-80;x++) {
                int y=Math.round(top+line*14+slope*(x-WIDTH*.5f));
                labels[y*WIDTH+x]=OmrMeasurePostProcessor.STAFF;
            }
        return labels;
    }

    @Test public void slightPositiveTiltBetweenCoarseAnglesIsRecovered() {
        assertEquals(.003f,OmrMeasurePostProcessor.estimateStaffSlope(rules(.003f),WIDTH,HEIGHT),.0007f);
    }

    @Test public void slightNegativeTiltIsRecoveredWithoutSkippingOddRows() {
        assertEquals(-.0042f,OmrMeasurePostProcessor.estimateStaffSlope(rules(-.0042f),WIDTH,HEIGHT),.0007f);
    }

    @Test public void horizontalThinRulesOnOddRowsStayHorizontal() {
        assertEquals(0f,OmrMeasurePostProcessor.estimateStaffSlope(rules(0),WIDTH,HEIGHT),0f);
    }

    @Test public void missingStaffEvidenceDoesNotInventASlope() {
        assertEquals(0f,OmrMeasurePostProcessor.estimateStaffSlope(new byte[WIDTH*HEIGHT],WIDTH,HEIGHT),0f);
    }

    @Test public void tiltedPrintedRulesPreserveTheFadedEndOfAStaff() {
        byte[] labels=new byte[WIDTH*HEIGHT],gray=new byte[WIDTH*HEIGHT];
        Arrays.fill(gray,(byte)255);
        float slope=.012f;
        for(int line=0;line<5;line++)for(int x=80;x<=1920;x++) {
            int y=Math.round(101+line*14+slope*(x-WIDTH*.5f));
            gray[y*WIDTH+x]=0;
            if(x<1100)labels[y*WIDTH+x]=OmrMeasurePostProcessor.STAFF;
        }
        for(int y=112;y<=168;y++) {
            gray[y*WIDTH+1920]=0;
            labels[y*WIDTH+1920]=OmrMeasurePostProcessor.STEM_OR_REST;
        }
        var measures=OmrMeasurePostProcessor.process(labels,gray,WIDTH,HEIGHT);
        assertFalse(measures.isEmpty());
        assertTrue(measures.stream().anyMatch(m->m.right()*WIDTH>1850));
    }

    @Test public void uphillMeasureBoxesStillReadFromLeftToRight() {
        float slope=-.012f;
        byte[] labels=rules(slope),gray=new byte[labels.length];
        Arrays.fill(gray,(byte)255);
        for(int p=0;p<labels.length;p++)if(labels[p]!=0)gray[p]=0;
        for(int top:new int[]{101,241,381})for(int x:new int[]{600,1200,1800}) {
            int start=Math.round(top+slope*(x-WIDTH*.5f));
            for(int y=start;y<=start+56;y++) {
                labels[y*WIDTH+x]=OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y*WIDTH+x]=0;
            }
        }
        var measures=OmrMeasurePostProcessor.process(labels,gray,WIDTH,HEIGHT);
        // The fixture has three unconnected rows, each containing four measures.
        // Reading order increases in x within a row, then resumes on the next row.
        assertEquals(12,measures.size());
        for(int row=0;row<3;row++) {
            for(int column=1;column<4;column++)
                assertTrue(measures.get(row*4+column).left()>measures.get(row*4+column-1).left());
            if(row>0)assertTrue(measures.get(row*4).top()>measures.get((row-1)*4).top());
        }
    }
}
