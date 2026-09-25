// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Cross-checks above-staff false heads against independently recognized fingering text. */
final class FingeringAnnotationFilter {
    private FingeringAnnotationFilter() { }
    static List<ScoreNoteEvent> apply(List<PlayingTechniqueDetector.Word> words,
            List<PlayingTechniqueDetector.Staff> staffs,List<MeasureRegion> measures,
            List<ScoreNoteEvent> notes,byte[] gray,int width,int height) {
        if(words==null||words.isEmpty()||gray==null||width<=0||height<=0||gray.length!=(long)width*height)return notes;
        var result=new ArrayList<ScoreNoteEvent>();
        for(var note:notes) {
            if(note.measureIndex()<0||note.measureIndex()>=measures.size()||note.staffStep()<8
                    ||(note.articulations()&NoteOrnament.GRACE)!=0){result.add(note);continue;}
            var measure=measures.get(note.measureIndex());float x=(measure.left()+note.positionInMeasure()*(measure.right()-measure.left()))*width,y=note.pageY()*height;
            boolean annotation=false;
            for(var word:words) {
                if(!isFingering(word.text()))continue;
                float l=word.left()*width,r=word.right()*width,t=word.top()*height,b=word.bottom()*height;
                if(!Float.isFinite(l+r+t+b)||x<l||x>r||y<t||y>b)continue;
                PlayingTechniqueDetector.Staff owner=null;float distance=Float.MAX_VALUE;
                for(var staff:staffs) {
                    if(staff.index()!=note.staffIndex()||staff.count()!=note.staffCount()||staff.gap()<5)continue;
                    float dy=(staff.top()-b)/staff.gap();
                    if(dy<-.4f||dy>5||Math.abs(dy)>=distance)continue;
                    owner=staff;distance=Math.abs(dy);
                }
                if(owner==null)continue;float gap=owner.gap();
                if(b-t<gap*1.15f||b-t>gap*5.5f||r-l<gap*.45f||r-l>gap*7)continue;
                if(note.staffStep()>=10&&printedLedgerAcrossWord(gray,width,height,l,r,y,gap))continue;
                annotation=true;break;
            }
            if(!annotation)result.add(note);
        }
        return result.size()==notes.size()?notes:List.copyOf(result);
    }
    static boolean isFingering(String text) {
        if(text==null)return false;
        String compact=text.trim().replaceAll("\\s+","").toUpperCase(Locale.ROOT);
        return compact.matches("[LH]?[1-5]")||compact.matches("[LRH]{2,3}");
    }
    private static boolean printedLedgerAcrossWord(byte[] gray,int width,int height,float left,float right,float y,float gap) {
        for(int yy=Math.max(1,Math.round(y-gap*.3f));yy<=Math.min(height-2,Math.round(y+gap*.3f));yy++) {
            boolean both=true;
            for(int side:new int[]{-1,1}) {
                int hits=0,total=0,center=Math.round(side<0?left:right);
                for(int d=1;d<=Math.round(gap*.65f);d++) {
                    int x=center+side*d;if(x<0||x>=width){both=false;break;}
                    total++;if((gray[yy*width+x]&255)<190)hits++;
                }
                if(total<3||hits<total*.8f)both=false;
            }
            if(both)return true;
        }
        return false;
    }
}
