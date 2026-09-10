// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** Staff-local absolute dynamic (direction 0) or hairpin (+1/-1). Positions are score geometry. */
public record ScoreDynamicChange(int measureIndex,float positionInMeasure,int staffIndex,int staffCount,
                                 int endMeasureIndex,float endPosition,float decibels,int direction,
                                 boolean sharedStaffs) {
    public ScoreDynamicChange(int measureIndex,float positionInMeasure,int staffIndex,int staffCount,
                              int endMeasureIndex,float endPosition,float decibels,int direction) {
        this(measureIndex,positionInMeasure,staffIndex,staffCount,endMeasureIndex,endPosition,decibels,direction,false);
    }
    public ScoreDynamicChange {
        if(measureIndex<0||endMeasureIndex<measureIndex||staffCount<1||staffCount>8
                ||staffIndex<0||staffIndex>=staffCount||!Float.isFinite(positionInMeasure)
                ||!Float.isFinite(endPosition)||positionInMeasure<0||positionInMeasure>1
                ||endPosition<0||endPosition>1||!Float.isFinite(decibels)||decibels< -24||decibels>9
                ||Math.abs(direction)>1||endMeasureIndex==measureIndex&&endPosition<positionInMeasure)
            throw new IllegalArgumentException("Invalid dynamic change");
    }
    public ScoreDynamicChange offset(int measures) {
        return new ScoreDynamicChange(measureIndex+measures,positionInMeasure,staffIndex,staffCount,
                endMeasureIndex+measures,endPosition,decibels,direction,sharedStaffs);
    }
}
