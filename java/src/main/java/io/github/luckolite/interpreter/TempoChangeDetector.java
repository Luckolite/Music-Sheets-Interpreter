// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conservative detector for printed note-equals-number tempo marks. */
final class TempoChangeDetector {
    private TempoChangeDetector() { }

    static List<ScoreTempoChange> detect(List<MeasureNumberReconciler.NumberToken> tokens,
                                         byte[] gray, int width, int height,
                                         List<MeasureRegion> measures) {
        if (tokens == null || tokens.isEmpty() || gray == null || gray.length != width * height
                || width <= 0 || height <= 0 || measures == null || measures.isEmpty())
            return List.of();
        List<ScoreTempoChange> result = new ArrayList<>();
        for (MeasureNumberReconciler.NumberToken token : tokens) {
            if (token.value() < 30 || token.value() > 400) continue;
            int equalsLeft = equalsSignLeft(token, gray, width, height);
            if (equalsLeft < 0) continue;
            // A direction such as "Lento, poco rubato (quarter = c. 88)" starts at the
            // change; the digits can extend to the next bar. Keep their ink box for symbol
            // recognition, but attach the change to the start of its OCR direction line.
            var anchor=new MeasureNumberReconciler.NumberToken(token.value(),token.annotationLeft(),
                    token.top(),token.annotationLeft()+(token.right()-token.left()),token.bottom());
            int measureIndex = nearestFollowingMeasure(anchor, measures,
                    token.annotationLeft() >= token.left()-.001f);
            // OCR may include the tempo note/equal sign in a line starting left of
            // the first bar. The verified digits may still clearly belong to the
            // opening staff. Do not relax ink checks or relocate later directions.
            if(measureIndex<0&&nearestFollowingMeasure(token,measures,true)==0)measureIndex=0;
            if (measureIndex < 0) continue;
            MeasureRegion measure = measures.get(measureIndex);
            float position = (anchor.left() - measure.left())
                    / Math.max(.0001f, measure.right() - measure.left());
            // Engravers place the number to the right of the note/equal sign. Snap a mark printed
            // near the barline to beat zero instead of delaying the change by the text width.
            position = Math.max(0f, Math.min(1f, position));
            if (measureIndex == 0 && token.bottom() <= measure.top()) position = 0f;
            if (position < .22f) position = 0f;
            double unit = printedBeatUnit(token, gray, width, height, equalsLeft);
            result.add(new ScoreTempoChange(measureIndex, position, token.value() * unit, unit));
        }
        result.sort(Comparator.comparingInt(ScoreTempoChange::measureIndex)
                .thenComparingDouble(ScoreTempoChange::positionInMeasure));
        List<ScoreTempoChange> deduplicated = new ArrayList<>();
        for (ScoreTempoChange change : result) {
            if (!deduplicated.isEmpty()) {
                ScoreTempoChange previous = deduplicated.get(deduplicated.size() - 1);
                if (previous.measureIndex() == change.measureIndex()
                        && previous.bpm() == change.bpm()) continue;
            }
            deduplicated.add(change);
        }
        return List.copyOf(deduplicated);
    }

    private static int nearestFollowingMeasure(MeasureNumberReconciler.NumberToken token,
                                                List<MeasureRegion> measures, boolean inferOpening) {
        float centerX = (token.left() + token.right()) * .5f;
        float centerY = (token.top() + token.bottom()) * .5f;
        for (MeasureRegion staff : measures) {
            if (centerX >= staff.left() - .025f && centerX <= staff.right() + .025f
                    && centerY > staff.top() + .002f && centerY < staff.bottom()) return -1;
        }
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int index = 0; index < measures.size(); index++) {
            MeasureRegion measure = measures.get(index);
            float rowHeight = measure.bottom() - measure.top();
            if (centerX < measure.left() - .025f || centerX > measure.right() + .025f
                    // Clefs, accidentals and ledger lines can OCR as digits with a spurious
                    // nearby '='. A tempo annotation belongs above the staff, not inside it.
                    // Take Flight's treble clef became 47 BPM at measure 13 on the phone.
                    || token.bottom() > measure.top() + Math.min(.002f, rowHeight * .05f)
                    || token.bottom() < measure.top() - Math.max(.015f, rowHeight * 1.1f)) continue;
            float distance = Math.abs(measure.top() - token.bottom());
            if (distance < bestDistance) { bestDistance = distance; best = index; }
        }
        if (best < 0) return -1;
        MeasureRegion target = measures.get(best);
        float topmost = Float.MAX_VALUE;
        for (MeasureRegion measure : measures) topmost = Math.min(topmost, measure.top());
        float tolerance = Math.max(.012f, (target.bottom() - target.top()) * .22f);
        if (inferOpening && Math.abs(target.top() - topmost) <= tolerance && token.bottom() <= target.top()) {
            for (int index = 0; index < measures.size(); index++) {
                MeasureRegion measure = measures.get(index);
                if (Math.abs(measure.top() - target.top()) <= tolerance
                        && measure.left() < measures.get(best).left()) best = index;
            }
        }
        return best;
    }

    /** Two separated, short horizontal ink bands immediately left of the BPM digits. */
    private static int equalsSignLeft(MeasureNumberReconciler.NumberToken token,
                                         byte[] gray, int width, int height) {
        int tokenLeft = Math.max(0, Math.round(token.left() * width));
        int tokenRight = Math.min(width - 1, Math.round(token.right() * width));
        int tokenTop = Math.max(0, Math.round(token.top() * height));
        int tokenBottom = Math.min(height - 1, Math.round(token.bottom() * height));
        int tokenWidth = Math.max(2, tokenRight - tokenLeft + 1);
        int tokenHeight = Math.max(3, tokenBottom - tokenTop + 1);
        int left = Math.max(0, tokenLeft - Math.max(tokenWidth, tokenHeight * 2));
        int right = Math.min(width - 1, tokenLeft - 1);
        int top = Math.max(0, tokenTop - tokenHeight / 4);
        int bottom = Math.min(height - 1, tokenBottom + tokenHeight / 4);
        if (right < left || bottom < top) return -1;
        // An equals sign is two disconnected, aligned horizontal strokes. Counting arbitrary
        // dark scanlines also counts letter tops/bottoms in titles such as "Op. 101".
        int rw = right-left+1, rh = bottom-top+1;
        boolean[] visited = new boolean[rw*rh];
        int[] queue = new int[rw*rh];
        List<int[]> bands = new ArrayList<>();
        for (int sy=0; sy<rh; sy++) for (int sx=0; sx<rw; sx++) {
            int seed=sy*rw+sx;
            if (visited[seed] || (gray[(top+sy)*width+left+sx]&0xff)>125) continue;
            int read=0, size=1, minX=sx, maxX=sx, minY=sy, maxY=sy;
            queue[0]=seed; visited[seed]=true;
            while(read<size) {
                int point=queue[read++], x=point%rw, y=point/rw;
                minX=Math.min(minX,x); maxX=Math.max(maxX,x);
                minY=Math.min(minY,y); maxY=Math.max(maxY,y);
                for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx, ny=y+dy;
                    if(nx<0||ny<0||nx>=rw||ny>=rh) continue;
                    int next=ny*rw+nx;
                    if(!visited[next] && (gray[(top+ny)*width+left+nx]&0xff)<=125) {
                        visited[next]=true; queue[size++]=next;
                    }
                }
            }
            int bw=maxX-minX+1, bh=maxY-minY+1;
            if(bw>=Math.max(3,Math.round(tokenHeight*.22f)) && bw>=bh*2
                    && bw<=tokenHeight*1.5f && bh<=Math.max(2,tokenHeight*.28f))
                bands.add(new int[]{minX,maxX,minY,maxY});
        }
        int minimumSeparation = Math.max(2, Math.round(tokenHeight * .12f));
        int maximumSeparation = Math.max(minimumSeparation, Math.round(tokenHeight * .82f));
        for (int first = 0; first < bands.size(); first++)
            for (int second = first + 1; second < bands.size(); second++) {
                int[] a=bands.get(first), b=bands.get(second);
                float separation = Math.abs((b[2]+b[3]-a[2]-a[3])*.5f);
                int overlap = Math.min(a[1],b[1])-Math.max(a[0],b[0])+1;
                int wider = Math.max(a[1]-a[0]+1,b[1]-b[0]+1);
                if (overlap >= wider*.7f && separation >= minimumSeparation
                        && separation <= maximumSeparation) return left + Math.min(a[0], b[0]);
            }
        return -1;
    }

    /** Quarter-beat length of a filled tempo note, retaining its flag and augmentation dot. */
    private static double printedBeatUnit(MeasureNumberReconciler.NumberToken token,
            byte[] gray, int width, int height, int equalsLeft) {
        int unit=Math.max(3,Math.round((token.bottom()-token.top())*height));
        // OCR boxes may include generous vertical padding (ML Kit's 38px box surrounds
        // 23px digits here). Measure the printed ink before comparing note/dot geometry.
        int inkTop=height,inkBottom=-1;
        for(int y=Math.max(0,Math.round(token.top()*height));y<=Math.min(height-1,Math.round(token.bottom()*height));y++)
            for(int x=Math.max(0,Math.round(token.left()*width));x<=Math.min(width-1,Math.round(token.right()*width));x++)
                if((gray[y*width+x]&255)<=125){inkTop=Math.min(inkTop,y);inkBottom=Math.max(inkBottom,y);}
        if(inkBottom>=inkTop)unit=Math.max(3,inkBottom-inkTop+1);
        int left=Math.max(0,equalsLeft-unit*3),right=equalsLeft-1;
        int top=Math.max(0,Math.round(token.top()*height)-unit);
        int bottom=Math.min(height-1,Math.round(token.bottom()*height)+unit/3);
        int rw=right-left+1,rh=bottom-top+1;
        if(rw<=0||rh<=0)return 1;
        boolean[] seen=new boolean[rw*rh];int[] queue=new int[seen.length];
        List<int[]> parts=new ArrayList<>();
        for(int sy=0;sy<rh;sy++)for(int sx=0;sx<rw;sx++) {
            int seed=sy*rw+sx;
            if(seen[seed]||(gray[(top+sy)*width+left+sx]&255)>200)continue;
            int count=1,read=0,minX=sx,maxX=sx,minY=sy,maxY=sy;
            seen[seed]=true;queue[0]=seed;
            while(read<count) {
                int at=queue[read++],x=at%rw,y=at/rw;
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;
                    if(nx<0||ny<0||nx>=rw||ny>=rh)continue;
                    int next=ny*rw+nx;
                    if(!seen[next]&&(gray[(top+ny)*width+left+nx]&255)<=200){seen[next]=true;queue[count++]=next;}
                }
            }
            parts.add(new int[]{left+minX,left+maxX,top+minY,top+maxY,count});
        }
        for(int[] note:parts) {
            int nw=note[1]-note[0]+1,nh=note[3]-note[2]+1;
            if(nh<unit*1.2f||nh>unit*2.8f||nw<unit*.25f||nw>unit*1.5f)continue;
            // The head's centre is dark for a quarter; a half-note centre remains paper.
            int cy=note[3]-Math.max(1,Math.round(unit*.12f));
            int headLeft=note[1],headRight=note[0];
            for(int x=note[0];x<=note[1];x++)if((gray[cy*width+x]&255)<=200){headLeft=Math.min(headLeft,x);headRight=Math.max(headRight,x);}
            int cx=(headLeft+headRight)/2;
            if((gray[cy*width+cx]&255)>125)continue;
            // Locate the long upright stem independently of the flag's bounding box.
            int stem=-1,best=0;
            for(int x=note[0];x<=note[1];x++) {
                int ink=0;for(int y=note[2];y<note[2]+nh*2/3;y++)if((gray[y*width+x]&255)<=200)ink++;
                if(ink>=best){best=ink;stem=x;}
            }
            if(stem<0||best<nh*.5f)continue;
            int flaggedRows=0;
            for(int y=note[2];y<note[2]+nh*2/3;y++) {
                boolean protrudes=false;
                for(int x=stem+Math.max(2,Math.round(unit*.2f));x<=note[1];x++)
                    if((gray[y*width+x]&255)<=200){protrudes=true;break;}
                if(protrudes)flaggedRows++;
            }
            double beat=flaggedRows>=Math.max(3,Math.round(unit*.25f))?.5:1;
            for(int[] dot:parts) {
                int dw=dot[1]-dot[0]+1,dh=dot[3]-dot[2]+1;
                float gap=dot[0]-note[1],dy=Math.abs((dot[2]+dot[3])*.5f-cy);
                // The light fringe of '=' can extend left of its dark-core bound.
                // It is clipped by this search window, unlike a complete printed dot.
                if(dot[1]<right&&gap>0&&gap<unit*.6f&&dw>=2&&dh>=2&&dw<=unit*.35f&&dh<=unit*.35f
                        &&dy<=unit*.22f&&dot[4]>=dw*dh*.45f)return beat*1.5;
            }
            return beat;
        }
        return 1;
    }
}
