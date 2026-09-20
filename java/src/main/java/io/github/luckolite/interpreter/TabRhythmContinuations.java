// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Rhythmic tab continuations require both a detached stem and a visible connecting tie. */
public final class TabRhythmContinuations {
 private TabRhythmContinuations() {}
 public static List<TablatureDecoder.Staff> apply(List<TablatureDecoder.Staff> tabs,byte[] gray,int w,int h) {
  byte[] clean=TabFretRaster.clean(gray,w,h,tabs);var out=new ArrayList<TablatureDecoder.Staff>();
  for(var tab:tabs) {
   if(tab.standardTop()>=0||tab.frets().isEmpty()){out.add(tab);continue;}
   var slots=new ArrayList<Float>();int start=-1;
   for(int x=0;x<=w;x++) {
    int longest=0,run=0;if(x<w)for(int y=Math.max(0,Math.round(tab.bottom()+tab.gap()*.45f));y<Math.min(h,Math.round(tab.bottom()+tab.gap()*3.1f));y++){
     if((gray[y*w+x]&255)<180){run++;longest=Math.max(longest,run);}else run=0;
    }
    boolean stem=longest>=tab.gap()*.8f;if(stem&&start<0)start=x;
    if(!stem&&start>=0){if(x-start<tab.gap()*.25f)slots.add((start+x-1)*.5f);start=-1;}
   }
   // The curved end of a flag is not a second rhythmic stem.
   var separated=new ArrayList<Float>();
   for(float slot:slots){if(!separated.isEmpty()&&slot-separated.get(separated.size()-1)<tab.gap()*.75f){float prior=separated.get(separated.size()-1),a=Float.MAX_VALUE,b=Float.MAX_VALUE;for(var f:tab.frets()){a=Math.min(a,Math.abs(f.x()-prior));b=Math.min(b,Math.abs(f.x()-slot));}if(b<a)separated.set(separated.size()-1,slot);}else separated.add(slot);}
   slots=separated;
   var frets=new ArrayList<>(tab.frets());frets.sort(Comparator.comparingDouble(TablatureDecoder.Fret::x));
   var chordBars=new HashSet<Integer>();
   for(var a:tab.frets())for(var b:tab.frets())if(a.string()!=b.string()&&a.fret()>=-1&&b.fret()>=-1&&(a.marks()&NoteOrnament.GRACE)==0&&(b.marks()&NoteOrnament.GRACE)==0&&Math.abs(a.x()-b.x())<tab.gap()*.4f)chordBars.add(barIndex(tab,a.x()));
   for(float slot:slots)for(int string=0;string<tab.stringCount();string++) {
    if(frets.stream().anyMatch(f->(f.marks()&NoteOrnament.GRACE)!=0&&Math.abs(f.x()-slot)<tab.gap()*.45f))continue;
    TablatureDecoder.Fret previous=null,current=null;
    for(var f:frets)if(f.string()==string&&f.fret()>=-1){if(Math.abs(f.x()-slot)<tab.gap()*.45f)current=f;else if(f.x()<slot&&(previous==null||f.x()>previous.x()))previous=f;}
    if(previous==null||previous.fret()<0||current!=null&&current.fret()<0||(previous.marks()&NoteOrnament.GRACE)!=0||current!=null&&previous.fret()!=current.fret()||slot-previous.x()>tab.gap()*15)continue;
    if(!chordBars.contains(barIndex(tab,slot))) {
     boolean occupied=false;float last=-1;
     for(var f:frets)if(f.fret()>=-1&&(f.marks()&NoteOrnament.GRACE)==0){if(Math.abs(f.x()-slot)<tab.gap()*.45f)occupied=true;else if(f.x()<slot)last=Math.max(last,f.x());}
     // A monophonic legato arc must not create a second voice under a note on another string.
     if(current==null&&occupied||previous.x()<last-tab.gap()*.45f)continue;
    }
    float end=current==null?slot:current.x();if(!arc(clean,w,h,previous.x(),end,previous.y(),tab.gap()))continue;
    if(current!=null){frets.remove(current);frets.add(new TablatureDecoder.Fret(current.x(),current.y(),string,current.fret(),current.duration(),current.beams(),current.dots(),current.marks(),true,current.tuplet()));}
    else frets.add(new TablatureDecoder.Fret(slot,tab.top()+string*tab.gap(),string,previous.fret(),0,0,0,previous.marks(),true));
   }
   frets.sort(Comparator.comparingDouble(TablatureDecoder.Fret::x));out.add(tab.withFrets(frets));
  }
  return List.copyOf(out);
 }
 private static int barIndex(TablatureDecoder.Staff t,float x){int index=0;for(float bar:t.bars())if(bar<x)index++;return index;}
 static boolean arc(byte[] clean,int w,int h,float from,float to,float cy,float gap) {
  if(to-from<gap*.8f)return false;
  float inset=to-from<gap*2?gap*.18f:gap*.35f;
  int left=Math.max(0,Math.round(from+inset)),right=Math.min(w-1,Math.round(to-inset));if(right-left<gap*.25f)return false;
  for(int direction:new int[]{-1,1}) {
   boolean end=false;
   for(int x=Math.max(0,Math.round(to-gap*.45f));x<=Math.min(w-1,Math.round(to+gap*.1f));x++)
    for(int j=Math.round(gap*.08f);j<=Math.round(gap*.85f);j++){int y=Math.round(cy)+direction*j;if(y>=0&&y<h&&(clean[y*w+x]&255)<180)end=true;}
   if(!end)continue;
   int hit=0,total=0;float min=Float.MAX_VALUE,max=0;
   for(int x=left;x<=right;x++) {
    total++;float closest=Float.MAX_VALUE;
    for(int j=Math.round(gap*.08f);j<=Math.round(gap*1.8f);j++){int y=Math.round(cy)+direction*j;if(y>=0&&y<h&&(clean[y*w+x]&255)<180)closest=Math.min(closest,j);}
    if(closest<Float.MAX_VALUE){hit++;min=Math.min(min,closest);max=Math.max(max,closest);}
   }
   // A flat leftover string fringe cannot prove a tie; require curved vertical travel.
   if(hit>=total*.65f&&max-min>=gap*.07f)return true;
  }
  return false;
 }
}
