// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;

/** Cacheable geometry, logical measure start, and notes for one undecorated PDF page. */
public record ScorePageInterpretation(List<MeasureRegion> measures, List<ScoreNoteEvent> notes,
                                      int firstMeasureNumber, List<ScoreKeyChange> keyChanges,
                                      List<ScoreTempoChange> tempoChanges,
                                      List<ScoreMeterChange> meterChanges, List<ScoreRestEvent> rests,
                                      List<ScoreTechniqueChange> techniqueChanges,
                                      List<ScoreDynamicChange> dynamicChanges,
                                      List<ScorePlaybackDirection> playbackDirections) {
    public ScorePageInterpretation(List<MeasureRegion> measures,List<ScoreNoteEvent> notes,int first,
            List<ScoreKeyChange> keys,List<ScoreTempoChange> tempos,List<ScoreMeterChange> meters,
            List<ScoreRestEvent> rests,List<ScoreTechniqueChange> techniques,List<ScoreDynamicChange> dynamics) {
        this(measures,notes,first,keys,tempos,meters,rests,techniques,dynamics,List.of());
    }
    public ScorePageInterpretation withPlaybackDirections(List<ScorePlaybackDirection> directions) {
        return new ScorePageInterpretation(measures,notes,firstMeasureNumber,keyChanges,tempoChanges,meterChanges,
                rests,techniqueChanges,dynamicChanges,directions);
    }
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
        playbackDirections = playbackDirections == null ? List.of() : List.copyOf(playbackDirections);
        for(var direction:playbackDirections)if(direction.measureBoundary()>measures.size())
            throw new IllegalArgumentException("Playback direction is outside the page");
    }

    /** Each boundary belongs to the page containing the sign's incoming/outgoing bar. */
    public static List<ScorePlaybackDirection> directionsOnPage(
            List<ScorePlaybackDirection> changes, int firstMeasure, int measureAfterLast) {
        if (firstMeasure < 0 || measureAfterLast < firstMeasure)
            throw new IllegalArgumentException("Invalid page measure range");
        if (changes == null || changes.isEmpty() || firstMeasure == measureAfterLast) return List.of();
        return changes.stream().filter(change -> {
            int boundary = change.measureBoundary();
            boolean outgoing = change.kind() == ScorePlaybackDirection.Kind.TO_CODA
                    || change.kind() == ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA;
            return outgoing ? boundary > firstMeasure && boundary <= measureAfterLast
                    : boundary >= firstMeasure && boundary < measureAfterLast;
        }).map(change -> change.offset(-firstMeasure)).toList();
    }

    /** Keep a cross-page hairpin once, with its full endpoint relative to its starting page. */
    public static List<ScoreDynamicChange> dynamicsStartingOnPage(
            List<ScoreDynamicChange> changes, int firstMeasure, int measureAfterLast) {
        if (firstMeasure < 0 || measureAfterLast < firstMeasure)
            throw new IllegalArgumentException("Invalid page measure range");
        if (changes == null || changes.isEmpty()) return List.of();
        return changes.stream().filter(change -> change.measureIndex() >= firstMeasure
                && change.measureIndex() < measureAfterLast)
                .map(change -> change.offset(-firstMeasure))
                .collect(java.util.stream.Collectors.toList());
    }
}
