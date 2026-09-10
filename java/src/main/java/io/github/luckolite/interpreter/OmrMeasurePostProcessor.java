// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Converts the segmentation model's semantic pixel labels into the measure rectangles used by the viewer. */
final class OmrMeasurePostProcessor {
    static final byte STEM_OR_REST = 1;
    static final byte NOTEHEAD = 2;
    static final byte CLEF_OR_KEY = 3;
    static final byte STAFF = 4;
    static final byte SYMBOL = 5;
    /** Upper violin writing can place a head this far beyond the five staff lines. */
    private static final float MAX_HEAD_LEDGER_GAPS = 6.75f;
    /** Faint scanned barlines are commonly mid-gray after PDF rendering. */
    private static final int RAW_BARLINE_DARK = 205;
    /** Two staves in one grand staff are close; consecutive compact violin systems are not. */
    private static final float MAX_GRAND_STAFF_SEPARATION_GAPS = 6.75f;
    /** A printed bracket/shared barline is stronger evidence than whitespace. Duet and orchestral
     * layouts can leave a wider gap between connected staves than a compact grand staff does. */
    private static final float MAX_CONNECTED_STAFF_SEPARATION_GAPS = 16f;

    private OmrMeasurePostProcessor() { }

    static List<MeasureRegion> process(byte[] labels, int width, int height) {
        return process(labels, null, width, height);
    }

    static List<MeasureRegion> process(byte[] labels, byte[] gray, int width, int height) {
        if (labels == null || width <= 0 || height <= 0 || labels.length != width * height)
            return List.of();
        if (gray != null && gray.length != labels.length) gray = null;
        List<StaffRun> staffs = findStaffs(labels, gray, width, height);
        if (staffs.isEmpty()) return List.of();
        List<SystemRun> systems = mergeAlignedStaffs(staffs, gray, width, height);
        List<MeasureRegion> result = new ArrayList<>();
        for (SystemRun system : systems) addMeasures(labels, width, height, system, result);
        // Systems already run top to bottom and their boundaries left to right.
        // Sorting tilted measure boxes by their top edge reverses an uphill row.
        return List.copyOf(result);
    }

    private static List<StaffRun> findStaffs(byte[] labels, byte[] gray, int width, int height) {
        float slope = estimateStaffSlope(labels, width, height);
        int[] rowStrength = new int[height];
        float centerX = width / 2f;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
            if (labels[y * width + x] == STAFF) {
                int deskewedY = Math.round(y - slope * (x - centerX));
                if (deskewedY >= 0 && deskewedY < height) rowStrength[deskewedY]++;
            }
        int minimumStrength = Math.max(10, width / 80);
        List<RawStaffLineDetector.StaffLines> semanticStaffs =
                RawStaffLineDetector.detectFromStrength(rowStrength, minimumStrength, height);

        List<StaffRun> result = new ArrayList<>();
        for (RawStaffLineDetector.StaffLines semantic : semanticStaffs) {
            int[] rows = semantic.rows();
            float gap = semantic.gap();
            int top = Math.max(0, Math.round(rows[0] - gap * 0.65f));
            int bottom = Math.min(height - 1, Math.round(rows[4] + gap * 0.65f));
            int[] columns = new int[width];
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                if (labels[y * width + x] == STAFF) {
                    int deskewedY = Math.round(y - slope * (x - centerX));
                    if (deskewedY >= top && deskewedY <= bottom) columns[x]++;
                }
            int total = Arrays.stream(columns).sum();
            int left = percentileColumn(columns, total, 0.012f);
            int right = percentileColumn(columns, total, 0.988f);
            if(gray!=null) {
                int[] printed=rawStaffColumns(gray,width,height,rows,gap,slope);
                int printedTotal=Arrays.stream(printed).sum();
                int printedLeft=percentileColumn(printed,printedTotal,.012f);
                int printedRight=percentileColumn(printed,printedTotal,.988f);
                if(printedLeft<left&&continuousStaffExtension(printed,printedLeft,left))left=printedLeft;
                if(printedRight>right&&continuousStaffExtension(printed,right,printedRight))right=printedRight;
                // Very faded horizontal rules may disappear while the closing
                // bar remains clear. A verified full-height bar can preserve
                // those final notes without guessing a regular measure width.
                List<Integer> outer=findBoundaries(labels,gray,width,height,rows,gap,left,width-1,slope);
                if(outer.size()>2) {
                    int closing=outer.get(outer.size()-2);
                    if(closing>right&&closing-right<width*.25f)right=closing;
                }
            }
            if (right - left >= Math.max(width / 4, Math.round(gap * 18f))) {
                List<Integer> boundaries = findBoundaries(labels, gray, width, height, rows,
                        gap, left, right, slope);
                if (boundaries.size() >= 2)
                    result.add(new StaffRun(rows[0], rows[4], gap, left, right, boundaries, slope));
            }
        }
        recoverRawStaffs(labels, gray, width, height, result);
        result.sort(Comparator.comparingInt(StaffRun::top));
        return result;
    }

    /**
     * A photographed or scanned page can keep perfectly readable staff lines in the source while
     * the model labels only the flatter systems. Recover only strong page-spanning five-line groups,
     * leaving the semantic result in charge wherever it already found the system.
     */
    private static void recoverRawStaffs(byte[] labels, byte[] gray, int width, int height,
                                         List<StaffRun> result) {
        List<Float> semanticCenters = new ArrayList<>();
        for (StaffRun staff : result) semanticCenters.add((staff.top + staff.bottom) * .5f);
        semanticCenters.sort(Float::compare);
        float semanticSystemStep = typicalSystemStep(semanticCenters);
        List<RawStaffLineDetector.StaffLines> rawStaffs=RawStaffLineDetector.detect(gray,width,height);
        for (RawStaffLineDetector.StaffLines raw : rawStaffs) {
            int representedIndex = -1;
            for (int index = 0; index < result.size(); index++) {
                StaffRun existing = result.get(index);
                float center = (existing.top + existing.bottom) * .5f;
                if (Math.abs(center - raw.center()) <= Math.max(raw.gap() * 2.2f, height * .008f)) {
                    representedIndex = index;
                    break;
                }
            }

            int[] columns = rawStaffColumns(gray, width, height, raw.rows(), raw.gap());
            int total = Arrays.stream(columns).sum();
            int left = percentileColumn(columns, total, .012f);
            int right = percentileColumn(columns, total, .988f);
            if (right - left < Math.max(width / 4, Math.round(raw.gap() * 18f))) continue;
            List<Integer> boundaries = findBoundaries(labels, gray, width, height, raw.rows(),
                    raw.gap(), left, right, 0f);
            if (representedIndex >= 0) {
                StaffRun existing = result.get(representedIndex);
                // A global semantic deskew can find the staff but still miss its raw vertical
                // bars. Replace only a completely unsplit semantic row with a conservative raw
                // result; busier semantic layouts keep their existing evidence. The printed-number
                // reconciler can reduce false raw boundaries while retaining real unequal bar
                // positions, so final systems need this evidence too.
                if (existing.boundaries.size() == 2 && boundaries.size() > 2
                        && boundaries.size() <= 9)
                    result.set(representedIndex, new StaffRun(existing.top, existing.bottom,
                            existing.gap, left, right, boundaries, existing.slope));
                else if (Math.abs(raw.top()-existing.top) <= existing.gap*.5f
                        && Math.abs(raw.gap()-existing.gap) <= existing.gap*.18f) {
                    // The mask may fade before the printed staff ends. Preserve
                    // inner barlines, but let matching raw five-line geometry
                    // retain the header and final notes at the outer edges.
                    int expandedLeft = existing.left;
                    int expandedRight = existing.right;
                    if (left < expandedLeft && expandedLeft-left <= existing.gap*4f) expandedLeft=left;
                    if (right > expandedRight && (right-expandedRight <= existing.gap*4f
                            || continuousStaffExtension(columns,expandedRight,right))) expandedRight=right;
                    if (expandedLeft != existing.left || expandedRight != existing.right) {
                        List<Integer> extended = new ArrayList<>(existing.boundaries);
                        extended.set(0,expandedLeft);
                        extended.set(extended.size()-1,expandedRight);
                        result.set(representedIndex,new StaffRun(existing.top,existing.bottom,
                                existing.gap,expandedLeft,expandedRight,List.copyOf(extended),existing.slope));
                    }
                }
            } else if (boundaries.size() >= 2
                    && (!betweenAdjacentSemanticSystems(raw.center(), semanticCenters,
                    semanticSystemStep)||RawStaffLineDetector.connectedToStaff(raw,rawStaffs,gray,width,height)))
                result.add(new StaffRun(raw.top(), raw.bottom(), raw.gap(), left, right,
                        boundaries, 0f));
        }
    }

    /**
     * Dense beams can form five page-wide horizontal peaks at the same scale as the staff. They
     * usually sit between two already recognized consecutive systems. A genuinely omitted staff
     * instead creates a roughly double-sized hole in the semantic sequence. Reject only the
     * former so raw recovery remains available for missing rows.
     */
    private static boolean betweenAdjacentSemanticSystems(float center, List<Float> centers,
                                                           float typicalStep) {
        if (centers == null || centers.size() < 3 || !Float.isFinite(typicalStep)
                || typicalStep <= 0) return false;
        for (int index = 0; index + 1 < centers.size(); index++) {
            float before = centers.get(index), after = centers.get(index + 1);
            if (center <= before || center >= after) continue;
            float span = after - before;
            return span <= typicalStep * 1.45f
                    && center - before >= typicalStep * .20f
                    && after - center >= typicalStep * .20f;
        }
        return false;
    }

    private static float typicalSystemStep(List<Float> centers) {
        if (centers == null || centers.size() < 3) return Float.NaN;
        List<Float> gaps = new ArrayList<>();
        for (int index = 0; index + 1 < centers.size(); index++) {
            float gap = centers.get(index + 1) - centers.get(index);
            if (gap > 0) gaps.add(gap);
        }
        if (gaps.size() < 2) return Float.NaN;
        gaps.sort(Float::compare);
        // The upper-middle gap ignores the smaller separation between staves in a grand staff.
        return gaps.get((gaps.size() * 3) / 4);
    }

    private static int[] rawStaffColumns(byte[] gray, int width, int height, int[] rows,
                                         float gap) {
        return rawStaffColumns(gray,width,height,rows,gap,0f);
    }

    private static int[] rawStaffColumns(byte[] gray,int width,int height,int[] rows,
                                          float gap,float slope) {
        int[] columns = new int[width];
        int radius = Math.max(2, Math.round(gap * .30f));
        for (int x = 0; x < width; x++) for (int row : rows) {
            int printedRow=Math.round(row+slope*(x-width*.5f));
            boolean dark = false;
            for (int y = Math.max(0, printedRow-radius); y <= Math.min(height-1, printedRow+radius); y++)
                if ((gray[y * width + x] & 0xff) <= 170) { dark = true; break; }
            if (dark) columns[x]++;
        }
        return columns;
    }

    static float estimateStaffSlope(byte[] labels, int width, int height) {
        // Sampling alternate rows biases thin rules toward horizontal. Keep every
        // row, and reuse only staff pixels while testing candidate angles.
        int count=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x+=3)
            if(labels[y*width+x]==STAFF)count++;
        if(count==0)return 0f;
        int[] xs=new int[count],ys=new int[count];int index=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x+=3)
            if(labels[y*width+x]==STAFF){xs[index]=x;ys[index++]=y;}
        float bestSlope = 0f;
        float centerX = width / 2f;
        long horizontalScore=staffProjectionScore(xs,ys,height,centerX,0f);
        long bestScore=horizontalScore;
        for (int step = -10; step <= 10; step++) {
            float slope = step * 0.006f;
            long score=staffProjectionScore(xs,ys,height,centerX,slope);
            if (score > bestScore) { bestScore = score; bestSlope = slope; }
        }
        // A slight photographic tilt can lie halfway between the coarse angles.
        // At page width it still moves a rule by several pixels and can hide a row.
        float coarseSlope=bestSlope;
        for(int step=-5;step<=5;step++) {
            float slope=coarseSlope+step*.0006f;
            long score=staffProjectionScore(xs,ys,height,centerX,slope);
            if(score>bestScore){bestScore=score;bestSlope=slope;}
        }
        return bestScore>horizontalScore*1.05 ? bestSlope : 0f;
    }

    private static long staffProjectionScore(int[] xs,int[] ys,int height,float centerX,float slope) {
        int[] projection=new int[height];
        for(int i=0;i<xs.length;i++) {
            int row=Math.round(ys[i]-slope*(xs[i]-centerX));
            if(row>=0&&row<height)projection[row]++;
        }
        long score=0;
        for(int value:projection)score+=(long)value*value;
        return score;
    }

    private static int percentileColumn(int[] counts, int total, float percentile) {
        if (total <= 0) return percentile < 0.5f ? 0 : Math.max(0, counts.length - 1);
        int target = Math.max(1, Math.round(total * percentile)), accumulated = 0;
        for (int x = 0; x < counts.length; x++) {
            accumulated += counts[x];
            if (accumulated >= target) return x;
        }
        return counts.length - 1;
    }

    private static List<Integer> findBoundaries(byte[] labels, byte[] gray, int width, int height, int[] rows,
                                                 float gap, int left, int right, float slope) {
        float centerX = width / 2f;
        int span = Math.max(1, Math.round(rows[4] - rows[0] + gap * 0.5f));
        List<Integer> candidates = new ArrayList<>();
        boolean inRun = false;
        int runStart = 0;
        for (int x = left + 1; x <= right; x++) {
            float shift = slope * (x - centerX);
            int top = Math.max(0, Math.round(rows[0] + shift - gap * 0.25f));
            int bottom = Math.min(height - 1, Math.round(rows[4] + shift + gap * 0.25f));
            int covered = 0;
            for (int y = top; y <= bottom; y++) {
                boolean ink = false;
                for (int dx = -1; dx <= 1 && !ink; dx++) {
                    int checkX = x + dx;
                    ink = checkX >= 0 && checkX < width && isVerticalInk(labels[y * width + checkX]);
                }
                if (ink) covered++;
            }
            boolean touchesTop = hasVerticalInk(labels, width, height, x, 2,
                    top, Math.min(bottom, Math.round(top + gap)));
            boolean touchesBottom = hasVerticalInk(labels, width, height, x, 2,
                    Math.max(top, Math.round(bottom - gap)), bottom);
            boolean attachedHead = countLabel(labels, width, height, NOTEHEAD,
                    Math.round(x - gap * .88f), Math.round(x + gap * .88f),
                    // Only heads that can attach to a stem spanning this staff matter here.
                    // The pitch reader's six-ledger-line search reaches neighboring systems:
                    // Hunter's Frontier's low head on the preceding row hid a real bar below it.
                    Math.round(top - gap * 1.5f),
                    Math.round(bottom + gap * 1.5f))
                    >= Math.max(3, Math.round(gap * 0.65f));
            if(attachedHead&&gray!=null&&isolatedFullHeightRule(gray,width,height,x,rows,gap,shift))
                attachedHead=headTouchesColumn(labels,gray,width,height,x,
                        Math.round(top-gap*1.5f),Math.round(bottom+gap*1.5f),rows,gap,shift);
            if (!attachedHead) attachedHead = distantHeadOnSameStem(labels, width, height,
                    x, top, bottom, gap);
            // the segmentation model's stem/rest mask often shortens true barlines to stem height. Its generic
            // symbol mask retains more of the original line, so combine both semantic outputs
            // and rely on the absence of an attached notehead to reject ordinary note stems.
            boolean rawSpansStaff = gray != null && rawBarlineSpansStaff(gray, width, height,
                    x, rows, gap, shift, slope);
            boolean semanticBar = covered >= Math.max(gap * 1.65f, span * 0.36f)
                    && (touchesTop || touchesBottom) && (gray == null || rawSpansStaff);
            // Raw pixels validate a semantic candidate, but never create one by themselves:
            // aligned note stems can span all five lines on dense music such as Humoresque.
            boolean bar = semanticBar && !attachedHead;
            if (bar && !inRun) { inRun = true; runStart = x; }
            if ((!bar || x == right) && inRun) {
                int runEnd = bar && x == right ? x : x - 1;
                candidates.add((runStart + runEnd) / 2);
                inRun = false;
            }
        }

        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(left);
        int minimumGap = Math.max(Math.round(gap * 2.5f), width / 55);
        for (int candidate : candidates) {
            if (candidate - boundaries.get(boundaries.size() - 1) < minimumGap) continue;
            if (right - candidate < minimumGap) continue;
            boundaries.add(candidate);
        }
        boundaries.add(right);
        // With no internal barline, the semantic staff extent is still one trustworthy measure;
        // unlike the retired detector, this never invents evenly spaced subdivisions.
        return boundaries.size() > 32 ? List.of() : List.copyOf(boundaries);
    }

    private static boolean isVerticalInk(byte label) {
        return label == STEM_OR_REST || label == SYMBOL;
    }

    private static boolean isolatedFullHeightRule(byte[] gray,int w,int h,int x,int[] rows,float gap,float shift) {
        int top=Math.round(rows[0]+shift),bottom=Math.round(rows[4]+shift),hit=0,outside=0,samples=0;
        for(int y=Math.max(0,top);y<=Math.min(h-1,bottom);y++) {
            boolean dark=false;for(int dx=-1;dx<=1;dx++)if(x+dx>=0&&x+dx<w&&(gray[y*w+x+dx]&255)<150)dark=true;
            if(dark)hit++;
        }
        for(int d=Math.max(3,Math.round(gap*.3f));d<=gap;d++)for(int y:new int[]{top-d,bottom+d}) {
            if(y<0||y>=h)continue;samples++;
            boolean dark=false;for(int dx=-1;dx<=1;dx++)if(x+dx>=0&&x+dx<w&&(gray[y*w+x+dx]&255)<150)dark=true;
            if(dark)outside++;
        }
        return hit>=(bottom-top+1)*.95f&&outside<=samples*.15f;
    }

    /** Dense engraving can put an unrelated head inside the old broad stem-veto box.
     * Require an actual ink connection, ignoring the staff lines that join everything. */
    private static boolean headTouchesColumn(byte[] labels,byte[] gray,int width,int height,int x,
            int top,int bottom,int[] rows,float gap,float shift) {
        for(int y=Math.max(0,top);y<=Math.min(height-1,bottom);y++) {
            boolean staffLine=false;for(int row:rows)if(Math.abs(y-row-shift)<=Math.max(1,gap*.14f))staffLine=true;
            if(staffLine)continue;
            // Semantic candidates include the two-pixel halo around a thin column.
            // Start from each real column, or its white halo falsely looks disconnected.
            for(int origin=x-2;origin<=x+2;origin++)for(int direction:new int[]{-1,1})
                for(int distance=0;distance<=gap*.88f;distance++) {
                    int xx=origin+direction*distance;if(xx<0||xx>=width||(gray[y*width+xx]&255)>RAW_BARLINE_DARK)break;
                    if(labels[y*width+xx]==NOTEHEAD)return true;
                }
        }
        return false;
    }

    /** High ledger heads still veto their own long stem, but not an unrelated bar on the next row. */
    private static boolean distantHeadOnSameStem(byte[] labels, int width, int height,
                                                int x, int top, int bottom, float gap) {
        for (int direction : new int[]{-1, 1}) {
            int start = direction < 0 ? top : bottom;
            int misses = 0;
            for (int distance = 1; distance <= gap * MAX_HEAD_LEDGER_GAPS; distance++) {
                int y = start + direction * distance;
                if (y < 0 || y >= height) break;
                boolean stem = false;
                for (int dx = -2; dx <= 2; dx++) {
                    int xx = x + dx;
                    if (xx < 0 || xx >= width) continue;
                    byte label = labels[y * width + xx];
                    if (isVerticalInk(label) || label == NOTEHEAD || label == STAFF) stem = true;
                }
                if (stem) misses = 0;
                else if (++misses > Math.max(2, gap * .28f)) break;
                if (distance < gap * 1.4f) continue;
                if (countLabel(labels, width, height, NOTEHEAD,
                        Math.round(x - gap * .88f), Math.round(x + gap * .88f),
                        y - 2, y + 2) >= Math.max(3, Math.round(gap * .65f))) return true;
            }
        }
        return false;
    }

    /** A note stem can be tall in the semantic mask, but unlike a barline it does not form a
     * nearly continuous raw-ink path through both outer staff lines. */
    private static boolean rawBarlineSpansStaff(byte[] gray, int width, int height, int centerX,
                                                int[] rows, float gap, float shift, float slope) {
        int top = Math.max(0, Math.round(rows[0] + shift - gap * .12f));
        int bottom = Math.min(height - 1, Math.round(rows[4] + shift + gap * .12f));
        if (bottom <= top) return false;
        int darkRows = 0, longest = 0, current = 0;
        boolean touchesTop = false, touchesBottom = false;
        int edgeBand = Math.max(2, Math.round(gap * .34f));
        for (int y = top; y <= bottom; y++) {
            boolean dark = false;
            for (int x = Math.max(0, centerX - 2); x <= Math.min(width - 1, centerX + 2); x++)
                if ((gray[y * width + x] & 0xff) <= RAW_BARLINE_DARK) {
                    dark = true;
                    break;
                }
            if (dark) {
                darkRows++;
                current++;
                longest = Math.max(longest, current);
                if (y <= top + edgeBand) touchesTop = true;
                if (y >= bottom - edgeBand) touchesBottom = true;
            } else current = 0;
        }
        int span = bottom - top + 1;
        if (!(touchesTop && touchesBottom && darkRows >= span * .68f
                && longest >= span * .48f)) return false;
        // A rest plus the five horizontal staff lines can satisfy the aggregate
        // coverage test while leaving an entire staff space empty. A barline
        // must also cross each of the four spaces between those lines. Ignore
        // the horizontal lines themselves so they cannot supply that evidence.
        // Use one consistent column, perpendicular to the detected staff slope.
        // Choosing a different dark pixel on every row follows the diagonal stem
        // of a multi-flag rest and can incorrectly make it look like a barline.
        int lineMargin = Math.max(1, Math.round(gap * .14f));
        for (int origin = centerX - 2; origin <= centerX + 2; origin++) {
            int covered = 0, sampled = 0, branched = 0, widthSamples = 0;
            boolean everySpace = true;
            for (int line = 0; line < 4; line++) {
                int start = Math.max(0, Math.round(rows[line] + shift) + lineMargin + 1);
                int end = Math.min(height - 1, Math.round(rows[line + 1] + shift) - lineMargin - 1);
                int spaceCovered = 0;
                for (int y = start; y <= end; y++) {
                    boolean awayFromRule = y - (rows[line] + shift) > gap * .29f
                            && rows[line + 1] + shift - y > gap * .29f;
                    if (awayFromRule) widthSamples++;
                    int x = Math.round(origin - slope * (y - (top + bottom) * .5f));
                    if (x >= 0 && x < width && (gray[y * width + x] & 0xff) <= RAW_BARLINE_DARK) {
                        spaceCovered++;
                        if (!awayFromRule) continue;
                        int reach = Math.max(3, Math.round(gap * .65f));
                        // Estimate the adjacent paper tone. Dark paper must not
                        // turn every thin line into a page-wide branch.
                        int paper = 0, surround = Math.round(gap * 2f);
                        for (int dx = -surround; dx <= surround; dx++)
                            if (x + dx >= 0 && x + dx < width)
                                paper = Math.max(paper, gray[y * width + x + dx] & 255);
                        int branchDark = Math.min(RAW_BARLINE_DARK, Math.max(0, paper - 25));
                        for (int direction = -1; direction <= 1; direction += 2) {
                            int distance = 1;
                            while (distance <= reach && x + direction * distance >= 0
                                    && x + direction * distance < width
                                    && (gray[y * width + x + direction * distance] & 0xff)
                                    <= branchDark) distance++;
                            if (distance > reach) { branched++; break; }
                        }
                    }
                }
                int samples = Math.max(0, end - start + 1);
                covered += spaceCovered;
                sampled += samples;
                if (spaceCovered < samples * .55f) everySpace = false;
            }
            // Stacked meter digits can contain one continuous vertical stroke. Their
            // wide branches occupy many staff-space rows; a thin bar may intersect
            // an occasional beam or slur, but does not have that repeated width.
            if (sampled > 0 && everySpace && covered >= sampled * .90f
                    && branched <= widthSamples * .35f) return true;
        }
        return false;
    }

    private static boolean hasVerticalInk(byte[] labels, int width, int height, int centerX,
                                           int radiusX, int top, int bottom) {
        for (int y = Math.max(0, top); y <= Math.min(height - 1, bottom); y++)
            for (int x = Math.max(0, centerX - radiusX); x <= Math.min(width - 1, centerX + radiusX); x++)
                if (isVerticalInk(labels[y * width + x])) return true;
        return false;
    }

    private static int countLabel(byte[] labels, int width, int height, byte wanted,
                                  int left, int right, int top, int bottom) {
        int count = 0;
        for (int y = Math.max(0, top); y <= Math.min(height - 1, bottom); y++)
            for (int x = Math.max(0, left); x <= Math.min(width - 1, right); x++)
                if (labels[y * width + x] == wanted) count++;
        return count;
    }

    private static List<SystemRun> mergeAlignedStaffs(List<StaffRun> staffs, byte[] gray,
                                                       int width, int height) {
        List<SystemRun> systems = new ArrayList<>();
        for (StaffRun staff : staffs) {
            if (!systems.isEmpty()) {
                SystemRun previous = systems.get(systems.size() - 1);
                float verticalGap = staff.top - previous.bottom;
                float gap = Math.max(previous.gap, staff.gap);
                boolean compactAligned = verticalGap <= gap
                        * MAX_GRAND_STAFF_SEPARATION_GAPS
                        && aligned(previous.boundaries, staff.boundaries, width, gap);
                boolean visiblyConnected = gray != null
                        && verticalGap <= gap * MAX_CONNECTED_STAFF_SEPARATION_GAPS
                        && connectedByVerticalRule(gray, width, height, previous, staff, gap);
                if (compactAligned || visiblyConnected) {
                    previous.bottom = staff.bottom;
                    previous.gap = (previous.gap + staff.gap) / 2f;
                    previous.boundaries = mergeBoundaries(previous.boundaries, staff.boundaries,
                            width, previous.gap);
                    continue;
                }
            }
            systems.add(new SystemRun(staff.top, staff.bottom, staff.gap, staff.boundaries, staff.slope));
        }
        return systems;
    }

    /**
     * Finds a bracket or a barline that physically crosses the whitespace between two staves.
     * Looking only beside already plausible boundaries avoids treating lyrics, dynamics, or one
     * unusually long note stem as a system connector. The raw page is used because the model often
     * labels the upper and lower pieces of one shared barline independently.
     */
    private static boolean connectedByVerticalRule(byte[] gray, int width, int height,
                                                    SystemRun upper, StaffRun lower, float gap) {
        int top = Math.max(0, Math.round(upper.bottom + gap * .12f));
        int bottom = Math.min(height - 1, Math.round(lower.top - gap * .12f));
        if (bottom - top < Math.max(3, Math.round(gap * .45f))) return false;
        List<Integer> candidates = new ArrayList<>(upper.boundaries.size()
                + lower.boundaries.size());
        candidates.addAll(upper.boundaries);
        candidates.addAll(lower.boundaries);
        int horizontalTolerance = Math.max(2, Math.round(gap * 1.65f));
        boolean[] checked = new boolean[width];
        for (int candidate : candidates) {
            int left = Math.max(0, candidate - horizontalTolerance);
            int right = Math.min(width - 1, candidate + horizontalTolerance);
            for (int x = left; x <= right; x++) {
                if (checked[x]) continue;
                checked[x] = true;
                if (verticalRuleAt(gray, width, x, top, bottom, gap)) return true;
            }
        }
        return false;
    }

    static boolean verticalRuleAt(byte[] gray, int width, int centerX, int top,
                                          int bottom, float gap) {
        int radius = Math.max(1, Math.round(gap * .16f));
        int edgeBand = Math.max(2, Math.round(gap * .42f));
        int rows = bottom - top + 1, darkRows = 0, longest = 0, run = 0, blanks = 0;
        boolean touchesTop = false, touchesBottom = false;
        for (int y = top; y <= bottom; y++) {
            int ink = 255;
            for (int x = Math.max(0, centerX - radius);
                 x < Math.min(width, centerX + radius + 1); x++)
                ink = Math.min(ink, gray[y * width + x] & 0xff);
            // A colored/scanned background can be darker than the absolute ink threshold for
            // the entire page. A connector must also be a narrow stroke with lighter paper on
            // BOTH sides, not merely a dark column through otherwise unconnected systems.
            int leftPaper = 0, rightPaper = 0;
            int flank = Math.max(radius + 2, Math.round(gap * .85f));
            for (int offset = radius + 1; offset <= flank; offset++) {
                if (centerX - offset >= 0)
                    leftPaper = Math.max(leftPaper, gray[y * width + centerX - offset] & 0xff);
                if (centerX + offset < width)
                    rightPaper = Math.max(rightPaper, gray[y * width + centerX + offset] & 0xff);
            }
            boolean dark = ink <= RAW_BARLINE_DARK
                    && leftPaper - ink >= 24 && rightPaper - ink >= 24;
            if (dark) {
                darkRows++;
                run += blanks + 1;
                blanks = 0;
                longest = Math.max(longest, run);
                if (y <= top + edgeBand) touchesTop = true;
                if (y >= bottom - edgeBand) touchesBottom = true;
            } else if (++blanks > Math.max(1, Math.round(gap * .18f))) {
                run = 0;
                blanks = 0;
            }
        }
        return touchesTop && touchesBottom && darkRows >= rows * .66f
                && longest >= rows * .72f;
    }

    /** A barline can be faint on only one stave of a grand staff. Once the staves are known to
     * align, retain the union of their independently detected boundaries instead of selecting
     * whichever stave happened to produce the larger list. */
    private static List<Integer> mergeBoundaries(List<Integer> first, List<Integer> second,
                                                  int width, float gap) {
        int tolerance = Math.max(Math.round(gap * 2.2f), width / 38);
        List<Integer> combined = new ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        combined.sort(Integer::compare);
        List<Integer> merged = new ArrayList<>();
        int total = combined.get(0), count = 1;
        for (int index = 1; index < combined.size(); index++) {
            int value = combined.get(index);
            if (value - Math.round(total / (float) count) <= tolerance) {
                total += value;
                count++;
            } else {
                merged.add(Math.round(total / (float) count));
                total = value;
                count = 1;
            }
        }
        merged.add(Math.round(total / (float) count));
        // The per-stave detector already rejects implausibly busy systems. Keep that same safety
        // ceiling after merging rather than allowing two noisy masks to amplify each other.
        if (merged.size() > 32)
            return first.size() >= second.size() ? List.copyOf(first) : List.copyOf(second);
        return List.copyOf(merged);
    }

    private static boolean aligned(List<Integer> first, List<Integer> second, int width, float gap) {
        int smaller = Math.min(first.size(), second.size());
        if (smaller < 2 || Math.abs(first.size() - second.size()) > 1) return false;
        int tolerance = Math.max(Math.round(gap * 2.2f), width / 38);
        int matches = 0;
        for (int value : first) {
            for (int other : second) if (Math.abs(value - other) <= tolerance) { matches++; break; }
        }
        return matches >= Math.ceil(smaller * 0.72f);
    }

    private static void addMeasures(byte[] labels, int width, int height, SystemRun system,
                                    List<MeasureRegion> output) {
        List<Integer> boundaries = system.boundaries;
        for (int index = 0; index + 1 < boundaries.size(); index++) {
            int rawLeft = boundaries.get(index), rawRight = boundaries.get(index + 1);
            float shiftLeft = system.slope * (rawLeft - width / 2f);
            float shiftRight = system.slope * (rawRight - width / 2f);
            float top = Math.max(0f, (system.top + Math.min(shiftLeft, shiftRight)
                    - system.gap * 2.1f) / height);
            float bottom = Math.min(1f, (system.bottom + Math.max(shiftLeft, shiftRight)
                    + system.gap * 2.1f) / height);
            int inset = Math.max(2, Math.round(system.gap * 0.55f));
            int playableLeft = rawLeft + inset;
            if (index == 0) {
                // Only the compact header immediately after the left staff edge can be a clef,
                // key, or time signature. Searching the entire first measure lets an isolated
                // class-3 mistake beside a later note chop most of that measure away.
                int headerLimit = Math.min(rawRight,
                        rawLeft + Math.max(Math.round(system.gap * 8f), width / 30));
                int headerRight = rightmostLabel(labels, width, height, CLEF_OR_KEY,
                        rawLeft, headerLimit, Math.round(system.top - system.gap * 2f),
                        Math.round(system.bottom + system.gap * 2f));
                if (headerRight >= 0) playableLeft = Math.max(playableLeft,
                        headerRight + Math.max(2, Math.round(system.gap * 0.8f)));
                // A stray accidental/clef label beside the first note must not
                // crop that note out of the playable measure. A substantial
                // notehead in the header window is the stopping point.
                int firstHead = firstHeaderHead(labels,width,height,rawLeft,headerLimit,
                        Math.round(system.top-system.gap*2f),Math.round(system.bottom+system.gap*2f),system.gap);
                if(firstHead>=0)playableLeft=Math.min(playableLeft,firstHead-Math.max(2,Math.round(system.gap*.12f)));
            }
            int playableRight = rawRight - inset;
            if (playableRight - playableLeft >= Math.max(6, Math.round(system.gap * 2.2f)))
                output.add(new MeasureRegion(playableLeft / (float) width, playableRight / (float) width,
                        top, bottom));
        }
    }

    private static int rightmostLabel(byte[] labels, int width, int height, byte wanted,
                                      int left, int right, int top, int bottom) {
        for (int x = Math.min(width - 1, right); x >= Math.max(0, left); x--)
            for (int y = Math.max(0, top); y <= Math.min(height - 1, bottom); y++)
                if (labels[y * width + x] == wanted) return x;
        return -1;
    }

    private static boolean continuousStaffExtension(int[] columns,int left,int right) {
        int supported=0;
        for(int x=left;x<=right;x++)if(columns[x]>=4)supported++;
        return supported>=(right-left+1)*.90f;
    }

    private static int firstHeaderHead(byte[] labels,int width,int height,int left,int right,
            int top,int bottom,float gap) {
        for(int x=Math.max(0,left);x<=Math.min(width-1,right);x++) {
            int pixels=0;
            for(int y=Math.max(0,top);y<=Math.min(height-1,bottom);y++)
                if(labels[y*width+x]==NOTEHEAD)pixels++;
            if(pixels>=Math.max(2,Math.round(gap*.20f))
                    &&countLabel(labels,width,height,NOTEHEAD,x,Math.round(x+gap*1.4f),top,bottom)
                    >=Math.max(5,Math.round(gap*gap*.16f))) {
                // The bottom curl of a treble clef can be labelled as a head.
                // A tall clef body at the same x keeps it inside the header.
                int first=-1,last=-1,clefPixels=0;
                for(int y=Math.max(0,top);y<=Math.min(height-1,bottom);y++)
                    for(int xx=Math.max(0,Math.round(x-gap*.7f));xx<=Math.min(width-1,Math.round(x+gap*1.7f));xx++)
                        if(labels[y*width+xx]==CLEF_OR_KEY){if(first<0)first=y;last=y;clefPixels++;}
                if(last-first>gap*4f&&clefPixels>gap*gap*1.6f)continue;
                return x;
            }
        }
        return -1;
    }

    private record StaffRun(int top, int bottom, float gap, int left, int right,
                            List<Integer> boundaries, float slope) { }

    private static final class SystemRun {
        final int top;
        int bottom;
        float gap;
        List<Integer> boundaries;
        final float slope;
        SystemRun(int top, int bottom, float gap, List<Integer> boundaries, float slope) {
            this.top = top; this.bottom = bottom; this.gap = gap;
            this.boundaries = boundaries; this.slope = slope;
        }
    }
}
