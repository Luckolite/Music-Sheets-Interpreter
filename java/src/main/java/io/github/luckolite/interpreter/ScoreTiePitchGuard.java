// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Keeps a decoded tie only when its earlier same-pitch note is present. */
final class ScoreTiePitchGuard {
    private ScoreTiePitchGuard() { }

    static List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes,List<ScoreKeyChange> keys) {
        List<ScoreNoteEvent> result=new ArrayList<>(notes);
        for(int i=0;i<notes.size();i++) {
            ScoreNoteEvent current=notes.get(i);
            if(!current.tiedFromPrevious()||current.measureIndex()==0)continue;
            int pitch=midi(current,keys);
            if(pitch==Integer.MIN_VALUE && !explicitPitchChange(notes,i))continue;
            boolean prior=false;
            for(int j=i-1;pitch!=Integer.MIN_VALUE && j>=0;j--) {
                ScoreNoteEvent earlier=notes.get(j);
                if(current.measureIndex()-earlier.measureIndex()>1)break;
                if(!sameContinuingStaff(earlier,current))continue;
                if(earlier.measureIndex()==current.measureIndex()
                        &&earlier.positionInMeasure()>=current.positionInMeasure()-.018f)continue;
                if(midi(earlier,keys)==pitch){prior=true;break;}
            }
            if(prior)continue;
            result.set(i,new ScoreNoteEvent(current.measureIndex(),current.positionInMeasure(),
                    current.staffStep(),current.staffIndex(),current.staffCount(),current.pageY(),
                    false,current.augmentationDots(),current.beamCount(),current.writtenAccidental(),
                    current.unbeamedDurationBeats(),current.tupletDivisor(),current.followingRestBeats(),
                    current.articulations(),current.clefBottomDiatonic(),current.crossStaffBeam(),
                    current.leadingRestBeats(),current.compactOpening(),current.octaveShift()));
        }
        return result;
    }

    /** Two different printed accidentals on one staff position cannot form a tie,
     * even when the caller has not supplied the page's inherited key. */
    private static boolean explicitPitchChange(List<ScoreNoteEvent> notes,int index) {
        ScoreNoteEvent current=notes.get(index);
        if(current.writtenAccidental()==ScoreNoteEvent.ACCIDENTAL_FROM_KEY)return false;
        for(int j=index-1;j>=0;j--) {
            ScoreNoteEvent earlier=notes.get(j);
            if(current.measureIndex()-earlier.measureIndex()>1)break;
            if(!sameContinuingStaff(earlier,current)
                    ||earlier.diatonicPitchIdentity()!=current.diatonicPitchIdentity())continue;
            if(earlier.measureIndex()==current.measureIndex()
                    && earlier.positionInMeasure()>=current.positionInMeasure()-.018f)continue;
            return earlier.writtenAccidental()!=ScoreNoteEvent.ACCIDENTAL_FROM_KEY
                    && earlier.writtenAccidental()!=current.writtenAccidental();
        }
        return false;
    }

    /** A hidden lower staff does not change the identity of the continuing top staff.
     * Only adjacent systems with the same explicit clef qualify; recognition still
     * requires matching pitches and returning arcs at both printed endpoints. */
    static boolean sameContinuingStaff(ScoreNoteEvent earlier,ScoreNoteEvent current) {
        if(earlier.staffIndex()!=current.staffIndex())return false;
        if(earlier.staffCount()==current.staffCount())return true;
        return earlier.staffIndex()==0
                &&Math.min(earlier.staffCount(),current.staffCount())==1
                &&Math.max(earlier.staffCount(),current.staffCount())==2
                &&current.measureIndex()==earlier.measureIndex()+1
                &&current.pageY()-earlier.pageY()>.04f
                &&current.positionInMeasure()<.58f
                &&earlier.clefBottomDiatonic()!=ScoreNoteEvent.CLEF_UNKNOWN
                &&earlier.clefBottomDiatonic()==current.clefBottomDiatonic();
    }

    private static int midi(ScoreNoteEvent note,List<ScoreKeyChange> keys) {
        if(note.clefBottomDiatonic()==ScoreNoteEvent.CLEF_UNKNOWN)return Integer.MIN_VALUE;
        int fifths=Integer.MIN_VALUE;
        for(ScoreKeyChange key:keys)if(key.measureIndex()<=note.measureIndex())fifths=key.fifths();
        if(fifths==Integer.MIN_VALUE)return Integer.MIN_VALUE;
        int diatonic=note.diatonicPitchIdentity();
        int letter=Math.floorMod(diatonic,7),octave=Math.floorDiv(diatonic,7);
        int accidental=note.writtenAccidental();
        if(accidental==ScoreNoteEvent.ACCIDENTAL_FROM_KEY) {
            accidental=0;
            int[] order=fifths>=0?new int[]{3,0,4,1,5,2,6}:new int[]{6,2,5,1,4,0,3};
            for(int k=0;k<Math.abs(fifths);k++)if(order[k]==letter)accidental=fifths>0?1:-1;
        }
        return (octave+1+note.octaveShift())*12+new int[]{0,2,4,5,7,9,11}[letter]
                +ScoreNoteEvent.accidentalSemitones(accidental);
    }
}
