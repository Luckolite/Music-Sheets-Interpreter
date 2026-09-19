// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Six-string numeric tablature. Frets are absolute semitone offsets, never key-signature steps. */
public final class TablatureDecoder {
    public record Fret(float x,float y,int string,int fret) { }
    public record Staff(float top,float gap,float standardTop,List<Fret> frets,List<Float> bars) { }
    private static final int[] STANDARD={64,59,55,50,45,40};
    private TablatureDecoder() { }
    public static List<Staff> detect(byte[] gray,int w,int h) {
        if(gray==null||w<64||h<64||gray.length!=(long)w*h)return List.of();
        var lines=new ArrayList<Integer>();var thickness=new ArrayList<Integer>();var strength=new ArrayList<Integer>();int start=-1,peak=0;
        for(int y=0;y<=h;y++) {
            int ink=0;if(y<h)for(int x=0;x<w;x++)if((gray[y*w+x]&255)<180)ink++;
            if(ink>w*.55){if(start<0){start=y;peak=0;}peak=Math.max(peak,ink);}
            else if(start>=0){if(y-start<Math.max(6,w/180)){lines.add((start+y-1)/2);thickness.add(y-start);strength.add(peak);}start=-1;}
        }
        var result=new ArrayList<Staff>();
        for(int i=0;i+5<lines.size();i++) {
            float gap=(lines.get(i+5)-lines.get(i))/5f;if(gap<6||gap>w*.04)continue;
            boolean regular=true;for(int n=1;n<6;n++)if(Math.abs(lines.get(i+n)-lines.get(i)-n*gap)>gap*.16f)regular=false;
            int minThickness=Integer.MAX_VALUE,maxThickness=0,minInk=w,maxInk=0;
            for(int n=0;n<6;n++){minThickness=Math.min(minThickness,thickness.get(i+n));maxThickness=Math.max(maxThickness,thickness.get(i+n));minInk=Math.min(minInk,strength.get(i+n));maxInk=Math.max(maxInk,strength.get(i+n));}
            if(!regular||maxThickness>minThickness*2+1||minInk<maxInk*.88f)continue;
            if(i>0&&Math.abs(lines.get(i)-lines.get(i-1)-gap)<gap*.16f)continue;
            if(i+6<lines.size()&&Math.abs(lines.get(i+6)-lines.get(i+5)-gap)<gap*.16f)continue;
            float top=lines.get(i);List<Fret> frets=List.of();
            float standard=-1;
            if(i>=5&&(result.isEmpty()||lines.get(i-1)>result.get(result.size()-1).top+result.get(result.size()-1).gap*6)) {
                float sg=(lines.get(i-1)-lines.get(i-5))/4f;
                boolean five=sg>=4&&top-lines.get(i-1)<gap*15;
                for(int n=1;n<5;n++)if(Math.abs(lines.get(i-5+n)-lines.get(i-5)-n*sg)>sg*.16f)five=false;
                if(five)standard=lines.get(i-5);
            }
            var bars=new ArrayList<Float>();int run=-1;
            for(int x=0;x<=w;x++) {
                int ink=0,total=0;
                if(x<w)for(int y=Math.round(top);y<=Math.round(top+5*gap);y++){total++;if((gray[y*w+x]&255)<185)ink++;}
                if(total>0&&ink>=total*.98){if(run<0)run=x;}
                else if(run>=0){float center=(run+x-1)*.5f;if(bars.isEmpty()||center-bars.get(bars.size()-1)>gap*.6f)bars.add(center);run=-1;}
            }
            result.add(new Staff(top,gap,standard,List.copyOf(frets),List.copyOf(bars)));i+=5;
        }
        return List.copyOf(result);
    }
    public record Word(String text,float left,float top,float right,float bottom) {
        public Word {
            Objects.requireNonNull(text);
            if(!Float.isFinite(left)||!Float.isFinite(top)||!Float.isFinite(right)||!Float.isFinite(bottom)
                    ||left<0||top<0||right>1||bottom>1||left>=right||top>=bottom)throw new IllegalArgumentException("Invalid tab OCR box");
        }
    }
    /** OCR boxes are normalized page coordinates. Unknown glyphs never become open strings. */
    public static List<Staff> withWords(List<Staff> staffs,List<Word> words,int w,int h) {
        var result=new ArrayList<Staff>();
        for(Staff staff:staffs) {
            var frets=new ArrayList<Fret>();
            for(Word word:words) {
                float y=(word.top+word.bottom)*.5f*h;
                int string=Math.round((y-staff.top)/staff.gap);
                if(string<0||string>=6||Math.abs(y-staff.top-string*staff.gap)>staff.gap*.47f)continue;
                String text=word.text.trim();
                if(!text.matches("[0-9xX]+(?:[/\\\\hHpP~][0-9xX]+)*"))continue;
                var matcher=java.util.regex.Pattern.compile("[0-9]{1,2}|[xX]").matcher(text);
                while(matcher.find()) {
                    if(matcher.end()<text.length()&&Character.isDigit(text.charAt(matcher.end())))break;
                    int fret=matcher.group().equalsIgnoreCase("x")?-1:Integer.parseInt(matcher.group());
                    if(fret>36)continue;
                    float x=(word.left+(word.right-word.left)*(matcher.start()+matcher.end())/(2f*text.length()))*w;
                    if(staff.bars.stream().noneMatch(bar->Math.abs(bar-x)<staff.gap*.25f)
                            &&frets.stream().noneMatch(f->f.string==string&&Math.abs(f.x-x)<staff.gap*.3f))
                        frets.add(new Fret(x,staff.top+string*staff.gap,string,fret));
                }
            }
            frets.sort(Comparator.comparingDouble(Fret::x));
            result.add(new Staff(staff.top,staff.gap,staff.standardTop,List.copyOf(frets),staff.bars));
        }
        return List.copyOf(result);
    }
    public static byte[] withoutTabs(byte[] pixels,int w,int h,List<Staff> tabs,boolean grayscale) {
        if(tabs.isEmpty())return pixels;
        byte[] result=pixels.clone();
        for(Staff tab:tabs)for(int y=Math.max(0,Math.round(tab.top-tab.gap*.65f));y<=Math.min(h-1,Math.round(tab.top+tab.gap*5.65f));y++)
            Arrays.fill(result,y*w,(y+1)*w,(byte)(grayscale?255:0));
        return result;
    }
    public static int midi(int string,int fret,int[] tuning,int capo) {
        if(tuning.length!=6||string<0||string>=6||fret<0||fret>36||capo<0||capo>12)throw new IllegalArgumentException("Invalid six-string tab tuning/fret/capo");
        int result=tuning[string]+fret+capo;if(result<0||result>127)throw new IllegalArgumentException("Tab pitch outside MIDI range");return result;
    }
    public static List<MeasureRegion> reconcileMeasures(List<MeasureRegion> input,List<Staff> tabs,int w,int h) {
        if(tabs.isEmpty())return input;
        var measures=new ArrayList<>(input);
        for(Staff tab:tabs) {
            if(tab.standardTop<0||tab.bars.size()<2)continue;
            var old=new ArrayList<MeasureRegion>();
            for(var m:measures)if(tab.standardTop/h>=m.top()-tab.gap/h*2&&tab.standardTop/h<=m.bottom()&&m.bottom()*h<tab.top)old.add(m);
            if(old.isEmpty())continue;
            float left=old.stream().map(MeasureRegion::left).min(Float::compare).orElse(0f),right=old.stream().map(MeasureRegion::right).max(Float::compare).orElse(1f);
            var cuts=new ArrayList<Float>();cuts.add(left);
            for(float x:tab.bars)if(x/w>left+tab.gap/w*2&&x/w<right-tab.gap/w*2)cuts.add(x/w);
            cuts.add(right);int insertion=measures.indexOf(old.get(0));measures.removeAll(old);
            for(int i=0;i+1<cuts.size();i++)measures.add(insertion++,new MeasureRegion(cuts.get(i),cuts.get(i+1),old.get(0).top(),old.get(0).bottom()));
        }
        return List.copyOf(measures);
    }
    public static ScorePageInterpretation apply(ScorePageInterpretation score,List<Staff> tabs,int w,int h) {
        return apply(score,tabs,w,h,STANDARD,0);
    }
    public static ScorePageInterpretation apply(ScorePageInterpretation score,List<Staff> tabs,int w,int h,int[] tuning,int capo) {
        if(tabs.isEmpty())return score;
        for(int string=0;string<6;string++)midi(string,0,tuning,capo);
        var notes=new ArrayList<>(score.notes());var measures=new ArrayList<>(score.measures());
        for(Staff tab:tabs) {
            if(tab.standardTop<0) {
                // A simple tab without written rhythm has pitches but no provable durations.
                // Preserve unknown duration so callers can identify estimated playback.
                if(!score.measures().isEmpty()||tab.frets.isEmpty())continue;
                var bars=new ArrayList<>(tab.bars);
                if(bars.size()<2){bars.clear();bars.add(tab.frets.get(0).x-tab.gap);bars.add(tab.frets.get(tab.frets.size()-1).x+tab.gap);}
                for(int bi=0;bi+1<bars.size();bi++) {
                    float left=bars.get(bi),right=bars.get(bi+1);if(right-left<tab.gap*2)continue;
                    int bar=measures.size();measures.add(new MeasureRegion(Math.max(0,left/w),Math.min(1,right/w),Math.max(0,(tab.top-tab.gap)/h),Math.min(1,(tab.top+tab.gap*6)/h)));
                    for(Fret f:tab.frets)if(f.fret>=0&&f.x>left&&f.x<right) {
                        var n=new ScoreNoteEvent(bar,(f.x-left)/(right-left),0,0,1,f.y/h,false,0,0,0,0);
                        notes.add(pitched(n,midi(f.string,f.fret,tuning,capo)));
                    }
                }
                continue;
            }
            var indices=new ArrayList<Integer>();int down=0,same=0;
            for(int i=0;i<score.notes().size();i++) {
                var n=score.notes().get(i);var m=score.measures().get(n.measureIndex());
                if(tab.standardTop/h<m.top()-tab.gap/h*2||tab.standardTop/h>m.bottom()||m.bottom()*h>=tab.top+tab.gap)continue;
                indices.add(i);float x=(m.left()+n.positionInMeasure()*(m.right()-m.left()))*w;
                int printed=printedMidi(n,score);
                boolean lowerMatch=false,sameMatch=false;
                for(Fret f:tab.frets)if(f.fret>=0&&Math.abs(f.x-x)<tab.gap*.65f) {
                    int sounding=midi(f.string,f.fret,tuning,capo);lowerMatch|=printed-sounding==12;sameMatch|=printed==sounding;
                }
                if(lowerMatch)down++;if(sameMatch)same++;
            }
            // Paired notation owns rhythm and note count. Require repeated cross-notation
            // agreement before resolving the guitar's commonly omitted octave transposition.
            if(down>=3&&down>same)for(int index:indices) {
                var n=score.notes().get(index);notes.set(index,n.withOctaveShift(Math.max(-2,n.octaveShift()-1)));
            }
        }
        var rests=new ArrayList<>(score.rests());
        var handled=new HashSet<String>();
        for(Staff tab:tabs)if(tab.standardTop>=0)for(Fret fret:tab.frets)if(fret.fret<0) {
            if(tab.frets.stream().anyMatch(f->f.fret>=0&&Math.abs(f.x-fret.x)<tab.gap*.65f))continue;
            if(tab.frets.stream().filter(f->f.fret<0&&Math.abs(f.x-fret.x)<tab.gap*.65f).map(Fret::string).distinct().count()<3)continue;
            ScoreNoteEvent target=null;float closest=tab.gap*.65f;
            for(var n:notes) {
                var m=measures.get(n.measureIndex());
                if(tab.standardTop/h<m.top()-tab.gap/h*2||tab.standardTop/h>m.bottom()||m.bottom()*h>=tab.top)continue;
                float dx=Math.abs((m.left()+n.positionInMeasure()*(m.right()-m.left()))*w-fret.x);
                if(dx<closest){closest=dx;target=n;}
            }
            if(target==null)continue;
            var m=measures.get(target.measureIndex());float tolerance=tab.gap*.65f/((m.right()-m.left())*w);
            String key=target.measureIndex()+":"+Math.round(target.positionInMeasure()*1000);
            if(!handled.add(key))continue;
            float duration=target.beamCount()>0?1f/(1<<Math.max(1,Math.min(3,target.beamCount()))):target.unbeamedDurationBeats();
            if(duration<=0)continue;
            duration*=target.durationScale();if(target.augmentationDots()>0)duration*=2-1f/(1<<target.augmentationDots());
            final int bar=target.measureIndex();final float position=target.positionInMeasure();
            notes.removeIf(n->n.measureIndex()==bar&&Math.abs(n.positionInMeasure()-position)<tolerance);
            float previous=-1,next=2;
            for(var n:notes)if(n.measureIndex()==bar){if(n.positionInMeasure()<position)previous=Math.max(previous,n.positionInMeasure());else next=Math.min(next,n.positionInMeasure());}
            for(int i=0;i<notes.size();i++) {
                var n=notes.get(i);if(n.measureIndex()!=bar)continue;
                if(previous>=0&&Math.abs(n.positionInMeasure()-previous)<tolerance)notes.set(i,withRestAfter(n,duration));
                else if(previous<0&&Math.abs(n.positionInMeasure()-next)<tolerance)notes.set(i,n.withLeadingRest(n.leadingRestBeats()+duration));
            }
            rests.add(new ScoreRestEvent(bar,position,target.pageY(),tab.gap/h,target.staffIndex(),target.staffCount(),duration));
        }
        notes.sort(Comparator.comparingInt(ScoreNoteEvent::measureIndex).thenComparingDouble(ScoreNoteEvent::positionInMeasure).thenComparingInt(ScoreNoteEvent::staffStep));
        return new ScorePageInterpretation(measures,notes,score.firstMeasureNumber(),score.keyChanges(),score.tempoChanges(),score.meterChanges(),rests,score.techniqueChanges(),score.dynamicChanges());
    }
    private static ScoreNoteEvent withRestAfter(ScoreNoteEvent n,float beats) {
        return new ScoreNoteEvent(n.measureIndex(),n.positionInMeasure(),n.staffStep(),n.staffIndex(),n.staffCount(),n.pageY(),n.tiedFromPrevious(),n.augmentationDots(),n.beamCount(),n.writtenAccidental(),n.unbeamedDurationBeats(),n.tupletDivisor(),n.followingRestBeats()+beats,n.articulations(),n.clefBottomDiatonic(),n.crossStaffBeam(),n.leadingRestBeats(),n.compactOpening(),n.octaveShift());
    }
    private static int printedMidi(ScoreNoteEvent n,ScorePageInterpretation score) {
        int clef=n.clefBottomDiatonic();if(clef==ScoreNoteEvent.CLEF_UNKNOWN)clef=ScoreNoteEvent.CLEF_TREBLE;
        int pitch=clef+n.staffStep(),letter=Math.floorMod(pitch,7),acc=n.writtenAccidental();
        if(acc==ScoreNoteEvent.ACCIDENTAL_FROM_KEY) {
            int key=0;for(var k:score.keyChanges())if(k.measureIndex()<=n.measureIndex())key=k.fifths();
            acc=0;int[] order=key>=0?new int[]{3,0,4,1,5,2,6}:new int[]{6,2,5,1,4,0,3};
            for(int i=0;i<Math.abs(key);i++)if(order[i]==letter)acc=key>0?1:-1;
        }
        return (Math.floorDiv(pitch,7)+1+n.octaveShift())*12+new int[]{0,2,4,5,7,9,11}[letter]+ScoreNoteEvent.accidentalSemitones(acc);
    }
    private static ScoreNoteEvent pitched(ScoreNoteEvent n,int midi) {
        int pc=midi%12;int[] letters={0,0,1,1,2,3,3,4,4,5,5,6},acc={0,1,0,1,0,0,1,0,1,0,1,0};
        int step=(midi/12-1)*7+letters[pc]-ScoreNoteEvent.CLEF_TREBLE;
        return new ScoreNoteEvent(n.measureIndex(),n.positionInMeasure(),step,n.staffIndex(),n.staffCount(),n.pageY(),n.tiedFromPrevious(),n.augmentationDots(),n.beamCount(),acc[pc],n.unbeamedDurationBeats(),n.tupletDivisor(),n.followingRestBeats(),n.articulations(),ScoreNoteEvent.CLEF_TREBLE,n.crossStaffBeam(),n.leadingRestBeats(),n.compactOpening(),0);
    }
}
