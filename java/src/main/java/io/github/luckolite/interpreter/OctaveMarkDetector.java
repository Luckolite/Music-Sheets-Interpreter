// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Applies octave directions to sounding pitch while preserving written staff geometry. */
final class OctaveMarkDetector {
    private OctaveMarkDetector() { }
    static int shift(String text) {
        if(text==null)return 0;
        String s=text.toLowerCase(Locale.ROOT).replaceAll("[\\s()\\[\\].,:;_\\-–—]","");
        return switch(s) {
            case "8va","8vaa","8vaalta","ottava" -> 1;
            case "8vb","8vab","8vabassa" -> -1;
            case "15ma","15maa","15maalta" -> 2;
            case "15mb","15mab","15mabassa" -> -2;
            default -> 0;
        };
    }
    private record Span(PlayingTechniqueDetector.Staff staff,float left,float right,int shift) { }

    static List<ScoreNoteEvent> apply(List<PlayingTechniqueDetector.Word> words,
            List<PlayingTechniqueDetector.Staff> staffs,List<MeasureRegion> measures,
            List<ScoreNoteEvent> notes,byte[] gray,int width,int height) {
        if(staffs==null||staffs.isEmpty()||notes.isEmpty())return notes;
        List<PlayingTechniqueDetector.Word> combined=new ArrayList<>(words==null?List.of():words);
        combined.addAll(printedWords(gray,width,height,staffs));
        if(combined.isEmpty())return notes;
        List<Span> spans=new ArrayList<>();
        for(var word:combined) {
            int shift=shift(word.text());if(shift==0)continue;
            var owner=owner(word,staffs,shift,height);if(owner==null)continue;
            float gap=owner.gap(),left=word.left()*width-gap*.65f;
            float textRight=Math.min(word.right()*width,word.left()*width+
                    Math.max(gap*2,(word.bottom()-word.top())*height*3.5f));
            float right=dashEnd(gray,width,height,textRight,word.top()*height,word.bottom()*height,gap);
            if(right<0) {
                // An isolated octave direction applies to the nearest attack or chord only.
                float first=Float.POSITIVE_INFINITY;
                for(var note:notes)if(onStaff(note,owner,staffs,height)) {
                    float x=x(note,measures,width);
                    if(x>=left&&x<=textRight+gap*1.5f)first=Math.min(first,x);
                }
                if(!Float.isFinite(first))continue;
                left=first-gap*.35f;right=first+gap*.35f;
            }
            spans.add(new Span(owner,left,right,shift));
        }
        if(spans.isEmpty())return notes;
        List<Span> ambiguous=new ArrayList<>();
        for(var a:spans)for(var b:spans)if(a!=b&&a.staff.equals(b.staff)&&a.shift!=b.shift
                &&Math.abs(a.left-b.left)<a.staff.gap()*.75f){ambiguous.add(a);ambiguous.add(b);}
        spans.removeAll(ambiguous);
        spans.sort(Comparator.comparingDouble(Span::left));
        for(int i=0;i<spans.size();i++) {
            var a=spans.get(i);float right=a.right;
            for(int j=i+1;j<spans.size();j++) {
                var b=spans.get(j);
                if(a.staff.equals(b.staff)&&b.left>a.left+a.staff.gap()*.75f)right=Math.min(right,b.left-.01f);
            }
            if(right!=a.right)spans.set(i,new Span(a.staff,a.left,right,a.shift));
        }
        List<ScoreNoteEvent> result=new ArrayList<>(notes.size());
        for(var note:notes) {
            float x=x(note,measures,width);Span selected=null;
            for(var span:spans)if(onStaff(note,span.staff,staffs,height)&&x>=span.left&&x<=span.right) {
                if(selected==null||span.left>selected.left)selected=span;
            }
            result.add(selected==null?note:note.withOctaveShift(selected.shift));
        }
        return List.copyOf(result);
    }
    private record InkBox(int left,int top,int right,int bottom,int area) { }
    static List<PlayingTechniqueDetector.Word> printedWords(byte[] gray,int width,int height,
            List<PlayingTechniqueDetector.Staff> staffs) {
        List<PlayingTechniqueDetector.Word> words=new ArrayList<>();
        if(gray==null||gray.length!=width*height)return words;
        for(var staff:staffs)for(boolean below:new boolean[]{false,true}) {
            float gap=staff.gap();
            int top=Math.max(0,Math.round(below?staff.bottom()+gap*.3f:staff.top()-gap*9));
            int bottom=Math.min(height-1,Math.round(below?staff.bottom()+gap*9:staff.top()-gap*.3f));
            if(top>=bottom)continue;
            int h=bottom-top+1;boolean[] visited=new boolean[width*h];int[] stack=new int[width*h];
            List<InkBox> boxes=new ArrayList<>();
            for(int origin=0;origin<visited.length;origin++) {
                if(visited[origin]||(gray[top*width+origin]&255)>=165)continue;
                int count=0,size=0,left=width,right=-1,y1=h,y2=-1;stack[size++]=origin;visited[origin]=true;
                while(size>0) {
                    int at=stack[--size],x=at%width,y=at/width;count++;
                    left=Math.min(left,x);right=Math.max(right,x);y1=Math.min(y1,y);y2=Math.max(y2,y);
                    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                        int xx=x+dx,yy=y+dy;if(xx<0||xx>=width||yy<0||yy>=h)continue;
                        int next=yy*width+xx;
                        if(!visited[next]&&(gray[top*width+next]&255)<165){visited[next]=true;stack[size++]=next;}
                    }
                }
                if(count>=Math.max(4,gap*gap*.025f)&&y2-y1+1>=gap*.55f&&y2-y1+1<=gap*3.2f
                        &&right-left+1>=gap*.12f&&right-left+1<=gap*8)
                    boxes.add(new InkBox(left,top+y1,right,top+y2,count));
            }
            boxes.sort(Comparator.comparingInt(InkBox::left));boolean[] used=new boolean[boxes.size()];
            for(int i=0;i<boxes.size();i++) {
                if(used[i])continue;var box=boxes.get(i);int left=box.left,right=box.right,a=box.top,b=box.bottom;used[i]=true;
                for(int j=i+1;j<boxes.size();j++) {
                    var next=boxes.get(j);if(next.left>right+gap*.7f)break;
                    if(used[j]||next.right<left||Math.min(b,next.bottom)-Math.max(a,next.top)<gap*.3f
                            ||Math.max(right,next.right)-left>gap*8)continue;
                    used[j]=true;right=Math.max(right,next.right);a=Math.min(a,next.top);b=Math.max(b,next.bottom);
                }
                int shift=OctaveWordShapes.match(gray,width,left,a,right,b);
                if(shift==0||(below?shift>0:shift<0))continue;
                String text=switch(shift){case 1->"8va";case -1->"8vb";case 2->"15ma";default->"15mb";};
                words.add(new PlayingTechniqueDetector.Word(text,left/(float)width,a/(float)height,
                        (right+1)/(float)width,(b+1)/(float)height));
            }
        }
        return words;
    }

    private static float x(ScoreNoteEvent note,List<MeasureRegion> measures,int width) {
        var m=measures.get(note.measureIndex());return (m.left()+note.positionInMeasure()*(m.right()-m.left()))*width;
    }
    private static boolean onStaff(ScoreNoteEvent note,PlayingTechniqueDetector.Staff staff,
            List<PlayingTechniqueDetector.Staff> staffs,int height) {
        PlayingTechniqueDetector.Staff nearest=null;float best=Float.POSITIVE_INFINITY;
        for(var candidate:staffs)if(note.staffIndex()==candidate.index()&&note.staffCount()==candidate.count()) {
            float y=note.pageY()*height,distance=Math.max(0,Math.max(candidate.top()-y,y-candidate.bottom()))/candidate.gap();
            if(distance<=7&&distance<best){best=distance;nearest=candidate;}
        }
        return staff.equals(nearest);
    }
    private static PlayingTechniqueDetector.Staff owner(PlayingTechniqueDetector.Word word,
            List<PlayingTechniqueDetector.Staff> staffs,int shift,int height) {
        PlayingTechniqueDetector.Staff best=null;float distance=Float.POSITIVE_INFINITY;
        for(var staff:staffs) {
            float d=shift>0?(staff.top()-word.bottom()*height)/staff.gap()
                    :(word.top()*height-staff.bottom())/staff.gap();
            if(d<.25f||d>9||d>=distance)continue;
            distance=d;best=staff;
        }
        return best;
    }
    /** Find a horizontal chain of short printed dashes, stopping at its actual end. */
    private static float dashEnd(byte[] gray,int width,int height,float start,float top,float bottom,float gap) {
        if(gray==null||gray.length!=width*height)return -1;
        int left=Math.max(0,Math.round(start-gap*.35f));
        int y1=Math.max(0,Math.round(top-gap*.15f)),y2=Math.min(height-1,Math.round(bottom+gap*.4f));
        int best=-1;
        for(int y=y1;y<=y2;y++) {
            int first=-1,last=-1,count=0,x=left;
            while(x<width) {
                int blank=0;
                while(x<width&&(gray[y*width+x]&255)>=165){blank++;x++;}
                if(blank>gap*(count==0?2:1.6f))break;
                int a=x;while(x<width&&(gray[y*width+x]&255)<165)x++;
                int length=x-a;
                if(length<Math.max(2,gap*.18f))continue;
                if(length>gap*1.65f)break;
                if(first<0)first=a;last=x-1;count++;
            }
            if(count>=3&&last-first>=gap*3)best=Math.max(best,last);
        }
        return best<0?-1:best+gap*.55f;
    }
}
