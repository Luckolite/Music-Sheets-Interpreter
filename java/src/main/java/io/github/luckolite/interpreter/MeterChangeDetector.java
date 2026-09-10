// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Bounded crops of stacked signature glyphs, including signatures inside a system. */
final class MeterChangeDetector {
    record Crop(int left, int top, int right, int bottom, int firstLine, float gap) { }

    /** Resolve contradictory staff copies only when two complete written bars support one reading. */
    static ScoreMeterChange resolveConflict(java.util.Set<ScoreMeterChange> choices,
            List<ScoreNoteEvent> notes,int nextChange) {
        ScoreMeterChange winner=null;
        for(var choice:choices) {
            int supportedBars=0;
            for(int bar=choice.measureIndex();bar<Math.min(nextChange,choice.measureIndex()+3);bar++) {
                java.util.Map<Integer,List<ScoreNoteEvent>> lanes=new java.util.HashMap<>();
                for(var note:notes)if(note.measureIndex()==bar)
                    lanes.computeIfAbsent(note.staffCount()*16+note.staffIndex(),ignored->new ArrayList<>()).add(note);
                boolean supported=false;
                for(var lane:lanes.values()) {
                    lane.sort(java.util.Comparator.comparingDouble(ScoreNoteEvent::positionInMeasure));
                    double total=0,duration=0;float position=-1;boolean reliable=true;
                    for(var note:lane) {
                        if(note.crossStaffBeam() || (note.beamCount()==0&&note.unbeamedDurationBeats()==0)) {reliable=false;break;}
                        if(position<0 || note.positionInMeasure()-position>.018f) {
                            total+=duration;duration=0;position=note.positionInMeasure();
                        }
                        duration=Math.max(duration,ScoreNoteTiming.writtenDurationBeats(note)
                                +note.followingRestBeats()+note.leadingRestBeats());
                    }
                    if(reliable && Math.abs(total+duration-choice.quarterBeats())<.01)supported=true;
                }
                if(supported)supportedBars++;
            }
            if(supportedBars>=2) {
                if(winner!=null)return null; // Equal-duration alternatives such as 3/4 and 6/8 stay ambiguous.
                winner=choice;
            }
        }
        return winner;
    }

    static List<Crop> candidates(byte[] labels, byte[] gray, int width, int height) {
        List<Crop> result = new ArrayList<>();
        if (labels == null || gray == null || labels.length != width*height || gray.length != width*height)
            return result;
        for (RawStaffLineDetector.StaffLines staff : RawStaffLineDetector.detect(gray,width,height)) {
            float gap=staff.gap();
            int top=Math.max(0,staff.top()), bottom=Math.min(height-1,staff.bottom());
            int radius=Math.max(1,Math.round(gap*.12f));
            int[] ink=new int[width];
            for(int y=top;y<=bottom;y++) {
                boolean line=false;
                for(int row:staff.rows()) if(Math.abs(y-row)<=radius) {line=true;break;}
                if(line) continue;
                for(int x=0;x<width;x++) if((gray[y*width+x]&255)<155) ink[x]++;
            }
            int maxBlank=Math.max(1,Math.round(gap*.22f));
            for(int x=0;x<width;x++) {
                if(ink[x]<2) continue;
                int left=x,last=x,blanks=0;
                for(;x<width;x++) {
                    if(ink[x]>=2) {last=x;blanks=0;}
                    else if(++blanks>maxBlank) break;
                }
                int span=last-left+1;
                if(span<gap*.5f || span>gap*3.2f) continue;
                int key=0,head=0,upper=0,lower=0;
                int minY=height,maxY=0;
                for(int y=top;y<=bottom;y++) for(int xx=left;xx<=last;xx++) {
                    byte label=labels[y*width+xx];
                    if(label==OmrMeasurePostProcessor.CLEF_OR_KEY) key++;
                    if(label==OmrMeasurePostProcessor.NOTEHEAD) head++;
                    boolean onLine=false;
                    for(int row:staff.rows()) if(Math.abs(y-row)<=radius) {onLine=true;break;}
                    if(!onLine && (gray[y*width+xx]&255)<155) {
                        minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                        if(y<top+gap*1.8f)upper++;
                        if(y>top+gap*2.2f)lower++;
                    }
                }
                // HOMR commonly leaves signature digits as background/rest and occasionally
                // labels a tiny part of an "8" as a head. Use raw stacked-glyph evidence; the
                // full-sized semantic head veto still excludes actual notes/chords.
                if(head>gap*gap*.20f || upper<gap*2 || lower<gap*2
                        || maxY-minY<gap*3.1f) continue;
                int pad=Math.max(2,Math.round(gap*.18f));
                result.add(new Crop(Math.max(0,left-pad),Math.max(0,top-pad),
                        Math.min(width,last+pad+1),Math.min(height,bottom+pad+1),staff.top(),gap));
                if(result.size()>=48) return List.copyOf(result);
            }
        }
        return List.copyOf(result);
    }

    /** Signature precedes the first note of its bar; repeated multi-rest rectangles use the first. */
    static int followingMeasure(Crop crop, int width, int height, List<MeasureRegion> measures) {
        float center=(crop.left()+crop.right())*.5f/width;
        float y=(crop.top()+crop.bottom())*.5f/height;
        int best=-1;
        float distance=Float.MAX_VALUE;
        for(int i=0;i<measures.size();i++) {
            MeasureRegion region=measures.get(i);
            if(y<region.top() || y>region.bottom() || center>region.right()) continue;
            float delta=Math.abs(center-region.left());
            if(center>=region.left() && center<=region.right()) delta=0;
            if(delta>crop.gap()*7/width || delta>=distance) continue;
            best=i;distance=delta;
        }
        return best;
    }

    static boolean precedesNotes(Crop crop,int width,int height,int measure,
            List<MeasureRegion> measures,List<ScoreNoteEvent> notes) {
        MeasureRegion region=measures.get(measure);
        for(var note:notes) {
            if(note.measureIndex()!=measure)continue;
            float y=note.pageY()*height;
            if(y<crop.top()-crop.gap()*2||y>crop.bottom()+crop.gap()*2)continue;
            float x=(region.left()+note.positionInMeasure()*(region.right()-region.left()))*width;
            if(x<crop.left()-crop.gap()*.2f)return false;
        }
        return true;
    }
}
