// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.*;

/** Isolated dynamic words and long straight hairpins, outside the five-line staff. */
final class ScoreDynamicsDetector {
    private ScoreDynamicsDetector() { }
    static List<PlayingTechniqueDetector.Staff> alignStaffs(List<PlayingTechniqueDetector.Staff> staffs,
                                                           List<ScoreNoteEvent> notes,int height) {
        List<PlayingTechniqueDetector.Staff> result=new ArrayList<>();
        for(var staff:staffs) {
            Map<Integer,Integer> votes=new HashMap<>();
            for(var note:notes) {
                float y=note.pageY()*height,center=(staff.top()+staff.bottom())*.5f;
                if(Math.abs(y-center)>staff.gap()*5)continue;
                boolean nearest=true;for(var other:staffs)if(Math.abs(y-(other.top()+other.bottom())*.5f)<Math.abs(y-center))nearest=false;
                if(nearest)votes.merge(note.staffCount()*16+note.staffIndex(),1,Integer::sum);
            }
            int lane=staff.count()*16+staff.index(),best=0;
            for(var vote:votes.entrySet())if(vote.getValue()>best){best=vote.getValue();lane=vote.getKey();}
            result.add(new PlayingTechniqueDetector.Staff(staff.top(),staff.bottom(),staff.gap(),lane%16,lane/16));
        }
        return result;
    }
    /** Isolated letter-sized raw components grouped into a word, without staff lines or beams. */
    static List<PlayingTechniqueDetector.Word> symbolBoxes(byte[] gray,List<PlayingTechniqueDetector.Staff> staffs,int width,int height) {
        boolean[] seen=new boolean[gray.length];int[] queue=new int[gray.length];
        List<int[]> boxes=new ArrayList<>();
        for(int p=0;p<gray.length;p++) {
            if(seen[p]||(gray[p]&255)>=145)continue;
            int start=0,n=1;queue[0]=p;seen[p]=true;int left=p%width,right=left,top=p/width,bottom=top;
            while(start<n) {
                int at=queue[start++],x=at%width,y=at/width;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;if(nx<0||nx>=width||ny<0||ny>=height)continue;int next=ny*width+nx;
                    if(!seen[next]&&(gray[next]&255)<145){seen[next]=true;queue[n++]=next;}
                }
            }
            var staff=owner(staffs,top,bottom);if(staff==null)continue;float gap=staff.gap();
            // A nearby hairpin must not glue itself to mf/mp and make the whole
            // group too wide to be a word. Hairpins are detected independently.
            if(right-left>=gap*4&&right-left>(bottom-top)*4)continue;
            // Keep small adjacent letters too, so the p in "pizz." is not accepted as piano.
            if(n>=3&&bottom-top>=gap*.2&&bottom-top<=gap*3&&right-left>=gap*.12&&right-left<=gap*8)
                boxes.add(new int[]{left,top,right+1,bottom+1,staffs.indexOf(staff)});
        }
        boxes.sort(Comparator.<int[]>comparingInt(b->b[4]).thenComparingInt(b->b[0]));
        List<int[]> joined=new ArrayList<>();
        for(int[] b:boxes) {
            int[] a=null;float gap=staffs.get(b[4]).gap();
            for(int i=joined.size()-1;i>=0;i--) {
                var candidate=joined.get(i);
                if(candidate[4]==b[4]&&b[0]-candidate[2]<gap*.5&&b[0]>=candidate[0]
                        &&Math.max(candidate[1],b[1])<Math.min(candidate[3],b[3])){a=candidate;break;}
            }
            if(a!=null){a[1]=Math.min(a[1],b[1]);a[2]=Math.max(a[2],b[2]);a[3]=Math.max(a[3],b[3]);}
            else joined.add(b);
        }
        List<PlayingTechniqueDetector.Word> result=new ArrayList<>();
        for(int[] b:joined) {
            float gap=staffs.get(b[4]).gap();
            if(b[3]-b[1]>=gap*.55&&b[3]-b[1]<=gap*3&&b[2]-b[0]>=gap*.3&&b[2]-b[0]<=gap*8)
                result.add(new PlayingTechniqueDetector.Word("",b[0]/(float)width,b[1]/(float)height,b[2]/(float)width,b[3]/(float)height));
        }
        return result;
    }
    static float level(String text) {
        if(text==null)return Float.NaN;
        return switch(text.trim().toLowerCase(Locale.ROOT).replaceAll("[.,:;]$","")) {
            case "ppp" -> -18; case "pp" -> -12; case "p" -> -8;
            case "mp" -> -4; case "m" -> -2; case "mf" -> 0;
            case "f" -> 3; case "ff" -> 6; case "fff" -> 9; default -> Float.NaN;
        };
    }
    static boolean dynamicLine(String text) {
        return text!=null&&text.trim().toLowerCase(Locale.ROOT)
                .matches("(?:(?:subito|sempre|poco|a|più|piu|cantabile|sostenuto|marcato|crescendo|diminuendo|cresc|dim|decresc|ppp|pp|p|mp|m|mf|fff|ff|f)[.,:;]?\\s*)+");
    }
    static List<String> packedLevels(String text) {
        if(text==null||Float.isFinite(level(text)))return List.of();
        var matcher=java.util.regex.Pattern.compile("^(mf|mp|fff|ff|f|ppp|pp|p)(mf|mp|fff|ff|f|ppp|pp|p)$")
                .matcher(text.trim().toLowerCase(Locale.ROOT));
        return matcher.matches()?List.of(matcher.group(1),matcher.group(2)):List.of();
    }
    static List<String> joinedDirection(String text) {
        if(text==null)return List.of();
        var matcher=java.util.regex.Pattern.compile("^(ppp|pp|p|mf|mp|fff|ff|f)(crescendo|cresc|diminuendo|dim|decresc)[.,:;]?$")
                .matcher(text.trim().toLowerCase(Locale.ROOT));
        return matcher.matches()?List.of(matcher.group(1),matcher.group(2)):List.of();
    }
    static int textDirection(String text) {
        if(text==null)return 0;
        return switch(text.toLowerCase(Locale.ROOT).replaceAll("[.,:;]$","")) {
            case "cresc","crescendo" -> 1;
            case "dim","diminuendo","decresc" -> -1;
            default -> 0;
        };
    }
    static boolean containsInk(PlayingTechniqueDetector.Word word,byte[] gray,int width,int height) {
        int left=Math.max(0,Math.round(word.left()*width)),right=Math.min(width,Math.round(word.right()*width));
        int top=Math.max(0,Math.round(word.top()*height)),bottom=Math.min(height,Math.round(word.bottom()*height));
        if(right<=left||bottom<=top)return false;
        int ink=0;
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)if((gray[y*width+x]&255)<145)ink++;
        return ink>=Math.max(4,(right-left)*(bottom-top)*.035);
    }
    static List<ScoreDynamicChange> detect(List<PlayingTechniqueDetector.Word> words,
            List<PlayingTechniqueDetector.Staff> staffs,List<MeasureRegion> measures,
            List<ScoreNoteEvent> notes,byte[] gray,int width,int height) {
        List<ScoreDynamicChange> result=new ArrayList<>();
        var shared=GrandStaffDynamics.bracedPairs(staffs,measures,gray,width,height);
        for(var word:words) {
            float db=level(word.text());if(!Float.isFinite(db))continue;
            var owner=owner(staffs,word.top()*height,word.bottom()*height);
            if(owner==null||word.bottom()-word.top()>owner.gap()*3/height)continue;
            var common=GrandStaffDynamics.between(shared,word.top()*height,word.bottom()*height);
            add(result,common==null?owner:common,word.left(),word.left(),db,0,measures,notes,width,height,common!=null);
        }
        if(gray!=null&&gray.length==width*height) {
            boolean[] seen=new boolean[gray.length];int[] queue=new int[gray.length];
            for(int p=0;p<gray.length;p++) {
                if(seen[p]||(gray[p]&255)>=145)continue;
                int start=0,n=1;queue[0]=p;seen[p]=true;
                int left=p%width,right=left,top=p/width,bottom=top;
                while(start<n) {
                    int at=queue[start++],x=at%width,y=at/width;
                    left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                    for(int dy=-2;dy<=2;dy++)for(int dx=-1;dx<=1;dx++) {
                        int nx=x+dx,ny=y+dy;if(nx<0||nx>=width||ny<0||ny>=height)continue;
                        int next=ny*width+nx;
                        if(!seen[next]&&(gray[next]&255)<145){seen[next]=true;queue[n++]=next;}
                    }
                }
                var owner=owner(staffs,top,bottom);if(owner==null)continue;
                float gap=owner.gap();int w=right-left+1,h=bottom-top+1;
                // Short >/< remain accents. Reject staff/beam blocks and vertical/slanted text.
                if(w<gap*4||h<gap*.4||h>gap*3||w<h*4||n>w*Math.max(8,gap))continue;
                int[] upper=new int[w],lower=new int[w];Arrays.fill(upper,Integer.MAX_VALUE);
                Arrays.fill(lower,Integer.MIN_VALUE);
                for(int i=0;i<n;i++){int x=queue[i]%width-left,y=queue[i]/width;upper[x]=Math.min(upper[x],y);lower[x]=Math.max(lower[x],y);}
                int direction=hairpinDirection(upper,lower,gap);
                var common=GrandStaffDynamics.between(shared,top,bottom);
                if(direction!=0)add(result,common==null?owner:common,left/(float)width,right/(float)width,0,direction,measures,notes,width,height,common!=null);
            }
        }
        // Written cresc./dim. continues across systems until the next printed level (or
        // the page boundary). Do not invent geometry-sized note durations for the ramp.
        for(var word:words) {
            int direction=textDirection(word.text());if(direction==0)continue;
            var owner=owner(staffs,word.top()*height,word.bottom()*height);if(owner==null)continue;
            var common=GrandStaffDynamics.between(shared,word.top()*height,word.bottom()*height);
            if(common!=null)owner=common;
            Slot a=slot(word.left(),owner,measures,notes,width,height);if(a==null)continue;
            // A direction printed beside an absolute level begins with that level.
            for(var c:result)if(c.direction()==0&&c.measureIndex()==a.measure&&c.staffIndex()==owner.index()
                    &&c.positionInMeasure()<=a.position&&a.position-c.positionInMeasure()
                        <owner.gap()*5/(width*(measures.get(a.measure).right()-measures.get(a.measure).left())))
                a=new Slot(a.measure,c.positionInMeasure());
            Slot end=new Slot(measures.size()-1,1);
            for(var c:result)if(c.direction()==0&&(c.sharedStaffs()||c.staffIndex()==owner.index())
                    &&(c.measureIndex()>a.measure||c.measureIndex()==a.measure&&c.positionInMeasure()>a.position+.025f)
                    &&(c.measureIndex()<end.measure||c.measureIndex()==end.measure&&c.positionInMeasure()<end.position))
                end=new Slot(c.measureIndex(),c.positionInMeasure());
            boolean duplicate=false;
            for(var c:result)if(c.direction()==direction&&c.measureIndex()==a.measure&&c.staffIndex()==owner.index()
                    &&Math.abs(c.positionInMeasure()-a.position)<.025f)duplicate=true;
            if(!duplicate&&(end.measure>a.measure||end.position>a.position))
                result.add(new ScoreDynamicChange(a.measure,a.position,owner.index(),owner.count(),end.measure,end.position,0,direction,common!=null));
        }
        result.sort(Comparator.comparingInt(ScoreDynamicChange::measureIndex)
                .thenComparingDouble(ScoreDynamicChange::positionInMeasure).thenComparingInt(c->c.direction()==0?0:1));
        return List.copyOf(result);
    }
    static int hairpinDirection(int[] upper,int[] lower,float gap) {
        int w=upper.length;double[] u=new double[8],l=new double[8];
        for(int b=0;b<8;b++) {
            int count=0;for(int x=b*w/8;x<(b+1)*w/8;x++)if(upper[x]!=Integer.MAX_VALUE){u[b]+=upper[x];l[b]+=lower[x];count++;}
            if(count<Math.max(1,w/8*.8))return 0;u[b]/=count;l[b]/=count;
        }
        double opening=(l[7]-u[7])-(l[0]-u[0]);if(Math.abs(opening)<gap*.4)return 0;
        if((u[7]-u[0])*(l[7]-l[0])>=0)return 0;
        for(int b=0;b<8;b++)if(Math.abs(u[b]-(u[0]+(u[7]-u[0])*b/7))>Math.max(1.4,gap*.14)
                ||Math.abs(l[b]-(l[0]+(l[7]-l[0])*b/7))>Math.max(1.4,gap*.14))return 0;
        return opening>0?1:-1;
    }
    private static PlayingTechniqueDetector.Staff owner(List<PlayingTechniqueDetector.Staff> staffs,float top,float bottom) {
        PlayingTechniqueDetector.Staff best=null;float score=Float.MAX_VALUE;
        for(var staff:staffs) {
            float distance;
            if(top>=staff.bottom()+staff.gap()*.25f)distance=(top-staff.bottom())/staff.gap();
            else if(bottom<=staff.top()-staff.gap()*.25f)distance=(staff.top()-bottom)/staff.gap()+.75f;
            else continue;
            if(distance<=4.5f&&distance<score){score=distance;best=staff;}
        }
        return best;
    }
    private record Slot(int measure,float position) { }
    private static Slot slot(float x,PlayingTechniqueDetector.Staff staff,List<MeasureRegion> measures,
                             List<ScoreNoteEvent> notes,int width,int height) {
        int found=-1;float distance=Float.MAX_VALUE,y=(staff.top()+staff.bottom())*.5f/height;
        for(int m=0;m<measures.size();m++) {
            var r=measures.get(m);if(y<r.top()-staff.gap()/height||y>r.bottom()+staff.gap()/height)continue;
            float d=Math.max(r.left()-x,Math.max(0,x-r.right()));
            if(d<distance){distance=d;found=m;}
        }
        if(found<0||distance>staff.gap()*3/width)return null;
        var r=measures.get(found);float position=Math.max(0,Math.min(1,(x-r.left())/(r.right()-r.left())));
        float nearest=staff.gap()/width/(r.right()-r.left()),printed=position;
        for(var note:notes)if(note.measureIndex()==found&&note.staffIndex()==staff.index()) {
            float d=Math.abs(note.positionInMeasure()-printed);if(d<nearest){nearest=d;position=note.positionInMeasure();}
        }
        return new Slot(found,position);
    }
    private static void add(List<ScoreDynamicChange> result,PlayingTechniqueDetector.Staff owner,float left,float right,
                            float db,int direction,List<MeasureRegion> measures,List<ScoreNoteEvent> notes,int width,int height,boolean shared) {
        Slot a=slot(left,owner,measures,notes,width,height),b=slot(right,owner,measures,notes,width,height);
        if(a==null||b==null||b.measure<a.measure||b.measure==a.measure&&b.position<a.position)return;
        if(direction!=0&&a.measure==b.measure&&b.position-a.position<.04)return;
        var change=new ScoreDynamicChange(a.measure,a.position,owner.index(),owner.count(),b.measure,b.position,db,direction,shared);
        for(var old:result)if(old.staffIndex()==change.staffIndex()&&old.staffCount()==change.staffCount()
                &&old.measureIndex()==change.measureIndex()&&Math.abs(old.positionInMeasure()-change.positionInMeasure())<.025
                &&old.direction()==direction&&old.decibels()==db)return;
        result.add(change);
    }
}
