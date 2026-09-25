// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Bounded source-to-performance mapping for one unambiguous D.S. al Coda.
 * It is deliberately linear when any required anchor is absent or ambiguous.
 * Recognition supplies source boundaries; no title, page count or bar-number rules. */
public final class ScoreNavigationPlan {
    private final List<Integer> sourceMeasures;
    private final boolean dalSegnoApplied;
    private ScoreNavigationPlan(List<Integer> sourceMeasures,boolean dalSegnoApplied) {
        this.sourceMeasures=List.copyOf(sourceMeasures);this.dalSegnoApplied=dalSegnoApplied;
    }
    public int measureCount(){return sourceMeasures.size();}
    public int sourceMeasure(int playbackMeasure){return sourceMeasures.get(playbackMeasure);}
    public List<Integer> sourceMeasures(){return sourceMeasures;}
    public boolean dalSegnoApplied(){return dalSegnoApplied;}
    /** Source clicks select the nearest occurrence to the active performance cursor. */
    public int playbackMeasure(int sourceMeasure,int preferredPlaybackMeasure) {
        int best=-1;long distance=Long.MAX_VALUE;
        for(int i=0;i<sourceMeasures.size();i++)if(sourceMeasures.get(i)==sourceMeasure) {
            long d=Math.abs((long)i-preferredPlaybackMeasure);
            if(d<distance){distance=d;best=i;}
        }
        return best;
    }

    public static ScoreNavigationPlan create(int sourceMeasureCount,List<ScorePlaybackDirection> directions) {
        if(sourceMeasureCount<0)throw new IllegalArgumentException("Negative measure count");
        var anchors=new EnumMap<ScorePlaybackDirection.Kind,Integer>(ScorePlaybackDirection.Kind.class);
        boolean valid=directions!=null;
        if(directions!=null)for(var direction:directions) {
            if(direction==null||direction.measureBoundary()>sourceMeasureCount){valid=false;continue;}
            Integer previous=anchors.putIfAbsent(direction.kind(),direction.measureBoundary());
            // Duplicated signs for parallel staves agree. Competing destinations do not.
            if(previous!=null&&previous!=direction.measureBoundary())valid=false;
        }
        if(valid&&anchors.size()==ScorePlaybackDirection.Kind.values().length) {
            int segno=anchors.get(ScorePlaybackDirection.Kind.SEGNO);
            int toCoda=anchors.get(ScorePlaybackDirection.Kind.TO_CODA);
            int dalSegno=anchors.get(ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA);
            int coda=anchors.get(ScorePlaybackDirection.Kind.CODA);
            if(segno<toCoda&&toCoda<=dalSegno&&dalSegno<=coda&&coda<sourceMeasureCount) {
                var route=new ArrayList<Integer>();
                // First pass ignores To Coda. Only reaching D.S. arms the jump.
                append(route,0,dalSegno);
                append(route,segno,toCoda);
                append(route,coda,sourceMeasureCount);
                return new ScoreNavigationPlan(route,true);
            }
        }
        var route=new ArrayList<Integer>();append(route,0,sourceMeasureCount);
        return new ScoreNavigationPlan(route,false);
    }
    private static void append(List<Integer> target,int first,int end) {
        for(int source=first;source<end;source++)target.add(source);
    }
}
