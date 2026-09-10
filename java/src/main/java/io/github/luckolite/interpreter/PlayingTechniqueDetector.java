// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class PlayingTechniqueDetector {
    record Staff(float top,float bottom,float gap,int index,int count) { }
    record Word(String text,float left,float top,float right,float bottom) { }
    private PlayingTechniqueDetector() { }
    static int technique(String word) {
        if(word==null)return -1;
        String clean=word.trim().toLowerCase(Locale.ROOT).replaceAll("^[.,:;]+|[.,:;]+$","");
        return switch(clean){case "pizz","pizzicato"->ScoreTechniqueChange.PIZZICATO;
            case "arco"->ScoreTechniqueChange.ARCO;
            case "cantabile"->ScoreTechniqueChange.CANTABILE;
            case "sostenuto"->ScoreTechniqueChange.SOSTENUTO;
            case "marcato"->ScoreTechniqueChange.MARCATO;
            case "ordinario","ord"->ScoreTechniqueChange.ORDINARIO;default->-1;};
    }
    static List<ScoreTechniqueChange> detect(List<Word> words,List<Staff> staffs,
                                            List<MeasureRegion> measures,List<ScoreNoteEvent> notes,int width,int height) {
        List<ScoreTechniqueChange> result=new ArrayList<>();
        for(Word word:words) {
            int technique=technique(word.text);if(technique<0)continue;
            Staff owner=null;float best=Float.MAX_VALUE;
            for(Staff staff:staffs) {
                float distance=(staff.top-word.bottom*height)/staff.gap;
                if(distance<-.6f||distance>4.2f)continue;
                if(Math.abs(distance)<best){best=Math.abs(distance);owner=staff;}
            }
            if(owner==null)continue;
            int measure=-1;float closest=Float.MAX_VALUE;
            for(int m=0;m<measures.size();m++) {
                var region=measures.get(m);float center=(owner.top+owner.bottom)*.5f/height;
                if(center<region.top()-owner.gap/height||center>region.bottom()+owner.gap/height)continue;
                if(word.left>region.right()+owner.gap/width*.5f)continue;
                float distance=Math.max(0,region.left()-word.left);
                if(distance<closest){closest=distance;measure=m;}
            }
            if(measure<0||closest>owner.gap/width*5)continue;
            var region=measures.get(measure);
            float position=Math.max(0,Math.min(1,(word.left-region.left())/(region.right()-region.left())));
            // Engravers center labels slightly to the right of the first affected head.
            // Snap to that head only within one staff gap; otherwise retain the printed slot.
            float nearest=owner.gap/width/(region.right()-region.left());
            float printedPosition=position;
            for(var note:notes)if(note.measureIndex()==measure&&note.staffIndex()==owner.index) {
                float d=Math.abs(note.positionInMeasure()-printedPosition);
                if(d<=nearest){nearest=d;position=note.positionInMeasure();}
            }
            var change=new ScoreTechniqueChange(measure,position,owner.index,owner.count,technique);
            boolean duplicate=false;
            for(var prior:result)if(prior.measureIndex()==measure&&prior.staffIndex()==owner.index
                    &&prior.technique()==technique&&Math.abs(prior.positionInMeasure()-position)<.03f)duplicate=true;
            if(!duplicate)result.add(change);
        }
        result.sort(Comparator.comparingInt(ScoreTechniqueChange::measureIndex)
                .thenComparingDouble(ScoreTechniqueChange::positionInMeasure).thenComparingInt(ScoreTechniqueChange::staffIndex));
        return List.copyOf(result);
    }
}
