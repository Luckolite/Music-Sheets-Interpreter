// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Anchors explicit navigation words and already-verified glyphs to printed bar boundaries. */
final class ScoreNavigationDetector {
    record Glyph(ScorePlaybackDirection.Kind kind,float centerX,float top,float bottom) { }
    private ScoreNavigationDetector() { }
    static ScorePlaybackDirection.Kind kind(String text) {
        if(text==null)return null;
        if(text.trim().equals("𝄋"))return ScorePlaybackDirection.Kind.SEGNO;
        if(text.trim().equals("𝄌"))return ScorePlaybackDirection.Kind.CODA;
        String clean=text.toLowerCase(Locale.ROOT).replaceAll("[\\s.,:;]","");
        return switch(clean){case "dsalcoda","dalsegnoalcoda"->ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA;
            case "tocoda"->ScorePlaybackDirection.Kind.TO_CODA;
            case "coda"->ScorePlaybackDirection.Kind.CODA;
            case "segno"->ScorePlaybackDirection.Kind.SEGNO;default->null;};
    }
    static List<ScorePlaybackDirection> detect(List<PlayingTechniqueDetector.Word> input,
            List<Glyph> glyphs,List<PlayingTechniqueDetector.Staff> staffs,List<MeasureRegion> measures,
            int width,int height) {
        if(width<=0||height<=0||staffs==null||measures==null)return List.of();
        var marks=new ArrayList<Glyph>();if(glyphs!=null)marks.addAll(glyphs);
        var words=new ArrayList<PlayingTechniqueDetector.Word>();if(input!=null)words.addAll(input);
        words.sort(Comparator.comparingDouble(PlayingTechniqueDetector.Word::top).thenComparingDouble(PlayingTechniqueDetector.Word::left));
        for(var word:words) {
            var value=kind(word.text());if(value!=null)marks.add(new Glyph(value,(word.left()+word.right())*.5f,word.top(),word.bottom()));
            // OCR may split D.S., al and Coda. Join only immediate same-line neighbors.
            var line=new ArrayList<PlayingTechniqueDetector.Word>();line.add(word);
            for(var next:words)if(next!=word&&next.left()>=word.right()
                    &&Math.abs((next.top()+next.bottom()-word.top()-word.bottom())*.5f)
                        <=Math.max(next.bottom()-next.top(),word.bottom()-word.top())*.5f)line.add(next);
            line.sort(Comparator.comparingDouble(PlayingTechniqueDetector.Word::left));
            String text=word.text();float right=word.right(),bottom=word.bottom();
            for(int i=1;i<Math.min(5,line.size());i++) {
                var next=line.get(i);float h=Math.max(bottom-word.top(),next.bottom()-next.top());
                if((next.left()-right)*width>h*height*1.3f)break;
                text+=" "+next.text();right=next.right();bottom=Math.max(bottom,next.bottom());
                value=kind(text);if(value!=null)marks.add(new Glyph(value,(word.left()+right)*.5f,word.top(),bottom));
            }
        }
        // A standalone Coda token within 'To Coda' or 'D.S. al Coda' is not a destination.
        marks.removeIf(mark->mark.kind()==ScorePlaybackDirection.Kind.CODA&&marks.stream().anyMatch(other->
                other!=mark&&(other.kind()==ScorePlaybackDirection.Kind.TO_CODA||other.kind()==ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA)
                &&Math.abs(mark.bottom()-other.bottom())*height<Math.max(3,(mark.bottom()-mark.top())*height)
                &&Math.abs(mark.centerX()-other.centerX())*width<(mark.bottom()-mark.top())*height*5));
        var result=new ArrayList<ScorePlaybackDirection>();
        for(var mark:marks) {
            PlayingTechniqueDetector.Staff owner=null;float best=Float.MAX_VALUE;
            for(var staff:staffs) {
                float distance=(staff.top()-mark.bottom()*height)/staff.gap();
                if(distance< -1.2f||distance>6)continue;
                if(Math.abs(distance)<best){best=Math.abs(distance);owner=staff;}
            }
            if(owner==null)continue;
            int picked=-1,firstOnStaff=-1;float nearest=Float.MAX_VALUE;
            float center=(owner.top()+owner.bottom())*.5f/height;
            for(int m=0;m<measures.size();m++) {
                var bar=measures.get(m);
                if(center<bar.top()-owner.gap()/height||center>bar.bottom()+owner.gap()/height)continue;
                if(firstOnStaff<0)firstOnStaff=m;
                float distance=mark.centerX()<bar.left()?bar.left()-mark.centerX():mark.centerX()>bar.right()?mark.centerX()-bar.right():0;
                if(distance<nearest){nearest=distance;picked=m;}
            }
            boolean outgoing=mark.kind()==ScorePlaybackDirection.Kind.TO_CODA||mark.kind()==ScorePlaybackDirection.Kind.DAL_SEGNO_AL_CODA;
            // A destination may precede the clef and a full seven-accidental signature.
            float headerAllowance=!outgoing&&picked==firstOnStaff?16:8;
            if(picked<0||nearest*width>owner.gap()*headerAllowance)continue;
            // Expanded multi-rests share a printed rectangle. Outgoing directions
            // occur after its final logical bar, not after its first repetition.
            if(outgoing)while(picked+1<measures.size()&&measures.get(picked).equals(measures.get(picked+1)))picked++;
            var direction=new ScorePlaybackDirection(picked+(outgoing?1:0),mark.kind());
            if(!result.contains(direction))result.add(direction);
        }
        result.sort(Comparator.comparingInt(ScorePlaybackDirection::measureBoundary).thenComparing(d->d.kind().ordinal()));
        return List.copyOf(result);
    }
}
