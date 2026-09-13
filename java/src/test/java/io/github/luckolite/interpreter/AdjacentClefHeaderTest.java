// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original flat signatures with an earlier overlapping, empty measure rectangle. */
public class AdjacentClefHeaderTest {
    private HeaderAccidentalOwnershipTest.Page page(){var p=new HeaderAccidentalOwnershipTest.Page(true,185);p.flat(122,140);return p;}
    private OmrScoreInterpreter.Analysis read(HeaderAccidentalOwnershipTest.Page p,boolean overlap,boolean raw,float start){
        var m=new ArrayList<MeasureRegion>();if(overlap)m.add(new MeasureRegion(0,.04f,.1f,.9f));m.add(new MeasureRegion(start/420,1,.1f,.9f));
        return OmrScoreInterpreter.analyze(p.labels,raw?p.gray:null,420,260,m);
    }
    @Test public void adjacentClefPreservesTheSignaturePrefix(){assertEquals(List.of(new ScoreKeyChange(1,-3)),read(page(),true,true,85).keyChanges());}
    @Test public void semanticClefAlsoRetainsThePrefix(){assertEquals(List.of(new ScoreKeyChange(1,-3)),read(page(),true,false,85).keyChanges());}
    @Test public void normalFirstMeasureKeepsAllThreeFlats(){assertEquals(List.of(new ScoreKeyChange(0,-3)),read(page(),false,true,85).keyChanges());}
    @Test public void distantClefCannotReopenAnEarlierHeader(){assertTrue(read(page(),true,true,175).keyChanges().isEmpty());}
    @Test public void aLaterLocalFlatRemainsExplicit(){var p=page();p.flat(250,132);var a=read(p,true,true,85);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,a.notes().get(a.notes().size()-1).writtenAccidental());}
    @Test public void analysisPreservesSourceArrays(){var p=page();var l=p.labels.clone();var g=p.gray.clone();read(p,true,true,85);assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
