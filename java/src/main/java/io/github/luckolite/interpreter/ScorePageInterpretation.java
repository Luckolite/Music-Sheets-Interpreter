// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.List;

/** Cacheable geometry, logical measure start, and notes for one undecorated PDF page. */
public record ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes,
                                      int firstMeasureNumber, List<ScoreKeyChange> keyChanges,
                                      List<ScoreTempoChange> tempoChanges,
                                      List<ScoreMeterChange> meterChanges, List<ScoreRestEvent> rests,
                                      List<ScoreTechniqueChange> techniqueChanges,
                                      List<ScoreDynamicChange> dynamicChanges) {
    public ScorePageInterpretation(List<MeasureRegion> measures,List<ScoreNoteEvent> notes,int first,
            List<ScoreKeyChange> keys,List<ScoreTempoChange> tempos,List<ScoreMeterChange> meters,
            List<ScoreRestEvent> rests,List<ScoreTechniqueChange> techniques) {
        this(measures,notes,first,keys,tempos,meters,rests,techniques,List.of());
    }
    public ScorePageInterpretation(List<MeasureRegion> measures,List<ScoreNoteEvent> notes,int first,
            List<ScoreKeyChange> keys,List<ScoreTempoChange> tempos,List<ScoreMeterChange> meters,List<ScoreRestEvent> rests) {
        this(measures,notes,first,keys,tempos,meters,rests,List.of());
    }
    public ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes,
                                    int firstMeasureNumber, List<ScoreKeyChange> keys,
                                    List<ScoreTempoChange> tempos, List<ScoreMeterChange> meters) {
        this(measures, notes, firstMeasureNumber, keys, tempos, meters, List.of());
    }
    public ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes,
                                    int firstMeasureNumber, List<ScoreKeyChange> keys,
                                    List<ScoreTempoChange> tempos) {
        this(measures, notes, firstMeasureNumber, keys, tempos, List.of());
    }
    public ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes) {
        this(measures, notes, 0, List.of(), List.of());
    }

    public ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes,
                                   int firstMeasureNumber) {
        this(measures, notes, firstMeasureNumber, List.of(), List.of());
    }

    public ScorePageInterpretation {
        measures = measures == null ? List.of() : List.copyOf(measures);
        notes = notes == null ? List.of() : List.copyOf(notes);
        firstMeasureNumber = Math.max(0, firstMeasureNumber);
        keyChanges = keyChanges == null ? List.of() : List.copyOf(keyChanges);
        tempoChanges = tempoChanges == null ? List.of() : List.copyOf(tempoChanges);
        meterChanges = meterChanges == null ? List.of() : List.copyOf(meterChanges);
        rests = rests == null ? List.of() : List.copyOf(rests);
        techniqueChanges = techniqueChanges == null ? List.of() : List.copyOf(techniqueChanges);
        dynamicChanges = dynamicChanges == null ? List.of() : List.copyOf(dynamicChanges);
    }
}
