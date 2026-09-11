// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Carries evidence of a short opening bar to the meter-aware timing pass. */
final class OpeningMeasureLayout {
    private OpeningMeasureLayout() { }

    static List<ScoreNoteEvent> mark(List<ScoreNoteEvent> notes, List<ScoreRestEvent> rests,
                                     List<MeasureRegion> measures) {
        if (measures.size()<3 || notes.isEmpty()) return notes;
        MeasureRegion first=measures.get(0);
        float height=first.bottom()-first.top(), width=first.right()-first.left();
        List<Float> followingWidths=new ArrayList<>();
        for (int i=1;i<measures.size();i++) {
            MeasureRegion other=measures.get(i);
            if (Math.abs(other.top()-first.top())>height*.25f
                    || Math.abs(other.bottom()-first.bottom())>height*.25f) break;
            if (other.left()<=first.right()) return notes;
            followingWidths.add(other.right()-other.left());
        }
        if (followingWidths.size()<2 || width<=0) return notes;
        followingWidths.sort(Float::compare);
        if (width>followingWidths.get(followingWidths.size()/2)*.5f) return notes;
        List<ScoreNoteEvent> opening=new ArrayList<>();
        for (ScoreNoteEvent note:notes) if (note.measureIndex()==0) opening.add(note);
        if (opening.isEmpty() || opening.stream().anyMatch(n -> n.positionInMeasure()<.4f
                || n.leadingRestBeats()>0 || n.followingRestBeats()>0 || n.tiedFromPrevious())
                || rests.stream().anyMatch(r -> r.measureIndex()==0)) return notes;
        List<ScoreNoteEvent> result=new ArrayList<>();
        for (ScoreNoteEvent note:notes) result.add(note.measureIndex()==0 ? note.withCompactOpening() : note);
        return List.copyOf(result);
    }
}
