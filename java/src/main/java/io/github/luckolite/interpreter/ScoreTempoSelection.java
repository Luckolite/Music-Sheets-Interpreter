// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;


import java.util.List;

/** The selected opening tempo controls playback; printed later changes keep their proportions. */
public final class ScoreTempoSelection {
    private ScoreTempoSelection() { }

    /** Non-quarter printed pulses take precedence over the meter's default pulse unit. */
    public static double selectedQuarterBpm(double selectedMarkedBpm, double fallbackQuarterBpm,
                                            List<ScoreTempoChange> printed) {
        if (printed != null) for (var mark : printed)
            if (mark.measureIndex()==0 && mark.positionInMeasure()==0 && mark.beatUnit()!=1)
                return selectedMarkedBpm * mark.beatUnit();
        return fallbackQuarterBpm;
    }

    public static List<ScoreTempoChange> atOpeningTempo(double selectedQuarterBpm,
                                                       List<ScoreTempoChange> printed) {
        if (printed == null || printed.isEmpty()) return List.of();
        ScoreTempoChange opening=null;
        for (ScoreTempoChange mark:printed) if(mark.measureIndex()==0&&mark.positionInMeasure()==0) {
            opening=mark;break;
        }
        if (opening==null) return List.copyOf(printed);
        double ratio=selectedQuarterBpm/opening.bpm();
        var result=new java.util.ArrayList<ScoreTempoChange>();
        for (ScoreTempoChange mark:printed) result.add(new ScoreTempoChange(mark.measureIndex(),mark.positionInMeasure(),
                Math.max(15,Math.min(1600,mark.bpm()*ratio)),mark.beatUnit()));
        return List.copyOf(result);
    }
}
