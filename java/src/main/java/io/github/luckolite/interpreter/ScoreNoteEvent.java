// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** One notehead plus written rhythm/tie/accidental metadata read by the measure-guide pass. */
public record ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                             int staffIndex, int staffCount, float pageY,
                             boolean tiedFromPrevious, int augmentationDots, int beamCount,
                             int writtenAccidental, float unbeamedDurationBeats, int tupletDivisor,
                             float followingRestBeats, int articulations, int clefBottomDiatonic,
                             boolean crossStaffBeam, float leadingRestBeats, boolean compactOpening) {
    /** Source-compatible constructor for callers without opening-measure geometry. */
    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
            int staffIndex, int staffCount, float pageY, boolean tiedFromPrevious,
            int augmentationDots, int beamCount, int writtenAccidental,
            float unbeamedDurationBeats, int tupletDivisor, float followingRestBeats,
            int articulations, int clefBottomDiatonic, boolean crossStaffBeam, float leadingRestBeats) {
        this(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,tiedFromPrevious,
                augmentationDots,beamCount,writtenAccidental,unbeamedDurationBeats,tupletDivisor,
                followingRestBeats,articulations,clefBottomDiatonic,crossStaffBeam,leadingRestBeats,false);
    }
    public ScoreNoteEvent withCompactOpening() {
        return new ScoreNoteEvent(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,
                tiedFromPrevious,augmentationDots,beamCount,writtenAccidental,unbeamedDurationBeats,
                tupletDivisor,followingRestBeats,articulations,clefBottomDiatonic,crossStaffBeam,leadingRestBeats,true);
    }
    public ScoreNoteEvent(int measureIndex,float positionInMeasure,int staffStep,int staffIndex,int staffCount,float pageY,
            boolean tiedFromPrevious,int augmentationDots,int beamCount,int writtenAccidental,float unbeamedDurationBeats,
            int tupletDivisor,float followingRestBeats,int articulations,int clefBottomDiatonic,boolean crossStaffBeam) {
        this(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,tiedFromPrevious,augmentationDots,beamCount,
                writtenAccidental,unbeamedDurationBeats,tupletDivisor,followingRestBeats,articulations,clefBottomDiatonic,crossStaffBeam,0);
    }
    public ScoreNoteEvent withLeadingRest(float beats) {
        return new ScoreNoteEvent(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,tiedFromPrevious,
                augmentationDots,beamCount,writtenAccidental,unbeamedDurationBeats,tupletDivisor,followingRestBeats,
                articulations,clefBottomDiatonic,crossStaffBeam,beats,compactOpening);
    }
    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY, boolean tiedFromPrevious,
                          int augmentationDots, int beamCount, int writtenAccidental,
                          float unbeamedDurationBeats, int tupletDivisor, float followingRestBeats,
                          int articulations, int clefBottomDiatonic) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, pageY,
                tiedFromPrevious, augmentationDots, beamCount, writtenAccidental,
                unbeamedDurationBeats, tupletDivisor, followingRestBeats, articulations, clefBottomDiatonic, false);
    }
    public ScoreNoteEvent withCrossStaffBeam() {
        return new ScoreNoteEvent(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, pageY,
                tiedFromPrevious, augmentationDots, Math.max(1, beamCount), writtenAccidental, 0,
                tupletDivisor, followingRestBeats, articulations, clefBottomDiatonic, true, leadingRestBeats,compactOpening);
    }
    public static final int CLEF_UNKNOWN = -1;
    public static final int CLEF_TREBLE = 30; // E4, C=0 diatonic numbering
    public static final int CLEF_BASS = 18;   // G2
    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY, boolean tiedFromPrevious,
                          int augmentationDots, int beamCount, int writtenAccidental,
                          float unbeamedDurationBeats, int tupletDivisor, float followingRestBeats, int articulations) {
        this(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,tiedFromPrevious,
                augmentationDots,beamCount,writtenAccidental,unbeamedDurationBeats,tupletDivisor,
                followingRestBeats,articulations,CLEF_UNKNOWN);
    }
    public ScoreNoteEvent withClef(int clef) {
        return new ScoreNoteEvent(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,
                tiedFromPrevious,augmentationDots,beamCount,writtenAccidental,unbeamedDurationBeats,
                tupletDivisor,followingRestBeats,articulations,clef,crossStaffBeam,leadingRestBeats,compactOpening);
    }
    public int diatonicPitchIdentity() {
        return staffStep + (clefBottomDiatonic == CLEF_UNKNOWN ? 0 : clefBottomDiatonic);
    }
    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY, boolean tiedFromPrevious,
                          int augmentationDots, int beamCount, int writtenAccidental,
                          float unbeamedDurationBeats, int tupletDivisor, float followingRestBeats) {
        this(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,pageY,
                tiedFromPrevious,augmentationDots,beamCount,writtenAccidental,
                unbeamedDurationBeats,tupletDivisor,followingRestBeats,0);
    }

    public ScoreNoteEvent withArticulations(int marks) {
        return new ScoreNoteEvent(measureIndex,positionInMeasure,staffStep,staffIndex,staffCount,
                pageY,tiedFromPrevious,augmentationDots,beamCount,writtenAccidental,
                unbeamedDurationBeats,tupletDivisor,followingRestBeats,marks,clefBottomDiatonic,crossStaffBeam,leadingRestBeats,compactOpening);
    }
    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY, boolean tiedFromPrevious,
                          int augmentationDots, int beamCount, int writtenAccidental,
                          float unbeamedDurationBeats, int tupletDivisor) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, pageY,
                tiedFromPrevious, augmentationDots, beamCount, writtenAccidental,
                unbeamedDurationBeats, tupletDivisor, 0);
    }
    public static final int ACCIDENTAL_FLAT = -1;
    public static final int ACCIDENTAL_NATURAL = 0;
    public static final int ACCIDENTAL_SHARP = 1;
    /** No local glyph: use the key signature unless an earlier accidental carries in the measure. */
    public static final int ACCIDENTAL_FROM_KEY = 2;
    /** Zero means the optical pass could not safely distinguish quarter/half/whole. */
    public static final float DURATION_UNKNOWN = 0f;
    public static final float DURATION_QUARTER = 1f;
    public static final float DURATION_HALF = 2f;
    public static final float DURATION_WHOLE = 4f;

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY,
                          boolean tiedFromPrevious, int augmentationDots, int beamCount,
                          int writtenAccidental, float unbeamedDurationBeats) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, pageY,
                tiedFromPrevious, augmentationDots, beamCount, writtenAccidental,
                unbeamedDurationBeats, 1);
    }

    /** Explicit printed triplet: three of this written value occupy the time of two. */
    public double durationScale() { return tupletDivisor == 3 ? 2.0 / 3.0 : 1.0; }

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY,
                          boolean tiedFromPrevious, int augmentationDots, int beamCount,
                          int writtenAccidental) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, pageY,
                tiedFromPrevious, augmentationDots, beamCount, writtenAccidental,
                DURATION_UNKNOWN);
    }

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount, .5f,
                false, 0, 0, ACCIDENTAL_FROM_KEY, DURATION_UNKNOWN);
    }

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY,
                          boolean tiedFromPrevious) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount,
                pageY, tiedFromPrevious, 0, 0, ACCIDENTAL_FROM_KEY, DURATION_UNKNOWN);
    }

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY,
                          boolean tiedFromPrevious, int augmentationDots) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount,
                pageY, tiedFromPrevious, augmentationDots, 0, ACCIDENTAL_FROM_KEY,
                DURATION_UNKNOWN);
    }

    public ScoreNoteEvent(int measureIndex, float positionInMeasure, int staffStep,
                          int staffIndex, int staffCount, float pageY,
                          boolean tiedFromPrevious, int augmentationDots, int beamCount) {
        this(measureIndex, positionInMeasure, staffStep, staffIndex, staffCount,
                pageY, tiedFromPrevious, augmentationDots, beamCount, ACCIDENTAL_FROM_KEY,
                DURATION_UNKNOWN);
    }

    public ScoreNoteEvent {
        if(!Float.isFinite(leadingRestBeats)||leadingRestBeats<0||leadingRestBeats>16)leadingRestBeats=0;
        if(clefBottomDiatonic!=CLEF_TREBLE&&clefBottomDiatonic!=CLEF_BASS)clefBottomDiatonic=CLEF_UNKNOWN;
        articulations &= NoteArticulation.ALL;
        if (!Float.isFinite(followingRestBeats) || followingRestBeats < 0 || followingRestBeats > 16)
            followingRestBeats = 0;
        if (tupletDivisor != 3) tupletDivisor = 1;
        augmentationDots = Math.max(0, Math.min(2, augmentationDots));
        beamCount = Math.max(0, Math.min(4, beamCount));
        if (writtenAccidental < ACCIDENTAL_FLAT || writtenAccidental > ACCIDENTAL_FROM_KEY)
            writtenAccidental = ACCIDENTAL_FROM_KEY;
        if (!Float.isFinite(unbeamedDurationBeats) || unbeamedDurationBeats < .25f
                || unbeamedDurationBeats > DURATION_WHOLE) unbeamedDurationBeats = DURATION_UNKNOWN;
    }
}
