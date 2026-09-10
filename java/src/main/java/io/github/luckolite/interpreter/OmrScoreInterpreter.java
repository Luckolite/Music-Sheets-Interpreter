// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts the segmentation model's staff/notehead mask into a deliberately transparent diagnostic melody. */
final class OmrScoreInterpreter {
    /** Two staves in one grand staff are close; consecutive compact violin systems are not. */
    private static final float MAX_STAFFS_IN_SYSTEM_SEPARATION_GAPS = 6.75f;
    /** D7/E7 in treble clef reaches roughly six to six-and-a-half staff gaps above the stave. */
    private static final float MAX_HEAD_LEDGER_GAPS = 6.75f;

    private OmrScoreInterpreter() { }

    static List<PlayingTechniqueDetector.Staff> techniqueStaffs(byte[] labels,byte[] gray,int width,int height,
                                                               List<MeasureRegion> measures) {
        List<PlayingTechniqueDetector.Staff> result=new ArrayList<>();
        for(Staff staff:findStaffs(labels,gray,width,height,measures))result.add(new PlayingTechniqueDetector.Staff(
                staff.top,staff.bottom,staff.gap,staff.index,staff.count));
        return result;
    }

    static List<ScoreNoteEvent> extract(byte[] labels, int width, int height,
                                        List<MeasureRegion> measures) {
        return extract(labels, null, width, height, measures);
    }

    static List<ScoreNoteEvent> extract(byte[] labels, byte[] gray, int width, int height,
                                        List<MeasureRegion> measures) {
        return analyze(labels, gray, width, height, measures).notes();
    }

    static Analysis analyze(byte[] labels, byte[] gray, int width, int height,
                            List<MeasureRegion> measures) {
        if (labels == null || labels.length != width * height || width <= 0 || height <= 0
                || measures == null || measures.isEmpty()) return new Analysis(List.of(), List.of());
        List<Staff> staffs = findStaffs(labels, gray, width, height, measures);
        if (staffs.isEmpty()) return new Analysis(List.of(), List.of());
        List<Component> rawHeadComponents = findComponents(labels, width, height,
                OmrMeasurePostProcessor.NOTEHEAD);
        List<Component> clefOrKeyComponents = findComponents(labels, width, height,
                OmrMeasurePostProcessor.CLEF_OR_KEY);
        rawHeadComponents.removeIf(head->isRoundedHeaderMeter(labels,gray,width,height,
                head,staffs,clefOrKeyComponents));
        rawHeadComponents.removeIf(head -> commonTimeGlyphBounds(labels, gray, width, height,
                head, staffs, clefOrKeyComponents) != null);
        rawHeadComponents.removeIf(head -> isTempoUnitHead(gray, width, height, head, staffs));
        rawHeadComponents.removeIf(head -> isHeavyRestBarFragment(gray, width, height, head, staffs));
        List<Component> headComponents = splitStackedHeads(labels, gray, width, height,
                rawHeadComponents, staffs);
        List<Component> symbolComponents = findComponents(labels, width, height,
                OmrMeasurePostProcessor.SYMBOL);
        List<Component> heads = new ArrayList<>();
        for (Component head : headComponents) {
            Staff staff = staffForHead(labels, gray, width, height, staffs, head);
            if (staff != null && plausibleHead(head, staff.gap)
                    && !flatStemlessFragment(gray, width, height, head, staff.gap)) heads.add(head);
        }
        // A bright paper halo can enlarge a printed augmentation dot enough for the model to label it
        // as a second plausible notehead. Demote only small, stemless components immediately to
        // the right of a substantially larger head; grace notes retain their attached stem.
        List<Component> demotedDotHeads = augmentationDotHeads(labels, width, height, heads, staffs);
        demotedDotHeads.addAll(articulationDotHeads(labels, gray, width, height, heads, staffs));
        heads.removeAll(demotedDotHeads);
        heads.removeAll(stemSlashFragments(gray, width, height, heads, staffs));
        heads.removeIf(head -> isHeaderMeterDigit(labels,gray,width,height,head,staffs,clefOrKeyComponents));
        List<Component> dotCandidates = new ArrayList<>(symbolComponents);
        for (Component component : headComponents) if (!heads.contains(component))
            dotCandidates.add(component);
        List<AccidentalCandidate> accidentalCandidates = new ArrayList<>();
        for (Component component : clefOrKeyComponents)
            accidentalCandidates.add(new AccidentalCandidate(component,
                    OmrMeasurePostProcessor.CLEF_OR_KEY));
        for (Component component : symbolComponents)
            accidentalCandidates.add(new AccidentalCandidate(component,
                    OmrMeasurePostProcessor.SYMBOL));
        List<AccidentalCandidate> localAccidentals = joinLocalAccidentalFragments(
                labels, gray, width, height, accidentalCandidates, staffs);
        List<Component> accidentalInk = new ArrayList<>();
        for (AccidentalCandidate candidate : localAccidentals) {
            Staff staff = nearestHeadStaff(staffs, candidate.component.centerY);
            if (staff != null && (isFlatGlyph(labels,width,height,candidate,staff.gap)
                    || isNaturalGlyph(labels,width,height,candidate,staff.gap)
                    || isSharpGlyph(labels,width,height,candidate,staff.gap)))
                accidentalInk.add(candidate.component);
        }
        List<DetectedNote> detected = new ArrayList<>();
        for (Component head : heads) {
            Staff staff = staffForHead(labels, gray, width, height, staffs, head);
            if (staff == null) continue;
            // Heads far outside a staff require printed ledger lines. A nearby
            // text stroke can look like a stem, so a semantic stem alone cannot
            // promote a tempo digit or other text into an extreme pitch.
            // Cross-staff stems may assign a note to the other voice. Validate
            // its printed position against the nearest physical staff instead.
            Staff physicalStaff = nearestHeadStaff(staffs,head.centerY);
            if (gray != null && physicalStaff != null
                    && (head.centerY < physicalStaff.top-physicalStaff.gap*1.8f
                    || head.centerY > physicalStaff.bottom+physicalStaff.gap*1.8f)
                    && (!hasLedgerInk(gray,width,height,head,physicalStaff.gap)
                    || !hasInnerLedgerInk(gray,width,height,head,physicalStaff))) continue;
            float normalizedX = head.centerX() / width;
            float normalizedY = head.centerY() / height;
            int measureIndex = containingMeasureForStaff(measures, normalizedX, normalizedY,
                    staff, height);
            if (measureIndex < 0) continue;
            MeasureRegion measure = measures.get(measureIndex);
            // Repeated geometry represents an expanded multi-measure rest. Its printed count
            // can look like a hollow notehead, but every logical bar in that span is silent.
            if ((measureIndex > 0 && measure.equals(measures.get(measureIndex - 1)))
                    || (measureIndex + 1 < measures.size() && measure.equals(measures.get(measureIndex + 1)))) continue;
            float position = (normalizedX - measure.left()) / Math.max(0.0001f,
                    measure.right() - measure.left());
            float[] localPitch = localStaffPitch(labels, gray, width, height, staff, head);
            float localBottom=localPitch[0],localGap=localPitch[1];
            int step = Math.round((localBottom - head.centerY()) / (localGap * 0.5f));
            int beamCount = detectBeamCount(labels, gray, width, height, head, staff);
            float unbeamedDuration = detectUnbeamedDuration(labels, gray, width, height,
                    head, staff.gap, beamCount);
            // Beamed notes cannot have open heads. If a staff line, slur, or artwork edge near
            // an open half/whole head looked like a beam, retain the notehead's stronger direct
            // evidence instead of collapsing the sustained passage into eighths/sixteenths.
            if (unbeamedDuration >= ScoreNoteEvent.DURATION_HALF) beamCount = 0;
            int augmentationDots = countAugmentationDots(dotCandidates, head, staff.gap,
                    gray, width, height,unbeamedDuration>=ScoreNoteEvent.DURATION_HALF,accidentalInk);
            if(augmentationDots>0&&beamCount>0&&hasHollowUnisonToRight(labels,gray,width,height,head,heads,staff.gap))
                augmentationDots=0;
            int writtenAccidental = detectWrittenAccidental(labels, width, height,
                    localAccidentals, head, localPitch[1]);
            if((writtenAccidental==ScoreNoteEvent.ACCIDENTAL_FROM_KEY
                    ||writtenAccidental==ScoreNoteEvent.ACCIDENTAL_FLAT)
                    &&rawNaturalFromCrossbars(gray,width,height,localAccidentals,head,localPitch[1]))
                writtenAccidental=ScoreNoteEvent.ACCIDENTAL_NATURAL;
            ScoreNoteEvent event = new ScoreNoteEvent(measureIndex, clamp(position),
                    Math.max(-32, Math.min(32, step)), staff.index, staff.count,
                    clamp(normalizedY), false, augmentationDots, beamCount,
                    writtenAccidental, unbeamedDuration);
            List<Component> unison=sideBySideUnison(labels,gray,width,height,head,staff.gap);
            if(!unison.isEmpty()) {
                // Opposite stems share a printed pitch/attack but have separate durations.
                for(int partIndex=0;partIndex<unison.size();partIndex++) {
                    Component part=unison.get(partIndex);
                    int partBeams=partIndex==0?detectBeamCount(labels,gray,width,height,part,staff):0;
                    float partDuration=partIndex==0?(partBeams>0?0:1):2;
                    int partDots=partIndex==0?0:countAugmentationDots(dotCandidates,part,staff.gap,gray,width,height,true);
                    var separate=new ScoreNoteEvent(measureIndex,clamp(position),step,staff.index,staff.count,
                            clamp(normalizedY),false,partDots,partBeams,writtenAccidental,partDuration);
                    detected.add(new DetectedNote(separate,part,staff.gap));
                }
                continue;
            }
            List<Component> seconds=sideBySideSeconds(labels,width,head,staff.gap);
            if(seconds.isEmpty())detected.add(new DetectedNote(event, head, staff.gap));
            else for(Component part:seconds) {
                int partStep=Math.round((localBottom-part.centerY)/(localGap*.5f));
                // Displaced seconds share the stem/attack, despite their two horizontal centres.
                var chord=new ScoreNoteEvent(measureIndex,clamp(position),partStep,staff.index,staff.count,
                        clamp(part.centerY/height),false,augmentationDots,beamCount,writtenAccidental,unbeamedDuration);
                detected.add(new DetectedNote(chord,part,staff.gap));
            }
        }
        detected = alignDisplacedSeconds(detected,gray,width,height);
        detected = applyPrintedClefs(detected, staffs, clefOrKeyComponents, labels, gray, width, height);
        detected.sort(Comparator.comparingInt((DetectedNote note) -> note.event.measureIndex())
                .thenComparingDouble(note -> note.event.positionInMeasure())
                .thenComparingInt(note -> note.event.staffIndex())
                .thenComparingInt(note -> note.event.staffStep()));
        List<DetectedNote> joined = applyAccidentalState(markTieContinuations(labels, gray,
                width, height, removeSplitDuplicates(detected)));
        logHeadCoverage(staffs, rawHeadComponents, headComponents, heads, demotedDotHeads, joined);
        List<ScoreNoteEvent> result = new ArrayList<>(joined.size());
        for (DetectedNote note : joined) result.add(note.event);
        List<ScoreNoteEvent> notation=TripletRhythmDetector.withoutNumeralHeads(result,measures,gray,width,height);
        if(notation.size()!=result.size()) {
            for(DetectedNote note:joined)if(!notation.contains(note.event))heads.remove(note.head);
            joined.removeIf(note->!notation.contains(note.event));
            result.clear();result.addAll(notation);
        }
        List<SixteenthRestDetector.Staff> restStaffs = new ArrayList<>();
        for (Staff staff : staffs) restStaffs.add(new SixteenthRestDetector.Staff(
                staff.pitchBottom - staff.pitchGap * 4, staff.pitchBottom, staff.pitchGap,
                staff.index, staff.count, staff.pitchTrack));
        List<ScoreRestEvent> rests = SixteenthRestDetector.detect(gray, width, height, measures, restStaffs, result);
        // Small stemless model heads can be augmentation dots of an independently
        // recognized rest. Re-read those dots without letting the mistaken head
        // claim ownership, but require the same rest to have survived the first pass.
        List<DetectedNote> compactDots=new ArrayList<>();
        for(DetectedNote note:joined) {
            Component h=note.head;float gap=note.staffGap;
            if(h.maxX-h.minX+1<=gap*.7f&&h.maxY-h.minY+1<=gap*.7f
                    &&h.area<=gap*gap*.32f&&gray!=null
                    &&attachedRawStem(gray,width,height,h,gap)==null)compactDots.add(note);
        }
        if(!compactDots.isEmpty()) {
            List<ScoreNoteEvent> owners=new ArrayList<>(result);
            for(DetectedNote note:compactDots)owners.remove(note.event);
            var evidence=SixteenthRestDetector.detectWithDots(gray,width,height,measures,restStaffs,owners);
            List<DetectedNote> removed=new ArrayList<>();
            // The same compact prediction may be inside the rest's zigzag, not
            // beside it. A complete independently recognized quarter-rest body
            // can establish that ownership even when the false head blocked the
            // original rest pass. Actual stemmed heads never enter compactDots.
            for(DetectedNote note:compactDots)for(ScoreRestEvent rest:evidence.rests()) {
                if(rest.durationBeats()<1||rest.durationBeats()>1.75
                        ||rest.measureIndex()!=note.event.measureIndex()
                        ||rest.staffIndex()!=note.event.staffIndex()
                        ||rest.staffCount()!=note.event.staffCount())continue;
                MeasureRegion region=measures.get(rest.measureIndex());
                float x=(region.left()+rest.positionInMeasure()*(region.right()-region.left()))*width;
                if(Math.abs(x-note.head.centerX)<=note.staffGap*.4f
                        &&Math.abs(rest.pageY()*height-note.head.centerY)<=rest.pageHeight()*height*.5f) {
                    removed.add(note);break;
                }
            }
            for(DetectedNote note:compactDots)for(var dot:evidence.dots()) {
                ScoreRestEvent parent=dot.rest();
                if(parent.measureIndex()!=note.event.measureIndex()||parent.staffIndex()!=note.event.staffIndex()
                        ||parent.staffCount()!=note.event.staffCount()
                        ||Math.abs(dot.x()-note.head.centerX)>note.staffGap*.3f
                        ||Math.abs(dot.y()-note.head.centerY)>note.staffGap*.3f)continue;
                boolean verified=rests.stream().anyMatch(rest->rest.measureIndex()==parent.measureIndex()
                        &&rest.staffIndex()==parent.staffIndex()&&rest.staffCount()==parent.staffCount()
                        &&Math.abs(rest.positionInMeasure()-parent.positionInMeasure())<.005f
                        &&Math.abs(rest.pageY()-parent.pageY())<.005f);
                if(verified){removed.add(note);break;}
            }
            if(!removed.isEmpty()) {
                joined.removeAll(removed);
                for(DetectedNote note:removed){result.remove(note.event);heads.remove(note.head);}
                rests=SixteenthRestDetector.detect(gray,width,height,measures,restStaffs,result);
            }
        }
        List<ScoreKeyChange> keyChanges = detectKeyChanges(labels, gray, width, height, measures,
                staffs, accidentalCandidates, heads);
        List<ScoreNoteEvent> withRests = new ArrayList<>();
        for (DetectedNote note : joined) {
            ScoreNoteEvent event = note.event;
            float next = 1.01f;
            for (ScoreNoteEvent other : result) if (other.measureIndex() == event.measureIndex()
                    && other.staffIndex() == event.staffIndex() && other.staffCount() == event.staffCount()
                    && other.positionInMeasure() > event.positionInMeasure() + .018f)
                next = Math.min(next, other.positionInMeasure());
            float silence = 0;
            boolean hasSixteenthRest=false;
            for (ScoreRestEvent rest : rests) if (rest.measureIndex() == event.measureIndex()
                    && rest.staffIndex() == event.staffIndex() && rest.staffCount() == event.staffCount()
                    && rest.positionInMeasure() > event.positionInMeasure() && rest.positionInMeasure() < next
                    &&(!ScoreNoteTiming.hasIndependentSustain(event)
                    ||rest.positionInMeasure()>event.positionInMeasure()+.018f)) {
                silence += (float)rest.durationBeats();
                hasSixteenthRest|=rest.durationBeats()==.25;
            }
            float leading = 0;
            boolean first = result.stream().noneMatch(other->other.measureIndex()==event.measureIndex()
                    &&other.staffIndex()==event.staffIndex()&&other.staffCount()==event.staffCount()
                    &&other.positionInMeasure()<event.positionInMeasure()-.018f);
            if(first)for(ScoreRestEvent rest:rests)if(rest.measureIndex()==event.measureIndex()
                    &&rest.staffIndex()==event.staffIndex()&&rest.staffCount()==event.staffCount()
                    &&rest.positionInMeasure()<event.positionInMeasure()
                    &&(!ScoreNoteTiming.hasIndependentSustain(event)
                    ||rest.positionInMeasure()<event.positionInMeasure()-.018f))leading+=(float)rest.durationBeats();
            // The first moving attack can follow a printed rest while another voice
            // already holds a half note in that rest's column.
            if(!first&&!ScoreNoteTiming.hasIndependentSustain(event)
                    &&result.stream().noneMatch(other->other.measureIndex()==event.measureIndex()
                    &&other.staffIndex()==event.staffIndex()&&other.staffCount()==event.staffCount()
                    &&other.positionInMeasure()<event.positionInMeasure()-.018f
                    &&!ScoreNoteTiming.hasIndependentSustain(other))) {
                for(ScoreRestEvent rest:rests)if(rest.measureIndex()==event.measureIndex()
                        &&rest.staffIndex()==event.staffIndex()&&rest.staffCount()==event.staffCount()
                        &&rest.positionInMeasure()<event.positionInMeasure()-.018f
                        &&result.stream().anyMatch(other->other.measureIndex()==event.measureIndex()
                        &&other.staffIndex()==event.staffIndex()&&other.staffCount()==event.staffCount()
                        &&ScoreNoteTiming.hasIndependentSustain(other)
                        &&Math.abs(other.positionInMeasure()-rest.positionInMeasure())<=.018f))
                    leading+=(float)rest.durationBeats();
            }
            int beams = event.beamCount();
            if (hasSixteenthRest && beams <= 2 && !ScoreNoteTiming.hasIndependentSustain(event)) {
                int flags = rawDetachedFlags(gray, labels, width, height, note.head, note.staffGap);
                if (flags > 0) beams = flags;
            }
            withRests.add(new ScoreNoteEvent(event.measureIndex(), event.positionInMeasure(), event.staffStep(),
                    event.staffIndex(), event.staffCount(), event.pageY(), event.tiedFromPrevious(),
                    dotsOutsideRests(dotCandidates, note, rests, measures, gray, width, height, accidentalInk),
                    beams, event.writtenAccidental(),
                    beams != event.beamCount() ? 0 : event.unbeamedDurationBeats(), event.tupletDivisor(), silence,
                    event.articulations(), event.clefBottomDiatonic()).withLeadingRest(leading));
        }
        List<NoteArticulationDetector.Anchor> anchors = new ArrayList<>();
        for (DetectedNote note : joined) anchors.add(new NoteArticulationDetector.Anchor(
                note.head.centerX, note.head.centerY, note.staffGap,
                staffs.indexOf(staffForHead(labels,gray,width,height,staffs,note.head))));
        int[] marks = NoteArticulationDetector.detect(labels,gray,width,height,anchors);
        for (int i=0;i<withRests.size();i++) withRests.set(i,withRests.get(i).withArticulations(marks[i]));
        markGraceHeads(labels, gray, width, height, joined, withRests);
        for (int i=1;i<joined.size();i++) {
            int prior=i-1;
            while(prior>=0&&ScoreNoteTiming.hasIndependentSustain(joined.get(prior).event))prior--;
            if(prior<0)continue;
            DetectedNote a=joined.get(prior),b=joined.get(i);
            if(a.event.measureIndex()!=b.event.measureIndex()||a.event.staffCount()!=2
                    ||b.event.staffCount()!=2||a.event.staffIndex()==b.event.staffIndex()
                    ||ScoreNoteTiming.hasIndependentSustain(a.event)||ScoreNoteTiming.hasIndependentSustain(b.event))continue;
            // Held heads are independent of the moving phrase. Require a single attack stream
            // among the moving heads before proving its cross-staff beam from raw ink.
            boolean singlePhrase=true;float previousPosition=-1;
            for(ScoreNoteEvent n:withRests) if(n.measureIndex()==a.event.measureIndex()) {
                if(ScoreNoteTiming.hasIndependentSustain(n))continue;
                if(n.followingRestBeats()>0
                        ||n.positionInMeasure()-previousPosition<.018f) {singlePhrase=false;break;}
                previousPosition=n.positionInMeasure();
            }
            if(!singlePhrase)continue;
            if(CrossStaffBeamDetector.connected(gray,width,height,a.head.centerX,a.head.centerY,
                    b.head.centerX,b.head.centerY,(a.staffGap+b.staffGap)/2)) {
                withRests.set(prior,withRests.get(prior).withCrossStaffBeam());
                withRests.set(i,withRests.get(i).withCrossStaffBeam());
            }
        }
        return new Analysis(withRests, keyChanges, rests);
    }

    /** Excludes proven non-note header ink before OCR rest reconciliation, without modifying input masks. */
    static byte[] normalizeHeaderSymbols(byte[] labels, byte[] gray, int width, int height,
                                         List<MeasureRegion> measures) {
        if (labels == null || gray == null || width <= 0 || height <= 0
                || labels.length != (long) width * height || gray.length != labels.length
                || measures == null || measures.isEmpty()) return labels;
        List<Staff> staffs = findStaffs(labels, gray, width, height, measures);
        List<Component> heads = findComponents(labels, width, height, OmrMeasurePostProcessor.NOTEHEAD);
        List<Component> glyphs = findComponents(labels, width, height, OmrMeasurePostProcessor.CLEF_OR_KEY);
        byte[] result = labels;
        for (Component head : heads) {
            int[] bounds = commonTimeGlyphBounds(labels, gray, width, height, head, staffs, glyphs);
            if (bounds == null && isTempoUnitHead(gray, width, height, head, staffs))
                bounds = new int[]{head.minX, head.maxX, head.minY, head.maxY};
            if (bounds == null && isRoundedHeaderMeter(labels, gray, width, height, head, staffs, glyphs))
                bounds = new int[]{head.minX, head.maxX, head.minY, head.maxY};
            if (bounds == null && isHeavyRestBarFragment(gray, width, height, head, staffs))
                bounds = new int[]{head.minX, head.maxX, head.minY, head.maxY};
            if (bounds == null) continue;
            for (int y = bounds[2]; y <= bounds[3]; y++) for (int x = bounds[0]; x <= bounds[1]; x++) {
                int at = y * width + x;
                if (result[at] == OmrMeasurePostProcessor.NOTEHEAD) {
                    if (result == labels) result = labels.clone();
                    // Keep source ink for OCR; do not promote removed fragments to accidental candidates.
                    result[at] = 0;
                }
            }
        }
        return result;
    }

    /** A compact prediction inside the thick centre of a two-capped multimeasure rest.
     * Both end caps must extend beyond both edges of the horizontal band. */
    private static boolean isHeavyRestBarFragment(byte[] gray,int width,int height,
            Component head,List<Staff> staffs) {
        if(gray==null)return false;
        Staff staff=nearestHeadStaff(staffs,head.centerY);if(staff==null)return false;
        float gap=staff.pitchGap;
        if(head.maxX-head.minX+1>gap*.8f||head.maxY-head.minY+1>gap*1.25f
                ||head.area>gap*gap*.4f||Math.abs(head.centerY-(staff.pitchBottom-gap*2))>gap*.7f)return false;
        for(int y=head.minY;y<=head.maxY;y++)
            if(heavyRestBarAtRow(gray,width,height,Math.round(head.centerX),y,gap))return true;
        return false;
    }

    private static boolean heavyRestBarAtRow(byte[] gray,int width,int height,int x,int y,float gap) {
        // A thin staff rule can run through the centre of the thick rest. Inspect
        // the predicted fragment's rows so that rule cannot hide the end caps.
        if((gray[y*width+x]&255)>=165)return false;
        int left=x,right=x;
        while(left>0&&(gray[y*width+left-1]&255)<165)left--;
        while(right+1<width&&(gray[y*width+right+1]&255)<165)right++;
        if(right-left<gap*6||right-left>gap*60)return false;
        int top=y,bottom=y;
        while(top>0&&restBandRow(gray,width,left,right,top-1))top--;
        while(bottom+1<height&&restBandRow(gray,width,left,right,bottom+1))bottom++;
        int thick=bottom-top+1;
        if(thick<gap*.35f||thick>gap*1.2f)return false;
        return restEndCap(gray,width,height,left,top,bottom,gap)
                &&restEndCap(gray,width,height,right,top,bottom,gap);
    }

    private static boolean restBandRow(byte[] gray,int width,int left,int right,int y) {
        for(int i=1;i<=5;i++)if((gray[y*width+left+(right-left)*i/6]&255)>=165)return false;
        return true;
    }

    private static boolean restEndCap(byte[] gray,int width,int height,int edge,
            int top,int bottom,float gap) {
        int margin=Math.max(2,Math.round(gap*.4f));
        if(top-margin<0||bottom+margin>=height)return false;
        for(int x=Math.max(0,Math.round(edge-gap*.25f));x<=Math.min(width-1,Math.round(edge+gap*.25f));x++) {
            boolean solid=true;
            for(int y=top-margin;y<=bottom+margin;y++)if((gray[y*width+x]&255)>=165){solid=false;break;}
            if(solid)return true;
        }
        return false;
    }

    /** A note followed by an equals sign and text above the staff is a tempo beat unit. */
    private static boolean isTempoUnitHead(byte[] gray, int width, int height,
                                           Component head, List<Staff> staffs) {
        if (gray == null) return false;
        Staff staff = nearestHeadStaff(staffs, head.centerY);
        if (staff == null) return false;
        float gap = staff.pitchGap, top = staff.pitchBottom - gap * 4;
        float y = head.centerY, x = head.maxX;
        if (head.maxY > top + gap * .15f || y < top - gap * 3.5f) return false;
        int[] stem = attachedRawStem(gray, width, height, head, gap);
        if (stem == null || stem[1] > y - gap * 1.5f) return false;
        List<TempoInk> glyphs = tempoInk(gray, width, height,
                Math.round(x + gap * .1f), Math.round(x + gap * 5.5f),
                Math.round(y - gap * 2), Math.round(y + gap * .3f));
        for (TempoInk upper : glyphs) for (TempoInk lower : glyphs) {
            if (upper.top >= lower.top || upper.width() < gap * .5f || upper.width() > gap * 1.6f
                    || lower.width() < gap * .5f || lower.width() > gap * 1.6f
                    || upper.height() > gap * .25f || lower.height() > gap * .25f
                    || upper.area < upper.width() * upper.height() * .7f
                    || lower.area < lower.width() * lower.height() * .7f
                    || Math.abs(upper.left - lower.left) > gap * .15f
                    || Math.abs(upper.right - lower.right) > gap * .2f
                    || lower.top - upper.bottom < gap * .08f
                    || lower.top - upper.bottom > gap * .5f
                    || lower.bottom - upper.top > gap * .8f
                    || upper.left - x > gap * 2.5f || lower.bottom > y + gap * .1f) continue;
            // This is shape evidence for following text, not OCR of the BPM value.
            // Ordinary ledger rules are farther apart; connected beams are not two isolated strokes.
            for (TempoInk text : glyphs) {
                if (text.left > lower.right + gap * .15f && text.left < lower.right + gap * 1.5f
                        && text.height() > gap * .75f && text.height() < gap * 2
                        && text.area > gap * gap * .18f
                        && text.top < upper.top && text.bottom > lower.bottom) return true;
            }
        }
        return false;
    }

    private record TempoInk(int left, int right, int top, int bottom, int area) {
        int width() { return right - left + 1; }
        int height() { return bottom - top + 1; }
    }

    /** Complete printed components only; clipping a letter must not manufacture an equals stroke. */
    private static List<TempoInk> tempoInk(byte[] gray, int width, int height,
                                          int left, int right, int top, int bottom) {
        left = Math.max(0, left); right = Math.min(width - 1, right);
        top = Math.max(0, top); bottom = Math.min(height - 1, bottom);
        int rw = right - left + 1, rh = bottom - top + 1;
        if (rw <= 0 || rh <= 0) return List.of();
        boolean[] seen = new boolean[rw * rh];
        int[] queue = new int[seen.length];
        List<TempoInk> result = new ArrayList<>();
        for (int start = 0; start < seen.length; start++) {
            if (seen[start] || (gray[(top + start / rw) * width + left + start % rw] & 255) >= 155)
                continue;
            int take = 0, size = 1, minX = rw, maxX = -1, minY = rh, maxY = -1;
            boolean edge = false;
            queue[0] = start; seen[start] = true;
            while (take < size) {
                int pos = queue[take++], x = pos % rw, y = pos / rw;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                edge |= x == 0 || y == 0 || x == rw - 1 || y == rh - 1;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || nx >= rw || ny < 0 || ny >= rh) continue;
                    int next = ny * rw + nx;
                    if (!seen[next] && (gray[(top + ny) * width + left + nx] & 255) < 155) {
                        seen[next] = true; queue[size++] = next;
                    }
                }
            }
            if (!edge) result.add(new TempoInk(left + minX, left + maxX,
                    top + minY, top + maxY, size));
        }
        return result;
    }

    /** The tall open-right C can contribute tiny false heads at its curved terminals. */
    private static int[] commonTimeGlyphBounds(byte[] labels, byte[] gray, int width, int height,
                                              Component head, List<Staff> staffs, List<Component> glyphs) {
        if (gray == null) return null;
        Staff staff = nearestHeadStaff(staffs, head.centerY);
        if (staff == null) return null;
        float gap = staff.pitchGap, bottom = staff.pitchBottom, top = bottom - gap * 4;
        float mid = (top + bottom) * .5f;
        if (head.area > gap * gap * .5f || head.maxX - head.minX > gap * .8f
                || head.maxY - head.minY > gap * .8f || Math.abs(head.centerY - mid) > gap * 1.15f)
            return null;
        boolean header = false;
        for (Component glyph : glyphs) {
            if (glyph.maxX < head.minX && head.minX - glyph.maxX < gap * 9
                    && glyph.maxY - glyph.minY > gap * 4.5f && glyph.maxX - glyph.minX > gap * 1.1f
                    && Math.abs(glyph.centerY - mid) < gap * 3) header = true;
        }
        if (!header) return null;
        int[] stem = attachedRawStem(gray, width, height, head, gap);
        if (stem != null && (stem[1] < top - gap * .25f || stem[1] > bottom + gap * .25f)) return null;

        // Follow the printed glyph across narrow antialiasing gaps, ignoring the five staff rules.
        int searchLeft = Math.max(0, Math.round(head.minX - gap * 2.5f));
        int searchRight = Math.min(width - 1, Math.round(head.maxX + gap * 2.5f));
        int[] columns = new int[searchRight - searchLeft + 1];
        int scanTop = Math.max(0, Math.round(top)), scanBottom = Math.min(height - 1, Math.round(bottom));
        for (int x = searchLeft; x <= searchRight; x++) for (int y = scanTop; y <= scanBottom; y++) {
            if (offHeaderStaffLine(y, top, gap) && (gray[y * width + x] & 255) < 155)
                columns[x - searchLeft]++;
        }
        int left = head.minX, right = head.maxX, blank = 0;
        int maxBlank = Math.max(1, Math.round(gap * .15f));
        for (int x = left - 1; x >= searchLeft; x--) {
            if (columns[x - searchLeft] > 0) { left = x; blank = 0; }
            else if (++blank > maxBlank) break;
        }
        blank = 0;
        for (int x = right + 1; x <= searchRight; x++) {
            if (columns[x - searchLeft] > 0) { right = x; blank = 0; }
            else if (++blank > maxBlank) break;
        }
        if (left == searchLeft || right == searchRight || right - left < gap * .9f
                || right - left > gap * 2.5f) return null;
        int noteInk = 0, minY = height, maxY = -1;
        for (int y = scanTop; y <= scanBottom; y++) for (int x = left; x <= right; x++) {
            if (labels[y * width + x] == OmrMeasurePostProcessor.NOTEHEAD) noteInk++;
            if (offHeaderStaffLine(y, top, gap) && (gray[y * width + x] & 255) < 155) {
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        }
        if (noteInk > gap * gap * .55f || maxY - minY < gap * 2.2f || maxY - minY > gap * 3.8f)
            return null;
        int span = right - left;
        int[] open = headerInkBand(gray, width, height, Math.round(left + span * .65f), right,
                Math.round(mid - gap * .25f), Math.round(mid + gap * .25f), top, gap);
        int[] upper = headerInkBand(gray, width, height, Math.round(left + span * .60f), right,
                Math.round(mid - gap * 1.05f), Math.round(mid - gap * .35f), top, gap);
        int[] lower = headerInkBand(gray, width, height, Math.round(left + span * .60f), right,
                Math.round(mid + gap * .35f), Math.round(mid + gap * 1.05f), top, gap);
        int[] leftUp = headerInkBand(gray, width, height, left, Math.round(left + span * .35f),
                Math.round(mid - gap * .65f), Math.round(mid - gap * .15f), top, gap);
        int[] leftDown = headerInkBand(gray, width, height, left, Math.round(left + span * .35f),
                Math.round(mid + gap * .15f), Math.round(mid + gap * .65f), top, gap);
        // Require both curved terminals and the left arc, with a genuinely open middle at the right.
        if (open[1] == 0 || open[0] > open[1] * .2f || upper[0] < gap * .7f || lower[0] < gap * .7f
                || leftUp[0] < gap * .7f || leftDown[0] < gap * .7f) return null;
        return new int[]{left, right, scanTop, scanBottom};
    }

    private static boolean offHeaderStaffLine(int y, float top, float gap) {
        return Math.abs((y - top) / gap - Math.round((y - top) / gap)) * gap > Math.max(1, gap * .15f);
    }

    private static int[] headerInkBand(byte[] gray, int width, int height, int left, int right,
                                      int top, int bottom, float staffTop, float gap) {
        int ink = 0, total = 0;
        for (int y = Math.max(0, top); y <= Math.min(height - 1, bottom); y++) {
            if (!offHeaderStaffLine(y, staffTop, gap)) continue;
            for (int x = Math.max(0, left); x <= Math.min(width - 1, right); x++) {
                total++;
                if ((gray[y * width + x] & 255) < 155) ink++;
            }
        }
        return new int[]{ink, total};
    }


    /** Stacked rounded meter digits can arrive as one tall semantic head blob.
     * Inspect their printed counters before splitting that blob into chord tones. */
    private static boolean isRoundedHeaderMeter(byte[] labels,byte[] gray,int width,int height,
            Component head,List<Staff> staffs,List<Component> glyphs) {
        if(gray==null)return false;
        Staff staff=nearestHeadStaff(staffs,head.centerY);if(staff==null)return false;
        float gap=staff.gap;
        boolean whole=head.maxY-head.minY>=gap*3&&head.maxY-head.minY<=gap*4.3f
                &&head.maxX-head.minX<=gap*1.9f&&head.maxX-head.minX>=gap*.65f
                &&head.minY>=staff.top-gap*.2f&&head.maxY<=staff.bottom+gap*.3f;
        boolean denominator=head.maxY-head.minY>=gap*1.45f&&head.maxY-head.minY<=gap*2.65f
                &&head.maxX-head.minX<=gap*2.2f&&head.maxX-head.minX>=gap*.65f
                &&head.minY+1>=staff.top+gap*1.75f&&head.centerY>=staff.top+gap*2.2f
                &&head.maxY<=staff.bottom+gap*.3f;
        if(!whole&&!denominator)return false;
        boolean header=false;
        for(Component glyph:glyphs)if(glyph.maxX<head.minX&&head.minX-glyph.maxX<gap*7
                &&glyph.maxY-glyph.minY>gap*4.5f&&glyph.maxX-glyph.minX>gap*1.1f
                &&glyph.centerY>staff.top-gap&&glyph.centerY<staff.bottom+gap)header=true;
        // Meter changes also occur after a barline within or at the end of a system.
        // Require a complete printed rule, not a short note stem or numeral stroke.
        if(!header)for(int x=Math.max(0,Math.round(head.minX-gap*3.8f));
                x<Math.min(width,Math.round(head.minX-gap*.3f));x++) {
            int ink=0,total=0;
            for(int y=Math.max(0,Math.round(staff.top));y<=Math.min(height-1,Math.round(staff.bottom));y++) {
                total++;if((gray[y*width+x]&255)<155)ink++;
            }
            if(total>=gap*3.8f&&ink>=total*.93f) {header=true;break;}
        }
        if(!header)return false;
        int[] stem=attachedRawStem(gray,width,height,head,gap);
        if(stem!=null&&(stem[1]<staff.top-gap*.25f||stem[1]>staff.bottom+gap*.25f))return false;
        int left=Math.max(0,Math.round(head.minX-gap*.3f));
        int right=Math.min(width-1,Math.round(head.maxX+gap*.3f));
        int top=Math.max(0,Math.round(staff.top-gap*.2f));
        int bottom=Math.min(height-1,Math.round(staff.bottom+gap*.2f));
        int w=right-left+1,h=bottom-top+1,upper=0,lower=0;
        boolean[] visited=new boolean[w*h];int[] queue=new int[w*h];
        for(int seed=0;seed<w*h;seed++) {
            if(visited[seed]||(gray[(top+seed/w)*width+left+seed%w]&255)<=155)continue;
            int take=0,size=1,minX=w,maxX=-1,minY=h,maxY=-1;boolean edge=false;
            queue[0]=seed;visited[seed]=true;
            while(take<size) {
                int index=queue[take++],x=index%w,y=index/w;
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                edge|=x==0||y==0||x==w-1||y==h-1;
                for(int direction=0;direction<4;direction++) {
                    int nx=x+(direction==0?-1:direction==1?1:0);
                    int ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||nx>=w||ny<0||ny>=h)continue;
                    int next=ny*w+nx;
                    if(!visited[next]&&(gray[(top+ny)*width+left+nx]&255)>155) {
                        visited[next]=true;queue[size++]=next;
                    }
                }
            }
            int cw=maxX-minX+1,ch=maxY-minY+1;
            // Full stacked glyphs need upright counters. A separate denominator
            // can have wider bowls; it also requires printed numerator evidence.
            // Broad shallow hollow-note counters remain excluded.
            if(edge||size<gap*gap*.07f||cw<gap*.2f||cw>gap*1.1f
                    ||ch<gap*.35f||ch>gap*1.1f||ch<cw*(whole?.75f:.6f))continue;
            float cy=top+(minY+maxY)*.5f;
            if(cy<staff.top+gap*2)upper++;else lower++;
        }
        if(whole)return upper>=1&&lower>=2;
        if(lower<2)return false;
        // An 8 denominator may be the only part labelled as a head. Its printed
        // numerator must span the upper half, without an actual note/chord there.
        int ink=0,heads=0,minY=height,maxY=-1;
        for(int y=Math.max(0,Math.round(staff.top));
                y<=Math.min(head.minY-1,Math.min(height-1,Math.round(staff.top+gap*1.85f)));y++)
            for(int x=Math.max(0,Math.round(head.minX-gap*.8f));
                    x<=Math.min(width-1,Math.round(head.maxX+gap*.25f));x++) {
                if(labels[y*width+x]==OmrMeasurePostProcessor.NOTEHEAD)heads++;
                float lineDistance=Math.abs((y-staff.top)/gap-Math.round((y-staff.top)/gap))*gap;
                if(lineDistance<=Math.max(1,gap*.15f))continue;
                if((gray[y*width+x]&255)<155) {ink++;minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
            }
        return heads<gap*gap*.15f&&ink>gap*3&&maxY-minY>gap*1.1f;
    }

    /** A tall, rounded lower meter digit can be painted as a hollow notehead. */
    private static boolean isHeaderMeterDigit(byte[] labels,byte[] gray,int width,int height,
            Component head,List<Staff> staffs,List<Component> glyphs) {
        if(gray==null)return false;
        Staff staff=nearestHeadStaff(staffs,head.centerY);if(staff==null)return false;
        float gap=staff.gap;
        if(head.centerY<staff.top+gap*2.2f||head.centerY>staff.bottom+gap*.2f
                ||head.maxX-head.minX>gap*2.2f||head.maxY-head.minY<gap*1.5f
                ||head.maxY-head.minY>gap*2.6f
                ||attachedRawStem(gray,width,height,head,gap)!=null)return false;
        boolean header=false;
        for(Component glyph:glyphs)if(glyph.maxX<head.minX&&head.minX-glyph.maxX<gap*7
                &&glyph.maxY-glyph.minY>gap*4.5f&&glyph.maxX-glyph.minX>gap*1.1f
                &&glyph.centerY>staff.top-gap&&glyph.centerY<staff.bottom+gap)header=true;
        if(!header)return false;
        int left=Math.max(0,Math.round(head.minX-gap*.55f)),right=Math.min(width-1,Math.round(head.maxX+gap*.25f));
        int top=Math.max(0,Math.round(staff.top)),bottom=Math.min(height-1,Math.round(staff.top+gap*1.85f));
        int ink=0,heads=0,minY=height,maxY=-1;
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
            if(labels[y*width+x]==OmrMeasurePostProcessor.NOTEHEAD)heads++;
            float lineDistance=Math.abs((y-staff.top)/gap-Math.round((y-staff.top)/gap))*gap;
            if(lineDistance<=Math.max(1,gap*.15f))continue;
            if((gray[y*width+x]&255)<=155){ink++;minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        }
        // Another genuine note above this head is a chord, not a stacked numeral pair.
        return heads<gap*gap*.15f&&ink>gap*3&&maxY-minY>gap*1.1f;
    }

    /** Adjacent chord pitches can occupy opposite sides of one shared stem even
     * when segmentation returns two separate components. Their horizontal offset
     * is engraving, not an extra attack between the surrounding eighth notes. */
    private static List<DetectedNote> alignDisplacedSeconds(List<DetectedNote> source,byte[] gray,int width,int height) {
        if(gray==null)return source;
        List<DetectedNote> result=new ArrayList<>(source);
        for(int i=0;i<result.size();i++)for(int j=i+1;j<result.size();j++) {
            DetectedNote a=result.get(i),b=result.get(j);
            if(a.event.measureIndex()!=b.event.measureIndex()||a.event.staffIndex()!=b.event.staffIndex()
                    ||a.event.staffCount()!=b.event.staffCount()||Math.abs(a.event.staffStep()-b.event.staffStep())!=1)continue;
            float gap=(a.staffGap+b.staffGap)*.5f,dx=Math.abs(a.head.centerX-b.head.centerX);
            if(dx<gap*.9f||dx>gap*1.8f||Math.abs(a.head.centerY-b.head.centerY)>gap*.75f)continue;
            int left=Math.max(a.head.minX,b.head.minX)-1,right=Math.min(a.head.maxX,b.head.maxX)+1;
            if(left>right)continue;
            boolean shared=false;
            for(int x=Math.max(0,left);x<=Math.min(width-1,right);x++)for(int direction:new int[]{-1,1}) {
                int edge=direction<0?Math.min(a.head.minY,b.head.minY):Math.max(a.head.maxY,b.head.maxY);
                int count=0,samples=0;
                for(int k=1;k<=Math.round(gap*1.8f);k++) {
                    int y=edge+direction*k;if(y<0||y>=height)break;
                    samples++;if((gray[y*width+x]&255)<165)count++;
                }
                if(samples>=gap*1.6f&&count>=samples*.92f)shared=true;
            }
            if(!shared)continue;
            float position=Math.min(a.event.positionInMeasure(),b.event.positionInMeasure());
            // A displaced second may share its original column with further chord tones.
            // Move that whole attack together, preserving every tone's independent duration.
            List<Integer> chord=new ArrayList<>();
            for(int index=0;index<result.size();index++) {
                DetectedNote n=result.get(index);ScoreNoteEvent e=n.event;
                if(e.measureIndex()!=a.event.measureIndex()||e.staffIndex()!=a.event.staffIndex()
                        ||e.staffCount()!=a.event.staffCount())continue;
                if(index!=i&&index!=j&&Math.abs(n.head.centerX-a.head.centerX)>gap*.3f
                        &&Math.abs(n.head.centerX-b.head.centerX)>gap*.3f)continue;
                chord.add(index);
                position=Math.min(position,e.positionInMeasure());
            }
            for(int index:chord) {
                DetectedNote n=result.get(index);ScoreNoteEvent e=n.event;
                result.set(index,new DetectedNote(new ScoreNoteEvent(e.measureIndex(),position,e.staffStep(),
                        e.staffIndex(),e.staffCount(),e.pageY(),e.tiedFromPrevious(),e.augmentationDots(),e.beamCount(),
                        e.writtenAccidental(),e.unbeamedDurationBeats(),e.tupletDivisor(),e.followingRestBeats(),
                        e.articulations(),e.clefBottomDiatonic(),e.crossStaffBeam(),e.leadingRestBeats()),n.head,n.staffGap));
            }
        }
        return result;
    }

    /** Each printed stave has its own clef stream, including small in-row changes. */
    private static List<DetectedNote> applyPrintedClefs(List<DetectedNote> notes, List<Staff> staffs,
                                                       List<Component> glyphs, byte[] labels, byte[] gray, int width, int height) {
        List<DetectedNote> result=new ArrayList<>(notes);
        Map<Integer,Integer> inherited=new HashMap<>();
        for(Staff staff:staffs) {
            List<ClefGlyph> clefs=new ArrayList<>();
            float gap=staff.gap;
            for(Component original:glyphs) {
                Component glyph=joinTrebleCurl(joinSmallTrebleFragments(original,glyphs,staff),glyphs,staff);
                float gh=glyph.maxY-glyph.minY+1,gw=glyph.maxX-glyph.minX+1;
                int clef=ScoreNoteEvent.CLEF_UNKNOWN;
                // A treble clef crosses the whole stave and projects beyond both outer lines.
                // This excludes tall accidentals and the thin bracket joining two staves.
                if(gh>=gap*4.8f&&gh<=gap*8.8f&&gw>=gap*1.25f&&gw<=gap*3.4f
                        &&glyph.area>=gap*gap*1.65f&&glyph.minY<staff.top-gap*.35f
                        &&glyph.maxY>staff.bottom+gap*.20f
                        &&Math.abs(glyph.centerY-(staff.top+staff.bottom)*.5f)<gap*1.1f)
                    clef=ScoreNoteEvent.CLEF_TREBLE;
                // Bass clef: curved body plus the two distinct dots straddling the F line.
                if(gh>=gap*2.3f&&gh<=gap*3.7f&&gw>=gap*1.35f&&gw<=gap*2.8f
                        &&Math.abs(glyph.minY-staff.top)<gap*.65f) {
                    boolean above=false,below=false;
                    for(Component dot:glyphs) {
                        float dh=dot.maxY-dot.minY+1,dw=dot.maxX-dot.minX+1;
                        if(dot.centerX<glyph.maxX+gap*.1f||dot.centerX>glyph.maxX+gap*1.1f
                                ||dw<gap*.2f||dw>gap*.7f||dh<gap*.2f||dh>gap*.7f)continue;
                        above|=Math.abs(dot.centerY-(staff.top+gap*.5f))<gap*.3f;
                        below|=Math.abs(dot.centerY-(staff.top+gap*1.5f))<gap*.3f;
                    }
                    if(above&&below)clef=ScoreNoteEvent.CLEF_BASS;
                }
                if(clef==ScoreNoteEvent.CLEF_UNKNOWN&&rawBassClef(original,gray,width,height,staff))
                    clef=ScoreNoteEvent.CLEF_BASS;
                if(clef!=ScoreNoteEvent.CLEF_UNKNOWN)clefs.add(new ClefGlyph(glyph.maxX,clef));
            }
            clefs.sort(Comparator.comparingDouble(ClefGlyph::x));
            int voice=staff.count*16+staff.index;
            int initial=inherited.getOrDefault(voice,ScoreNoteEvent.CLEF_UNKNOWN);
            for(int i=0;i<result.size();i++) {
                DetectedNote n=result.get(i);
                if(staffForHead(labels,gray,width,height,staffs,n.head)!=staff)continue;
                int active=initial;
                for(ClefGlyph clef:clefs)if(clef.x<n.head.centerX)active=clef.clef;
                result.set(i,new DetectedNote(n.event.withClef(active),n.head,n.staffGap));
            }
            if(!clefs.isEmpty())inherited.put(voice,clefs.get(clefs.size()-1).clef);
        }
        return result;
    }
    private record ClefGlyph(float x,int clef) { }

    /** A small bass-clef tail and one dot may be painted as generic symbols.
     * Confirm its two round dots and descending body in the printed pixels. */
    private static boolean rawBassClef(Component body,byte[] gray,int width,int height,Staff staff) {
        if(gray==null)return false;
        float gap=staff.pitchGap,top=staff.pitchBottom-gap*4;
        float gh=body.maxY-body.minY+1,gw=body.maxX-body.minX+1;
        if(gh<gap*1.3f||gh>gap*3.7f||gw<gap*1.3f||gw>gap*2.8f
                ||body.area<gap*gap*.5f||Math.abs(body.minY-top)>gap*.65f)return false;
        int left=Math.max(0,Math.round(body.maxX+gap*.10f));
        int right=Math.min(width-1,Math.round(body.maxX+gap*1.15f));
        int first=Math.max(0,Math.round(top+gap*.1f)),last=Math.min(height-1,Math.round(top+gap*1.95f));
        int w=right-left+1,h=last-first+1;if(w<1||h<1)return false;
        byte[] ink=new byte[w*h];
        for(int y=first;y<=last;y++) {
            float distance=Math.abs((y-top)/gap-Math.round((y-top)/gap))*gap;
            if(distance<gap*.12f)continue;
            for(int x=left;x<=right;x++)if((gray[y*width+x]&255)<165)ink[(y-first)*w+x-left]=1;
        }
        List<Component> dots=new ArrayList<>();
        for(Component dot:findComponents(ink,w,h,(byte)1)) {
            int dw=dot.maxX-dot.minX+1,dh=dot.maxY-dot.minY+1;
            if(dw>=gap*.18f&&dw<=gap*.7f&&dh>=gap*.18f&&dh<=gap*.7f
                    &&dot.area>=dw*dh*.45f)dots.add(dot);
        }
        boolean paired=false;
        for(Component a:dots)for(Component b:dots)if(b.centerY>a.centerY
                &&Math.abs(a.centerX-b.centerX)<gap*.25f
                &&b.centerY-a.centerY>=gap*.55f&&b.centerY-a.centerY<=gap*1.4f
                &&Math.abs((a.centerY+b.centerY)*.5f+first-(top+gap))<gap*.3f)paired=true;
        if(!paired)return false;
        // A two-dot punctuation mark beside another glyph is insufficient: the
        // clef must also have a printed tail reaching below its lower dot.
        for(int y=Math.max(0,Math.round(top+gap*2.35f));y<=Math.min(height-1,Math.round(top+gap*3.4f));y++) {
            float distance=Math.abs((y-top)/gap-Math.round((y-top)/gap))*gap;
            if(distance<gap*.15f)continue;
            int count=0;
            for(int x=Math.max(0,body.minX);x<=Math.min(width-1,body.maxX);x++)
                if((gray[y*width+x]&255)<165)count++;
            if(count>=gap*.25f&&count<gw*.8f)return true;
        }
        return false;
    }

    private static Component joinSmallTrebleFragments(Component body,List<Component> glyphs,Staff staff) {
        float gap=staff.gap;
        if(body.maxY-body.minY<gap*3||body.maxY-body.minY>gap*4.8f
                ||body.maxX-body.minX<gap*1.3f||body.maxX-body.minX>gap*2.5f
                ||body.minY>staff.top+gap*.3f||body.minY<staff.top-gap
                ||body.maxY<staff.bottom-gap||body.maxY>staff.bottom+gap*.2f)return body;
        Component joined=body;
        for(Component part:glyphs)if(part!=body&&part.minX>=body.minX-gap*.4f&&part.maxX<=body.maxX+gap*.4f
                &&part.minY>=staff.top-gap&&part.maxY<=staff.bottom+gap*1.4f) {
            int area=joined.area+part.area;
            joined=new Component(area,Math.min(joined.minX,part.minX),Math.max(joined.maxX,part.maxX),
                    Math.min(joined.minY,part.minY),Math.max(joined.maxY,part.maxY),
                    (joined.centerX*joined.area+part.centerX*part.area)/area,
                    (joined.centerY*joined.area+part.centerY*part.area)/area);
        }
        return joined;
    }

    /** Staff-line predictions can sever the bottom curl from an otherwise complete treble clef. */
    private static Component joinTrebleCurl(Component body,List<Component> glyphs,Staff staff) {
        float gap=staff.gap;
        float gh=body.maxY-body.minY+1,gw=body.maxX-body.minX+1;
        // Require the large, wide, above-staff body first: never construct a clef from accidentals,
        // bass-clef dots, a bracket, or arbitrary small symbols near the bottom of a stave.
        if(gh<gap*4.8f||gh>gap*8.8f||gw<gap*1.25f||gw>gap*3.4f
                ||body.area<gap*gap*1.65f||body.minY>=staff.top-gap*.35f
                ||body.maxY<staff.bottom-gap||body.maxY>staff.bottom+gap*.20f
                ||Math.abs(body.centerY-(staff.top+staff.bottom)*.5f)>=gap*1.1f)return body;
        Component joined=body;
        for(Component tail:glyphs) {
            if(tail==body||tail.minX<body.minX-gap*.2f||tail.maxX>body.maxX+gap*.2f
                    ||tail.minY<staff.bottom-gap||tail.minY>body.maxY+gap*.45f
                    ||tail.maxY<=staff.bottom+gap*.2f||tail.maxY>staff.bottom+gap*2.2f
                    ||tail.area<gap*gap*.15f||tail.area>body.area*.75f)continue;
            int area=joined.area+tail.area;
            joined=new Component(area,Math.min(joined.minX,tail.minX),Math.max(joined.maxX,tail.maxX),
                    Math.min(joined.minY,tail.minY),Math.max(joined.maxY,tail.maxY),
                    (joined.centerX*joined.area+tail.centerX*tail.area)/area,
                    (joined.centerY*joined.area+tail.centerY*tail.area)/area);
        }
        return joined;
    }

    /** Count separated flag attachments close to the stem, only beside a proven short rest. */
    private static int rawDetachedFlags(byte[] gray, byte[] labels, int width, int height,
                                         Component head, float gap) {
        if (gray == null) return 0;
        int stemX = head.maxX, best = 0, direction = -1;
        int top = Math.max(0, Math.round(head.centerY - gap * 4.8f));
        int bottom = Math.max(0, Math.round(head.centerY - gap * .65f));
        int belowTop = Math.min(height-1, Math.round(head.centerY + gap * .65f));
        int belowBottom = Math.min(height-1, Math.round(head.centerY + gap * 4.8f));
        for (int x = Math.max(0, head.minX - 3); x <= Math.min(width - 1, head.maxX + 4); x++) {
            int count = countVertical(labels, width, x, top, bottom);
            if (count > best) { best = count; stemX = x; direction = -1; }
            count = countVertical(labels, width, x, belowTop, belowBottom);
            if (count > best) { best = count; stemX = x; direction = 1; }
        }
        if (best < gap) return 0;
        int limit = direction < 0 ? top : belowBottom;
        bottom = direction < 0 ? bottom : belowTop;
        int end = findStemEnd(labels, width, stemX, direction < 0,
                direction < 0 ? top : belowTop, direction < 0 ? bottom : belowBottom);
        // A staff crossing can hide the first flag's stem segment in the semantic mask.
        // Follow the attached raw stem outward from the head, allowing only tiny ink gaps.
        int blank = 0, rawEnd = Math.round(head.centerY);
        for (int y = Math.round(head.centerY + direction * gap * .3f);
                direction < 0 ? y >= limit : y <= limit; y += direction) {
            boolean dark = false;
            for (int x = Math.max(0,stemX-1); x <= Math.min(width-1,stemX+1); x++)
                if ((gray[y*width+x]&255)<170) dark = true;
            if (dark) { rawEnd=y; blank=0; }
            else if (++blank > Math.max(2,Math.round(gap*.2f))) break;
        }
        if (Math.abs(rawEnd-head.centerY)>gap*1.5f && Math.abs(rawEnd-end)<gap*1.8f) end=rawEnd;
        int left = Math.min(width - 1, Math.round(stemX + gap * .3f));
        int right = Math.min(width - 1, Math.round(stemX + gap * .55f));
        int groups = 0, run = 0, first = -1, last = -1;
        int span = Math.abs(bottom-end);
        for (int offset = 0; offset <= span + 1; offset++) {
            int y = end - direction * offset;
            int rowLeft = Math.max(0, stemX - Math.round(gap * 2));
            int rowRight = Math.min(width - 1, stemX + Math.round(gap * 2));
            int rowInk = 0;
            if (offset <= span) for (int x = rowLeft; x <= rowRight; x++)
                if ((gray[y * width + x] & 255) < 170) rowInk++;
            if (rowInk > (rowRight - rowLeft + 1) * .85f) continue;
            int leftInk = 0, rightInk = 0;
            if (offset <= span) {
                for (int x = Math.max(0, stemX - Math.round(gap)); x < stemX - gap * .2f; x++)
                    if ((gray[y * width + x] & 255) < 170) leftInk++;
                for (int x = stemX + 1; x <= Math.min(width - 1, stemX + Math.round(gap)); x++)
                    if ((gray[y * width + x] & 255) < 170) rightInk++;
            }
            if (leftInk > gap * .55f && rightInk > gap * .45f) continue;
            int ink = 0;
            if (offset <= span) for (int x = left; x <= right; x++)
                if ((gray[y * width + x] & 255) < 170) ink++;
            if (ink >= 2) run++;
            else {
                if (run >= Math.max(2, gap * .12f)) {
                    groups++; if (first < 0) first = offset; last = offset;
                }
                run = 0;
            }
        }
        return groups == 2 && last - first >= gap * .75f && last - first <= gap * 2f ? 2
                : groups == 1 && first <= gap * 1.8f ? 1 : 0;
    }

    /**
     * Finds a compact run of signature accidentals immediately after a measure boundary and
     * before that stave's first note. Requiring at least two same-kind glyphs deliberately avoids
     * treating an ordinary local accidental as a modulation; one-flat/one-sharp changes remain
     * eligible only when the raw score also shows a double bar at the boundary.
     */
    private static List<ScoreKeyChange> detectKeyChanges(byte[] labels, byte[] gray, int width, int height,
                                                          List<MeasureRegion> measures,
                                                          List<Staff> staffs,
                                                          List<AccidentalCandidate> candidates,
                                                          List<Component> heads) {
        List<ScoreKeyChange> result = new ArrayList<>();
        candidates = joinSignatureFragments(labels, width, candidates, staffs);
        for (int measureIndex = 0; measureIndex < measures.size(); measureIndex++) {
            MeasureRegion measure = measures.get(measureIndex);
            Map<Integer, Integer> votes = new HashMap<>();
            for (Staff staff : staffs) {
                float staffCenter = (staff.top + staff.bottom) * .5f / height;
                float tolerance = staff.gap * .75f / height;
                if (staffCenter < measure.top() - tolerance
                        || staffCenter > measure.bottom() + tolerance) continue;
                float left = measure.left() * width;
                float boundary = left;
                // Measure rectangles start after clefs/signatures. Include a verified system
                // header so signatures repeated after a page/row break can restore the key.
                boolean firstInRow = true;
                for (MeasureRegion other : measures)
                    if (other.left() < measure.left() && staffCenter >= other.top() - tolerance
                            && staffCenter <= other.bottom() + tolerance) firstInRow = false;
                Component clef = null;
                if (firstInRow) for (AccidentalCandidate candidate : candidates) {
                    Component c = candidate.component;
                    if (candidate.label == OmrMeasurePostProcessor.CLEF_OR_KEY
                            && c.maxX < boundary && c.maxX > boundary - staff.gap * 10
                            && c.maxY - c.minY > staff.gap * 4.5f
                            && c.maxX - c.minX > staff.gap * 1.1f
                            && c.centerY > staff.top - staff.gap && c.centerY < staff.bottom + staff.gap
                            && (clef == null || c.maxX > clef.maxX)) clef = c;
                }
                if (clef != null) left = clef.maxX + staff.gap * .2f;
                float right = Math.min(measure.right() * width, left + staff.gap * 10.5f);
                float firstHead = Float.MAX_VALUE;
                for (Component head : heads) {
                    if (head.centerX < left - staff.gap * .2f || head.centerX > right) continue;
                    Staff owner = nearestHeadStaff(staffs, head.centerY);
                    if (owner == staff) firstHead = Math.min(firstHead, head.minX);
                }
                if (Float.isFinite(firstHead)) right = Math.min(right, firstHead - staff.gap * .28f);
                if (right <= left) continue;

                List<SignatureGlyph> glyphs = new ArrayList<>();
                for (AccidentalCandidate candidate : candidates) {
                    Component glyph = candidate.component;
                    if (glyph.centerX < left - staff.gap * .12f || glyph.centerX > right
                            || glyph.centerY < staff.top - staff.gap * 2.25f
                            || glyph.centerY > staff.bottom + staff.gap * 2.25f) continue;
                    // A fragmented semantic double bar may resemble a flat bowl.
                    // Its source column still crosses the complete staff.
                    if (glyph.maxX - glyph.minX + 1 <= staff.gap * .65f
                            && fullStaffRule(gray, width, height, Math.round(glyph.centerX), staff)) continue;
                    int accidental = isNaturalGlyph(labels, width, height, candidate, staff.gap)
                            ? ScoreNoteEvent.ACCIDENTAL_NATURAL
                            : isSharpGlyph(labels, width, height, candidate, staff.gap)
                            ? ScoreNoteEvent.ACCIDENTAL_SHARP
                            : isFlatGlyph(labels, width, height, candidate, staff.gap)
                            ? ScoreNoteEvent.ACCIDENTAL_FLAT
                            : ScoreNoteEvent.ACCIDENTAL_FROM_KEY;
                    if (accidental != ScoreNoteEvent.ACCIDENTAL_FROM_KEY)
                        glyphs.add(new SignatureGlyph(glyph.centerX, accidental));
                }
                glyphs.sort(Comparator.comparingDouble(SignatureGlyph::x));
                List<SignatureGlyph> run = densestSignatureRun(glyphs, staff.gap);
                if (run.isEmpty()) continue;
                int flats = 0, sharps = 0, naturals = 0;
                for (SignatureGlyph glyph : run) {
                    if (glyph.accidental == ScoreNoteEvent.ACCIDENTAL_FLAT) flats++;
                    else if (glyph.accidental == ScoreNoteEvent.ACCIDENTAL_SHARP) sharps++;
                    else if (glyph.accidental == ScoreNoteEvent.ACCIDENTAL_NATURAL) naturals++;
                }
                boolean doubleBar = hasDoubleBar(labels, gray, width, height, boundary, staff);
                boolean signatureHeader = clef != null && run.get(0).x < clef.maxX + staff.gap * 2.2f
                        && firstHead - run.get(run.size() - 1).x >= staff.gap * 1.35f;
                // Staff lines can cut a flat's bowl away from its spine, producing two semantic
                // components that are individually too incomplete for the local-accidental
                // classifier. Once at least one flat establishes the run's glyph family, count
                // the repeated tall left spines in the same pre-note slot.
                if (flats > 0 && sharps == 0 && naturals == 0) {
                    int spines = countFlatSpines(labels, gray, width, height, left, right, staff, doubleBar);
                    // One damaged sharp can satisfy the flat-bowl test; its two spines are
                    // not two separate flats. Do not change the inherited key on this evidence.
                    if (signatureHeader && run.size() == 1 && spines > flats) continue;
                    flats = Math.max(flats, spines);
                }
                int strongest = Math.max(naturals, Math.max(flats, sharps));
                if (strongest > 7 || (flats > 0 ? sharps + naturals > 0
                        : sharps > 0 ? naturals > 0 : false)) continue;
                if (strongest < 2 && !doubleBar && !signatureHeader) continue;
                // A single glyph pressed against the first head is a local accidental even
                // when its own spines confused the nearby double-bar test.
                if(strongest==1&&!signatureHeader&&firstHead-run.get(run.size()-1).x<staff.gap*1.35f)continue;
                int fifths = naturals > 0 ? 0 : flats > 0 ? -flats : sharps;
                votes.put(fifths, votes.getOrDefault(fifths, 0) + 1);
            }
            int bestFifths = 0, bestVotes = 0;
            for (Map.Entry<Integer, Integer> vote : votes.entrySet())
                if (vote.getValue() > bestVotes) {
                    bestFifths = vote.getKey(); bestVotes = vote.getValue();
                }
            // A damaged repeated header on one stave must not outvote the intact matching
            // header on another. Preserve the established key when support is tied.
            if(!result.isEmpty()) {
                int inherited=result.get(result.size()-1).fifths();
                if(votes.getOrDefault(inherited,0)==bestVotes&&bestVotes>0)bestFifths=inherited;
            }
            if (bestVotes > 0 && (result.isEmpty() || result.get(result.size() - 1).fifths() != bestFifths))
                result.add(new ScoreKeyChange(measureIndex, bestFifths));
        }
        return List.copyOf(result);
    }

    /** Reconnect a signature glyph cut horizontally by a staff-labelled scan line. */
    private static List<AccidentalCandidate> joinSignatureFragments(byte[] labels, int width,
            List<AccidentalCandidate> candidates, List<Staff> staffs) {
        List<AccidentalCandidate> joined = new ArrayList<>(candidates);
        for (int i = 0; i < joined.size(); i++) {
            AccidentalCandidate a = joined.get(i);
            if (a.label != OmrMeasurePostProcessor.CLEF_OR_KEY) continue;
            Staff staff = nearestHeadStaff(staffs, a.component.centerY);
            if (staff == null) continue;
            float gap = staff.gap;
            for (int j = i + 1; j < joined.size(); j++) {
                AccidentalCandidate b = joined.get(j);
                if (b.label != a.label) continue;
                Component upper = a.component.minY < b.component.minY ? a.component : b.component;
                Component lower = upper == a.component ? b.component : a.component;
                int separation = lower.minY - upper.maxY - 1;
                int left = Math.max(upper.minX, lower.minX), right = Math.min(upper.maxX, lower.maxX);
                if (separation < 0 || separation > gap * .35f
                        // A staff line can separate a flat's thin upper stem from its wider
                        // lower bowl. Require overlap of the narrower fragment; requiring
                        // the bowl's width leaves a truncated glyph that can resemble a sharp.
                        || right - left + 1 < Math.min(upper.maxX-upper.minX+1, lower.maxX-lower.minX+1) * .70f
                        || lower.maxY-upper.minY+1 > gap*3.65f
                        || Math.max(upper.maxX,lower.maxX)-Math.min(upper.minX,lower.minX)+1 > gap*1.65f) continue;
                boolean staffCut = false;
                for (int y=upper.maxY;y<=lower.minY;y++)
                    if (rowLabelCount(labels,width,y,left,right,OmrMeasurePostProcessor.STAFF) >= (right-left+1)*.7f) staffCut=true;
                if (!staffCut) continue;
                int area=upper.area+lower.area;
                Component merged=new Component(area,Math.min(upper.minX,lower.minX),Math.max(upper.maxX,lower.maxX),
                        upper.minY,lower.maxY,(upper.centerX*upper.area+lower.centerX*lower.area)/area,
                        (upper.centerY*upper.area+lower.centerY*lower.area)/area);
                a=new AccidentalCandidate(merged,a.label); joined.set(i,a);joined.remove(j--);
            }
        }
        return joined;
    }

    private static int countFlatSpines(byte[] labels, byte[] gray, int width, int height,
                                       float boundary, float right, Staff staff,
                                       boolean doubleBar) {
        int leftX=Math.max(0,Math.round(boundary+(doubleBar?staff.gap*.42f:0f)));
        int rightX=Math.min(width-1,Math.round(right));
        int top=Math.max(0,Math.round(staff.top-staff.gap*2.25f));
        int bottom=Math.min(height-1,Math.round(staff.bottom+staff.gap*2.25f));
        int threshold=Math.max(3,Math.round(staff.gap*1.55f));
        List<float[]> spines=new ArrayList<>();
        float[] active=null;
        for(int x=leftX;x<=rightX;x++) {
            int pixels=0,run=0,longest=0,blanks=0,start=top,bestTop=top,bestBottom=top;
            for(int y=top;y<=bottom;y++) {
                byte label=labels[y*width+x];
                boolean symbol=label==OmrMeasurePostProcessor.CLEF_OR_KEY||label==OmrMeasurePostProcessor.SYMBOL;
                if(symbol)pixels++;
                boolean ink=gray==null?symbol:(gray[y*width+x]&255)<=205;
                if(ink) {
                    if(run==0)start=y;
                    run++;blanks=0;
                    if(run>longest){longest=run;bestTop=start;bestBottom=y;}
                } else if(++blanks>1)run=0;
            }
            boolean strong=pixels>=threshold&&longest>=threshold
                    &&!fullStaffRule(gray,width,height,x,staff);
            if(strong) {
                if(active==null)active=new float[]{x,(bestTop+bestBottom)*.5f,longest};
                else if(longest>active[2]){active[0]=x;active[1]=(bestTop+bestBottom)*.5f;active[2]=longest;}
            } else if(active!=null){spines.add(active);active=null;}
        }
        if(active!=null)spines.add(active);
        if(spines.isEmpty())return 0;
        int groups=1;float[] previous=spines.get(0);
        for(int i=1;i<spines.size();i++) {
            float[] next=spines.get(i);float dx=next[0]-previous[0];
            // A bowl edge is not another accidental. Keys have separate, closely
            // spaced spines, alternating a fourth up or a fifth down on the page.
            if(dx<staff.gap*.5f)continue;
            if(dx>staff.gap*1.85f)break;
            float dy=next[1]-previous[1];
            if(Math.abs(dy+staff.gap*1.5f)>staff.gap*.55f
                    &&Math.abs(dy-staff.gap*2f)>staff.gap*.55f)continue;
            groups++;previous=next;
        }
        return groups;
    }

    private static boolean fullStaffRule(byte[] gray, int width, int height, int x, Staff staff) {
        if (gray == null) return false;
        int covered = 0, sampled = 0;
        int margin = Math.max(1, Math.round(staff.gap * .14f));
        for (int line = 0; line < 4; line++) {
            int first = Math.max(0, Math.round(staff.top + line * staff.gap) + margin + 1);
            int last = Math.min(height - 1, Math.round(staff.top + (line + 1) * staff.gap) - margin - 1);
            int space = 0;
            for (int y = first; y <= last; y++) if ((gray[y * width + x] & 255) <= 205) space++;
            int count = Math.max(0, last - first + 1);
            if (space < count * .55f) return false;
            covered += space;
            sampled += count;
        }
        return sampled > 0 && covered >= sampled * .90f;
    }

    private static List<SignatureGlyph> densestSignatureRun(List<SignatureGlyph> glyphs, float gap) {
        List<SignatureGlyph> best = List.of();
        for (int start = 0; start < glyphs.size(); start++) {
            List<SignatureGlyph> current = new ArrayList<>();
            current.add(glyphs.get(start));
            for (int index = start + 1; index < glyphs.size(); index++) {
                SignatureGlyph previous = current.get(current.size() - 1);
                SignatureGlyph next = glyphs.get(index);
                if (next.x - previous.x > gap * 1.85f
                        || next.x - current.get(0).x > gap * 7.8f) break;
                current.add(next);
            }
            if (current.size() > best.size()) best = current;
        }
        return best;
    }

    private static boolean hasDoubleBar(byte[] labels, byte[] gray, int width, int height,
                                        float boundaryX, Staff staff) {
        int left = Math.max(0, Math.round(boundaryX - staff.gap * 1.1f));
        int right = Math.min(width - 1, Math.round(boundaryX + staff.gap * 1.1f));
        int top = Math.max(0, Math.round(staff.top - staff.gap * .3f));
        int bottom = Math.min(height - 1, Math.round(staff.bottom + staff.gap * .3f));
        int groups = 0;
        boolean previous = false;
        for (int x = left; x <= right; x++) {
            int ink = 0;
            for (int y = top; y <= bottom; y++)
                if (labels[y * width + x] == OmrMeasurePostProcessor.STEM_OR_REST
                        || gray != null && (gray[y * width + x] & 0xff) < 180) ink++;
            boolean strong = ink >= (bottom - top + 1) * .72f;
            if (strong && !previous) groups++;
            previous = strong;
        }
        return groups >= 2;
    }

    private static void logHeadCoverage(List<Staff> staffs, List<Component> rawComponents,
                                         List<Component> splitComponents,
                                         List<Component> accepted, List<Component> demoted,
                                         List<DetectedNote> emitted) {
        int[] rawByStaff = new int[staffs.size()];
        int[] splitByStaff = new int[staffs.size()];
        int[] acceptedByStaff = new int[staffs.size()];
        int[] demotedByStaff = new int[staffs.size()];
        int[] emittedByStaff = new int[staffs.size()];
        int[] tallByStaff = new int[staffs.size()];
        float[] maxHeightByStaff = new float[staffs.size()];
        for (Component component : rawComponents) incrementNearest(staffs, rawByStaff, component.centerY);
        for (Component component : splitComponents) incrementNearest(staffs, splitByStaff, component.centerY);
        for (Component component : rawComponents) {
            Staff nearest = nearestStaff(staffs, component.centerY);
            int index = nearest == null ? -1 : staffs.indexOf(nearest);
            if (index < 0) continue;
            float ratio = (component.maxY - component.minY + 1f) / nearest.gap;
            if (ratio >= .90f) tallByStaff[index]++;
            maxHeightByStaff[index] = Math.max(maxHeightByStaff[index], ratio);
        }
        for (Component component : accepted) incrementNearest(staffs, acceptedByStaff, component.centerY);
        for (Component component : demoted) incrementNearest(staffs, demotedByStaff, component.centerY);
        for (DetectedNote note : emitted) incrementNearest(staffs, emittedByStaff, note.head.centerY);
        try {
            Diagnostics.log("Head coverage raw="
                    + java.util.Arrays.toString(rawByStaff) + " split="
                    + java.util.Arrays.toString(splitByStaff) + " accepted="
                    + java.util.Arrays.toString(acceptedByStaff) + " demotedDots="
                    + java.util.Arrays.toString(demotedByStaff) + " emitted="
                    + java.util.Arrays.toString(emittedByStaff) + " tall="
                    + java.util.Arrays.toString(tallByStaff) + " maxHeight="
                    + java.util.Arrays.toString(maxHeightByStaff));
        } catch (RuntimeException ignored) {
            // Diagnostics are optional and must not fail recognition.
        }
    }

    private static void incrementNearest(List<Staff> staffs, int[] counts, float centerY) {
        Staff nearest = nearestStaff(staffs, centerY);
        int index = nearest == null ? -1 : staffs.indexOf(nearest);
        if (index >= 0) counts[index]++;
    }

    /** the model occasionally joins the two vertically aligned ovals of a printed dyad into one
     * NOTEHEAD component. Split only an abnormally tall component with two strong ink lobes and
     * a real valley between them; ordinary single heads, dots, and solid artifacts stay intact. */
    private static List<Component> splitStackedHeads(byte[] labels, byte[] gray, int width, int height,
                                                      List<Component> source,
                                                      List<Staff> staffs) {
        List<Component> result = new ArrayList<>();
        for (Component component : source) {
            Staff staff = nearestHeadStaff(staffs, component.centerY);
            float componentHeight = component.maxY - component.minY + 1f;
            List<Component> filled = splitTouchingFilledVoices(labels, gray, width, height,
                    component, staff);
            if (!filled.isEmpty()) { result.addAll(filled); continue; }
            List<Component> mixed=splitMixedUnisonStack(labels,gray,width,height,component,staff);
            if(!mixed.isEmpty()){result.addAll(mixed);continue;}
            List<Component> regular = splitRegularStack(labels, width, component, staff);
            if (!regular.isEmpty()) {
                result.addAll(regular);
                continue;
            }
            List<Component> hollow = splitHollowStack(labels, gray, width, height, component, staff);
            if (!hollow.isEmpty()) {
                result.addAll(hollow);
                continue;
            }
            if (staff == null || componentHeight < staff.gap * 1.45f
                    || componentHeight > staff.gap * 3.2f) {
                result.add(component);
                continue;
            }
            int inset = Math.max(2, Math.round(staff.gap * .42f));
            int firstSplit = component.minY + inset;
            int lastSplit = component.maxY - inset;
            int split = -1, valley = Integer.MAX_VALUE;
            int upperPeak = 0, lowerPeak = 0;
            int[] rowInk = new int[component.maxY - component.minY + 1];
            for (int y = component.minY; y <= component.maxY; y++) {
                int count = 0;
                for (int x = component.minX; x <= component.maxX; x++)
                    if (labels[y * width + x] == OmrMeasurePostProcessor.NOTEHEAD) count++;
                rowInk[y - component.minY] = count;
            }
            for (int y = firstSplit; y <= lastSplit; y++) {
                int count = rowInk[y - component.minY];
                if (count < valley) { valley = count; split = y; }
            }
            if (split >= 0) {
                for (int y = component.minY; y < split; y++)
                    upperPeak = Math.max(upperPeak, rowInk[y - component.minY]);
                for (int y = split + 1; y <= component.maxY; y++)
                    lowerPeak = Math.max(lowerPeak, rowInk[y - component.minY]);
            }
            Component upper = split < 0 ? null : componentSlice(labels, width, component,
                    component.minY, split);
            Component lower = split < 0 ? null : componentSlice(labels, width, component,
                    split + 1, component.maxY);
            boolean twoLobes = upper != null && lower != null
                    && upperPeak >= Math.max(2, Math.round(staff.gap * .24f))
                    && lowerPeak >= Math.max(2, Math.round(staff.gap * .24f))
                    && valley <= Math.min(upperPeak, lowerPeak) * .82f
                    && plausibleHead(upper, staff.gap) && plausibleHead(lower, staff.gap)
                    && lower.centerY - upper.centerY >= staff.gap * .58f
                    && lower.centerY - upper.centerY <= staff.gap * 2.25f
                    && Math.abs(lower.centerX - upper.centerX) <= staff.gap * 1.45f;
            if(!twoLobes&&gray!=null&&componentHeight>=staff.gap*1.7f
                    &&componentHeight<=staff.gap*2.6f&&component.maxX-component.minX+1<=staff.gap*1.7f) {
                // A semantic bridge can fill the neck between two solid heads.
                // The printed silhouette must independently contain two lobes.
                int[] rawRows=new int[rowInk.length];
                for(int y=component.minY;y<=component.maxY;y++)
                    for(int x=component.minX;x<=component.maxX;x++)
                        if((gray[y*width+x]&255)<=165)rawRows[y-component.minY]++;
                int neck=-1,minimum=Integer.MAX_VALUE;
                for(int y=firstSplit;y<=lastSplit;y++)if(rawRows[y-component.minY]<minimum) {
                    minimum=rawRows[y-component.minY];neck=y;
                }
                int peakAbove=0,peakBelow=0;
                for(int y=component.minY;y<neck;y++)peakAbove=Math.max(peakAbove,rawRows[y-component.minY]);
                for(int y=neck+1;y<=component.maxY;y++)peakBelow=Math.max(peakBelow,rawRows[y-component.minY]);
                Component a=neck<0?null:componentSlice(labels,width,component,component.minY,neck);
                Component b=neck<0?null:componentSlice(labels,width,component,neck+1,component.maxY);
                if(a!=null&&b!=null&&minimum<=Math.min(peakAbove,peakBelow)*.78f
                        &&peakAbove>=staff.gap*.65f&&peakBelow>=staff.gap*.65f
                        &&plausibleHead(a,staff.gap)&&plausibleHead(b,staff.gap)
                        &&!hasOpenCenter(labels,gray,width,height,a,staff.gap)
                        &&!hasOpenCenter(labels,gray,width,height,b,staff.gap)
                        &&b.centerY-a.centerY>=staff.gap*.58f&&b.centerY-a.centerY<=staff.gap*2.25f
                        &&Math.abs(a.centerX-b.centerX)<=staff.gap*1.45f) {
                    upper=a;lower=b;twoLobes=true;
                }
            }
            if (!twoLobes && gray != null && componentHeight>=staff.gap*1.7f
                    && componentHeight<=staff.gap*2.6f
                    && component.maxX-component.minX+1<=staff.gap*1.8f) {
                // The model can fill both hollow ovals and their connecting ledger line into
                // one solid mask. Two separate white centres in the original engraving are
                // stronger evidence than a missing valley in that semantic mask.
                int middle=(component.minY+component.maxY)/2;
                Component a=componentSlice(labels,width,component,component.minY,middle);
                Component b=componentSlice(labels,width,component,middle+1,component.maxY);
                if(a!=null&&b!=null&&plausibleHead(a,staff.gap)&&plausibleHead(b,staff.gap)
                        &&b.centerY-a.centerY>=staff.gap*.75f
                        &&hasOpenCenter(labels,gray,width,height,a,staff.gap)
                        &&hasOpenCenter(labels,gray,width,height,b,staff.gap)) {
                    upper=a;lower=b;twoLobes=true;
                }
            }
            if (twoLobes) {
                result.add(upper);
                result.add(lower);
            } else result.add(component);
        }
        return List.copyOf(result);
    }

    /** A narrow raw-ink neck and opposing stems identify two touching filled voices.
     * Split before event decoding so each head retains its own position and duration. */
    private static List<Component> splitTouchingFilledVoices(byte[] labels, byte[] gray,
            int width, int height, Component head, Staff staff) {
        if (gray == null || staff == null) return List.of();
        float gap = staff.pitchGap;
        int w = head.maxX - head.minX + 1, h = head.maxY - head.minY + 1;
        if (w < gap * 1.8f || w > gap * 3.1f || h < gap * .65f || h > gap * 1.4f)
            return List.of();
        int middle = (head.minX + head.maxX) / 2;
        Component left = horizontalHeadSlice(labels, width, head, head.minX, middle);
        Component right = horizontalHeadSlice(labels, width, head, middle + 1, head.maxX);
        if (left == null || right == null || !plausibleHead(left, gap) || !plausibleHead(right, gap)
                || Math.abs(left.centerY - right.centerY) > gap * .3f
                || left.area < gap * gap * .35f || right.area < gap * gap * .35f
                || hasOpenCenter(labels, gray, width, height, left, gap)
                || hasOpenCenter(labels, gray, width, height, right, gap)) return List.of();
        int leftUp = attachedStemReach(labels, width, height, left, gap, true);
        int leftDown = attachedStemReach(labels, width, height, left, gap, false);
        int rightUp = attachedStemReach(labels, width, height, right, gap, true);
        int rightDown = attachedStemReach(labels, width, height, right, gap, false);
        int minimum = Math.max(3, Math.round(gap * .72f));
        boolean opposing = (leftDown >= minimum && rightUp >= minimum
                && leftUp < minimum && rightDown < minimum)
                || (leftUp >= minimum && rightDown >= minimum
                && leftDown < minimum && rightUp < minimum);
        if (!opposing) return List.of();
        int leftPeak = 0, rightPeak = 0, neck = h;
        for (int x = head.minX; x <= head.maxX; x++) {
            int ink = 0;
            for (int y = head.minY; y <= head.maxY; y++)
                if ((gray[y * width + x] & 255) < 165) ink++;
            if (x < middle) leftPeak = Math.max(leftPeak, ink);
            if (x > middle) rightPeak = Math.max(rightPeak, ink);
            if (Math.abs(x - middle) <= Math.max(1, Math.round(gap * .15f)))
                neck = Math.min(neck, ink);
        }
        if (Math.min(leftPeak, rightPeak) < gap * .65f
                || neck > Math.min(leftPeak, rightPeak) * .6f) return List.of();
        return List.of(left, right);
    }

    /** A filled voice can touch two hollow chord heads on the opposite side
     * of the stem. Require the two printed open centres before splitting this
     * unusually wide component; an ordinary filled chord is not sufficient. */
    private static List<Component> splitMixedUnisonStack(byte[] labels,byte[] gray,int width,int height,
                                                         Component source,Staff staff) {
        if(gray==null||staff==null)return List.of();
        float gap=staff.gap;int w=source.maxX-source.minX+1,h=source.maxY-source.minY+1;
        if(w<gap*2.6f||w>gap*3.6f||h<gap*1.8f||h>gap*2.7f)return List.of();
        int middle=source.minX+Math.round(gap*1.45f);
        Component left=horizontalHeadSlice(labels,width,source,source.minX,middle);
        Component right=horizontalHeadSlice(labels,width,source,middle+1,source.maxX);
        if(left==null||right==null||!plausibleHead(left,gap)||left.maxY-left.minY>gap*1.4f
                ||hasOpenCenter(labels,gray,width,height,left,gap)
                ||!hasAttachedStem(labels,width,height,left,gap))return List.of();
        List<Component> parts=splitRegularStack(labels,width,right,staff);
        if(parts.size()!=2)return List.of();
        for(Component part:parts)if(!hasOpenCenter(labels,gray,width,height,part,gap))return List.of();
        if(parts.stream().noneMatch(part->Math.abs(part.centerY-left.centerY)<gap*.3f))return List.of();
        return List.of(left,parts.get(0),parts.get(1));
    }

    /** Follow every broad oval lobe, rather than choosing the single darkest neck.
     * A filled-in ledger bridge can move that neck inside a hollow head, and a
     * triad has two necks. Staff-spaced broad lobes establish the actual centres. */
    private static List<Component> splitRegularStack(byte[] labels, int width,
                                                      Component component, Staff staff) {
        if (staff == null) return List.of();
        float gap = staff.gap;
        int height = component.maxY - component.minY + 1;
        if (height < gap * 1.7f || height > gap * 4.4f
                || component.maxX - component.minX + 1 > gap * 1.8f) return List.of();
        int[] rows = new int[height];
        int peak = 0;
        for (int y = 0; y < height; y++) {
            for (int x = component.minX; x <= component.maxX; x++)
                if (labels[(component.minY + y) * width + x] == OmrMeasurePostProcessor.NOTEHEAD)
                    rows[y]++;
            peak = Math.max(peak, rows[y]);
        }
        if (peak < gap * .75f) return List.of();
        List<Float> centers = new ArrayList<>();
        for (int y = 0; y < height;) {
            if (rows[y] < peak * .85f) { y++; continue; }
            int start = y;
            while (y < height && rows[y] >= peak * .85f) y++;
            if (y - start < Math.max(2, Math.round(gap * .20f))) return List.of();
            centers.add(component.minY + (start + y - 1) * .5f);
        }
        if (centers.size() < 2 || centers.size() > 4) return List.of();
        for (int i = 1; i < centers.size(); i++) {
            float separation = centers.get(i) - centers.get(i - 1);
            if (separation < gap * .75f || separation > gap * 1.4f) return List.of();
        }
        List<Component> parts = new ArrayList<>();
        int top = component.minY;
        for (int i = 0; i < centers.size(); i++) {
            int bottom = i + 1 == centers.size() ? component.maxY
                    : Math.round((centers.get(i) + centers.get(i + 1)) * .5f);
            Component part = componentSlice(labels, width, component, top, bottom);
            if (part == null || !plausibleHead(part, gap)
                    || Math.abs(part.centerY - centers.get(i)) > gap * .3f) return List.of();
            parts.add(part);
            top = bottom + 1;
        }
        return parts;
    }

    /** Ledger paint can hide a middle lobe of a hollow triad in the mask. Accept
     * equal slices only when every slice contains an independently printed open oval. */
    private static List<Component> splitHollowStack(byte[] labels, byte[] gray, int width, int height,
                                                    Component head, Staff staff) {
        if (gray == null || staff == null) return List.of();
        float gap = staff.pitchGap;
        int span = head.maxY - head.minY + 1;
        int count = Math.round(span / gap);
        if (count < 3 || count > 4 || span < gap * (count - .25f)
                || span > gap * (count + .4f)
                || head.maxX - head.minX + 1 > gap * 1.8f) return List.of();
        List<Component> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int top = head.minY + Math.round(i * span / (float)count);
            int bottom = head.minY + Math.round((i + 1) * span / (float)count) - 1;
            Component part = componentSlice(labels, width, head, top, bottom);
            if (part == null || !plausibleHead(part, gap)
                    || part.maxX - part.minX + 1 < gap * .75f
                    || !hasOpenCenter(labels, gray, width, height, part, gap)) return List.of();
            if (!parts.isEmpty()) {
                float separation = part.centerY - parts.get(parts.size() - 1).centerY;
                if (separation < gap * .75f || separation > gap * 1.4f) return List.of();
            }
            parts.add(part);
        }
        return parts;
    }

    /** Dots beyond the right-hand held voice do not lengthen its beamed unison. */
    private static boolean hasHollowUnisonToRight(byte[] labels,byte[] gray,int width,int height,
                                                Component head,List<Component> heads,float gap) {
        if(gray==null)return false;
        for(Component other:heads)if(other.centerX-head.centerX>=gap&&other.centerX-head.centerX<=gap*2
                &&Math.abs(other.centerY-head.centerY)<gap*.3f
                &&hasAttachedStem(labels,width,height,other,gap)
                &&hasOpenCenter(labels,gray,width,height,other,gap))return true;
        return false;
    }

    private static List<Component> sideBySideUnison(byte[] labels,byte[] gray,int width,int height,
                                                   Component head,float gap) {
        float w=head.maxX-head.minX+1,h=head.maxY-head.minY+1;
        if(gray==null||w<gap*1.8f||w>gap*3.1f||h<gap*.65f||h>gap*1.4f)return List.of();
        int middle=(head.minX+head.maxX)/2;
        Component left=horizontalHeadSlice(labels,width,head,head.minX,middle);
        Component right=horizontalHeadSlice(labels,width,head,middle+1,head.maxX);
        if(left==null||right==null||Math.abs(left.centerY-right.centerY)>gap*.3f
                ||left.area<gap*gap*.35f||right.area<gap*gap*.3f
                ||hasOpenCenter(labels,gray,width,height,left,gap)
                ||!hasOpenCenter(labels,gray,width,height,right,gap)
                ||!hasAttachedStem(labels,width,height,left,gap)
                ||!hasAttachedStem(labels,width,height,right,gap))return List.of();
        return List.of(left,right);
    }

    private static List<Component> sideBySideSeconds(byte[] labels,int width,Component head,float gap) {
        float w=head.maxX-head.minX+1,h=head.maxY-head.minY+1;
        if(w<gap*2.1f||w>gap*3.1f||h<gap*1.05f||h>gap*1.9f)return List.of();
        int middle=(head.minX+head.maxX)/2;
        Component a=horizontalHeadSlice(labels,width,head,head.minX,middle);
        Component b=horizontalHeadSlice(labels,width,head,middle+1,head.maxX);
        if(a==null||b==null||a.area<gap*gap*.4f||b.area<gap*gap*.4f
                ||Math.abs(a.centerY-b.centerY)<gap*.3f||Math.abs(a.centerY-b.centerY)>gap*.8f
                ||a.maxY-a.minY<gap*.65f||b.maxY-b.minY<gap*.65f)return List.of();
        return List.of(a,b);
    }

    private static Component horizontalHeadSlice(byte[] labels,int width,Component head,int left,int right) {
        int area=0,minY=head.maxY,maxY=head.minY;long sumX=0,sumY=0;
        for(int y=head.minY;y<=head.maxY;y++)for(int x=left;x<=right;x++)
            if(labels[y*width+x]==OmrMeasurePostProcessor.NOTEHEAD){area++;sumX+=x;sumY+=y;minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        return area==0?null:new Component(area,left,right,minY,maxY,sumX/(float)area,sumY/(float)area);
    }

    private static Component componentSlice(byte[] labels, int width, Component source,
                                             int top, int bottom) {
        int area = 0, minX = source.maxX + 1, maxX = source.minX - 1;
        int minY = bottom + 1, maxY = top - 1;
        long sumX = 0, sumY = 0;
        for (int y = top; y <= bottom; y++) for (int x = source.minX; x <= source.maxX; x++) {
            if (labels[y * width + x] != OmrMeasurePostProcessor.NOTEHEAD) continue;
            area++; sumX += x; sumY += y;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        return area < 3 ? null : new Component(area, minX, maxX, minY, maxY,
                sumX / (float) area, sumY / (float) area);
    }

    private static List<Staff> findStaffs(byte[] labels, byte[] gray, int width, int height,
                                          List<MeasureRegion> measures) {
        int[] projection = new int[height];
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++)
                if (labels[row + x] == OmrMeasurePostProcessor.STAFF) projection[y]++;
        }
        int threshold = Math.max(8, Math.round(width * 0.055f));
        List<Staff> staffs = new ArrayList<>();
        for (RawStaffLineDetector.StaffLines semantic :
                RawStaffLineDetector.detectFromStrength(projection, threshold, height))
            staffs.add(new Staff(semantic.top(), semantic.bottom(), semantic.gap()));

        // The peak detector is deliberately selective so a slur/beam cannot masquerade as a
        // second staff. On a faded scan that selectivity can omit a complete, otherwise clean
        // system (Phantom of the Opera pages 4/5 exposed this). Recover only legacy five-band
        // candidates that line up with an already accepted measure row and the page's staff
        // scale. This forms a union on real score rows without restoring the old stray-text
        // candidates that made Hedwig's Theme unreadable.
        for (Staff legacy : legacySemanticStaffs(projection, threshold, height)) {
            if (!alignedWithMeasureRow(legacy, measures, height)
                    || !compatibleStaffScale(legacy, staffs)
                    || !isolatedMissingSystem(legacy, staffs)
                    || representedStaff(legacy, staffs, height)) continue;
            staffs.add(legacy);
        }
        // Dense notes can interrupt one semantic stripe enough to miss the
        // pitch reader's stronger threshold even though the measure pass has
        // already established the row. Recover only a separate, correctly
        // spaced system inside those existing measure bounds.
        for (RawStaffLineDetector.StaffLines weak : RawStaffLineDetector.detectFromStrength(
                projection, Math.max(10,width/80),height)) {
            Staff recovered = new Staff(weak.top(),weak.bottom(),weak.gap());
            if (alignedWithMeasureRow(recovered,measures,height)
                    && compatibleStaffScale(recovered,staffs)
                    && isolatedMissingSystem(recovered,staffs)
                    && !representedStaff(recovered,staffs,height)) staffs.add(recovered);
        }
        // The measure reader already deskews semantic rules. Use that same
        // evidence for missing pitch staffs so an accepted row cannot go silent.
        float semanticSlope=OmrMeasurePostProcessor.estimateStaffSlope(labels,width,height);
        if(Math.abs(semanticSlope)>.001f) {
            int[] deskewed=new int[height];
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(labels[y*width+x]==OmrMeasurePostProcessor.STAFF) {
                int row=Math.round(y-semanticSlope*(x-width*.5f));
                if(row>=0&&row<height)deskewed[row]++;
            }
            for(var lines:RawStaffLineDetector.detectFromStrength(deskewed,Math.max(10,width/80),height)) {
                Staff recovered=new Staff(lines.top(),lines.bottom(),lines.gap());
                recovered.pitchSlope=semanticSlope;
                if(alignedWithMeasureRow(recovered,measures,height)&&compatibleStaffScale(recovered,staffs)
                        &&isolatedMissingSystem(recovered,staffs)&&!representedStaff(recovered,staffs,height))staffs.add(recovered);
            }
        }
        for (RawStaffLineDetector.StaffLines raw : RawStaffLineDetector.detect(gray, width, height)) {
            Staff recovered = new Staff(raw.top(), raw.bottom(), raw.gap());
            float[] pitch=printedStaffPitch(gray,width,height,raw);
            // Semantic paint can shorten the outer-line spacing by several pixels. That
            // error accumulates on ledger notes and moves them to a different printed pitch.
            // Keep semantic geometry for symbol ownership, but calibrate pitch to a matching
            // complete five-line staff measured directly from the page.
            // A missing semantic outer rule can admit a ledger line and shift the
            // entire five-line group by one gap. A complete printed staff also
            // calibrates that case; the nearby group must still overlap four rules.
            for (Staff staff : staffs) if (pitch!=null && Math.abs(staff.top-raw.top()) <= staff.gap*1.2f
                    && Math.abs(staff.bottom-raw.bottom()) <= staff.gap*1.2f
                    && raw.gap() >= staff.gap*.85f && raw.gap() <= staff.gap*1.18f) {
                staff.pitchGap=pitch[1];staff.pitchBottom=pitch[0];
            }
            if(pitch!=null){recovered.pitchGap=pitch[1];recovered.pitchBottom=pitch[0];}
            if (!representedStaff(recovered, staffs, height)) staffs.add(recovered);
        }
        staffs.sort(Comparator.comparingDouble(staff -> staff.top));
        for(Staff staff:staffs)staff.pitchTrack=StaffPitchTrack.detect(gray,width,height,staff.top,staff.bottom,staff.pitchGap);
        assignSystemPositions(staffs, measures, height);
        return staffs;
    }

    /** Center complete printed rule bands instead of using whichever edge pixel wins a peak. */
    private static float[] printedStaffPitch(byte[] gray,int width,int height,
                                             RawStaffLineDetector.StaffLines raw) {
        float[] centers=new float[5];int radius=Math.max(2,Math.round(raw.gap()*.28f));
        for(int line=0;line<5;line++) {
            int first=Math.max(0,raw.rows()[line]-radius),last=Math.min(height-1,raw.rows()[line]+radius);
            int[] strength=new int[last-first+1];int strongest=0,peak=0;
            for(int y=first;y<=last;y++) {
                int n=0;for(int x=0;x<width;x++)if((gray[y*width+x]&255)<=170)n++;
                strength[y-first]=n;if(n>strongest){strongest=n;peak=y-first;}
            }
            if(strongest<width*.25f)return null;
            int start=peak,end=peak;
            while(start>0&&strength[start-1]>=strongest*.85f)start--;
            while(end+1<strength.length&&strength[end+1]>=strongest*.85f)end++;
            if(end-start+1>Math.max(3,raw.gap()*.4f))return null;
            centers[line]=first+(start+end)*.5f;
            // A dense row of beams can outscore an outer staff rule in the
            // page projection. A printed rule stays thin in individual columns,
            // even on a skewed scan; beams do not. Do not let such a candidate
            // move the pitch reference by an entire staff gap.
            int thinColumns=0,maxThickness=Math.max(3,Math.round(raw.gap()*.4f));
            for(int x=0;x<width;x++) {
                for(int y=first;y<=last;y++) {
                    if((gray[y*width+x]&255)>170)continue;
                    int a=y,b=y;
                    while(a>0&&y-a<=maxThickness&&(gray[(a-1)*width+x]&255)<=170)a--;
                    while(b+1<height&&b-a<maxThickness&&(gray[(b+1)*width+x]&255)<=170)b++;
                    if(b-a+1<=maxThickness){thinColumns++;break;}
                    y=b;
                }
            }
            if(thinColumns<width*.25f)return null;
        }
        float gap=(centers[4]-centers[0])/4;
        for(int line=1;line<5;line++)if(Math.abs(centers[line]-centers[line-1]-gap)>
                Math.max(.75f,gap*.12f))return null;
        return new float[]{centers[4],gap};
    }

    /** Original contiguous-band reader retained as a conservative fallback on known score rows. */
    private static List<Staff> legacySemanticStaffs(int[] projection, int threshold, int height) {
        List<Float> lines = new ArrayList<>();
        for (int row = 0; row < height;) {
            if (projection[row] < threshold) { row++; continue; }
            long weighted = 0, strength = 0;
            int end = row;
            while (end < height && projection[end] >= threshold) {
                weighted += (long) end * projection[end];
                strength += projection[end++];
            }
            lines.add(strength == 0 ? (float) row : weighted / (float) strength);
            row = end;
        }
        List<Staff> result = new ArrayList<>();
        for (int start = 0; start + 4 < lines.size();) {
            float[] gaps = new float[4];
            for (int index = 0; index < gaps.length; index++)
                gaps[index] = lines.get(start + index + 1) - lines.get(start + index);
            float gap = median(gaps);
            boolean regular = gap >= 2f && gap <= height * .045f;
            for (float candidate : gaps)
                regular &= Math.abs(candidate - gap) <= Math.max(1.5f, gap * .34f);
            if (!regular) { start++; continue; }
            result.add(new Staff(lines.get(start), lines.get(start + 4), gap));
            start += 5;
        }
        return result;
    }

    private static boolean alignedWithMeasureRow(Staff staff, List<MeasureRegion> measures,
                                                  int pageHeight) {
        if (measures == null || measures.isEmpty()) return false;
        float center = (staff.top + staff.bottom) * .5f / pageHeight;
        float tolerance = staff.gap * .75f / pageHeight;
        for (MeasureRegion measure : measures)
            if (center >= measure.top() - tolerance && center <= measure.bottom() + tolerance)
                return true;
        return false;
    }

    private static boolean compatibleStaffScale(Staff candidate, List<Staff> accepted) {
        if (accepted == null || accepted.isEmpty()) return true;
        List<Float> gaps = new ArrayList<>();
        for (Staff staff : accepted) gaps.add(staff.gap);
        gaps.sort(Float::compare);
        float median = gaps.get(gaps.size() / 2);
        return candidate.gap >= median * .68f && candidate.gap <= median * 1.47f;
    }

    private static boolean isolatedMissingSystem(Staff candidate, List<Staff> accepted) {
        if (accepted == null || accepted.isEmpty()) return false;
        float center = (candidate.top + candidate.bottom) * .5f;
        for (Staff staff : accepted) {
            float other = (staff.top + staff.bottom) * .5f;
            // assignSystemPositions uses the same separation boundary. A candidate inside that
            // radius belongs to an already represented grand/orchestral system, not a missing
            // line of music, and admitting it can perturb the selected melody staff.
            if (Math.abs(other - center) <= Math.max(candidate.gap, staff.gap)
                    * MAX_STAFFS_IN_SYSTEM_SEPARATION_GAPS)
                return false;
        }
        return true;
    }

    private static boolean representedStaff(Staff candidate, List<Staff> accepted, int height) {
        float center = (candidate.top + candidate.bottom) * .5f;
        for (Staff staff : accepted) {
            float other = (staff.top + staff.bottom) * .5f;
            if (Math.abs(other - center) <= Math.max(candidate.gap * 2.2f, height * .008f))
                return true;
        }
        return false;
    }

    /** Uses the measure rectangles already resolved by the barline/bracket pass as the authority
     * for system membership. This permits a page to switch between solo rows and connected duet
     * rows without a global spacing guess joining the wrong pair or serializing the duet. */
    private static void assignSystemPositions(List<Staff> staffs, List<MeasureRegion> measures,
                                              int pageHeight) {
        int start = 0;
        while (start < staffs.size()) {
            int end = start + 1;
            while (end < staffs.size()) {
                Staff previous = staffs.get(end - 1), next = staffs.get(end);
                if (!shareMeasureSystem(previous, next, measures, pageHeight)) break;
                end++;
            }
            int count = end - start;
            for (int index = start; index < end; index++) {
                Staff staff = staffs.get(index);
                staff.index = index - start;
                staff.count = count;
            }
            start = end;
        }
    }

    private static boolean shareMeasureSystem(Staff first, Staff second,
                                              List<MeasureRegion> measures, int pageHeight) {
        if (measures == null || pageHeight <= 0) return false;
        float firstCenter = (first.top + first.bottom) * .5f / pageHeight;
        float secondCenter = (second.top + second.bottom) * .5f / pageHeight;
        float tolerance = Math.max(first.gap, second.gap) * .45f / pageHeight;
        for (MeasureRegion measure : measures)
            if (firstCenter >= measure.top() - tolerance
                    && firstCenter <= measure.bottom() + tolerance
                    && secondCenter >= measure.top() - tolerance
                    && secondCenter <= measure.bottom() + tolerance) return true;
        return false;
    }

    private static List<Component> findComponents(byte[] labels, int width, int height, byte target) {
        boolean[] visited = new boolean[labels.length];
        int[] stack = new int[labels.length];
        List<Component> result = new ArrayList<>();
        for (int origin = 0; origin < labels.length; origin++) {
            if (visited[origin] || labels[origin] != target) continue;
            int stackSize = 0;
            stack[stackSize++] = origin;
            visited[origin] = true;
            int area = 0, minX = width, minY = height, maxX = 0, maxY = 0;
            long sumX = 0, sumY = 0;
            while (stackSize > 0) {
                int current = stack[--stackSize];
                int x = current % width, y = current / width;
                area++; sumX += x; sumY += y;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    int next = ny * width + nx;
                    if (!visited[next] && labels[next] == target) {
                        visited[next] = true;
                        stack[stackSize++] = next;
                    }
                }
            }
            if (area >= 3) result.add(new Component(area, minX, maxX, minY, maxY,
                    sumX / (float) area, sumY / (float) area));
        }
        return result;
    }

    private static Staff nearestStaff(List<Staff> staffs, float y) {
        return nearestStaff(staffs, y, 5f);
    }

    private static Staff nearestHeadStaff(List<Staff> staffs, float y) {
        return nearestStaff(staffs, y, MAX_HEAD_LEDGER_GAPS);
    }

    /**
     * Ledger notes between two adjacent staves can be a fraction of a pixel closer to the
     * wrong stave. Their attached stem normally points back toward the owning stave, so use that
     * topology only for genuinely ambiguous adjacent staves and retain nearest-staff everywhere
     * else. This prevents a high piano note from joining and lengthening a simultaneous violin
     * note in mixed violin/piano scores.
     */
    private static Staff staffForHead(byte[] labels, byte[] gray, int width, int height,
                                      List<Staff> staffs, Component head) {
        Staff nearest = nearestHeadStaff(staffs, head.centerY);
        if (nearest == null) return null;
        Staff upper = null, lower = null;
        for (Staff staff : staffs) {
            if (staff.bottom < head.centerY
                    && (upper == null || staff.bottom > upper.bottom)) upper = staff;
            if (staff.top > head.centerY
                    && (lower == null || staff.top < lower.top)) lower = staff;
        }
        if (upper == null || lower == null) return nearest;
        // A head within a third staff is not between these two candidates.
        if (nearest != upper && nearest != lower) return nearest;
        boolean adjacentParts=upper.count==lower.count&&upper.index+1==lower.index;
        boolean adjacentSoloRows=upper.count==1&&lower.count==1;
        if (!adjacentParts&&!adjacentSoloRows) return nearest;
        float upperDistance = head.centerY - upper.bottom;
        float lowerDistance = lower.top - head.centerY;
        float smaller = Math.max(.001f, Math.min(upperDistance, lowerDistance));
        if (Math.max(upperDistance, lowerDistance) > smaller * 1.55f) return nearest;

        // Beamed voices may keep an upward stem even above the bass staff.
        // The ledger chain, when visible, identifies the printed pitch staff
        // more reliably than that stem direction.
        int upperLedgers=innerLedgerCount(gray,width,height,head,upper);
        int lowerLedgers=innerLedgerCount(gray,width,height,head,lower);
        if(upperLedgers>0&&lowerLedgers==0)return upper;
        if(lowerLedgers>0&&upperLedgers==0)return lower;
        float gap = Math.min(upper.gap, lower.gap);
        int upward = attachedStemReach(labels, width, height, head, gap, true);
        int downward = attachedStemReach(labels, width, height, head, gap, false);
        int minimum = Math.max(3, Math.round(gap * .72f));
        int advantage = Math.max(2, Math.round(gap * .24f));
        if (upward >= minimum && upward >= downward + advantage) return upper;
        if (downward >= minimum && downward >= upward + advantage) return lower;
        return nearest;
    }

    private static int innerLedgerCount(byte[] gray,int width,int height,Component head,Staff staff) {
        if(gray==null)return 0;
        float gap=staff.pitchGap;
        boolean above=head.centerY<staff.pitchBottom-gap*4;
        float outer=above?staff.pitchBottom-gap*4:staff.pitchBottom;
        float direction=above?-1:1;
        int left=Math.max(0,Math.round(head.minX-gap*.3f));
        int right=Math.min(width-1,Math.round(head.maxX+gap*.3f));
        int count=0;
        for(float line=outer+direction*gap;direction*(head.centerY-line)>gap*.65f;line+=direction*gap) {
            boolean found=false;
            for(int y=Math.max(0,Math.round(line-gap*.18f));y<=Math.min(height-1,Math.round(line+gap*.18f));y++) {
                int ink=0,leftInk=0,rightInk=0;
                for(int x=left;x<=right;x++)if((gray[y*width+x]&255)<165) {
                    ink++;if(x<head.minX)leftInk++;if(x>head.maxX)rightInk++;
                }
                int margin=Math.max(1,Math.round(gap*.18f));
                if(ink>=(right-left+1)*.85f&&leftInk>=margin&&rightInk>=margin
                        &&shortLedgerRule(gray,width,y,head,gap))found=true;
            }
            if(found)count++;
        }
        return count;
    }

    /** Ending brackets and long beams do not identify a ledger pitch. */
    private static boolean shortLedgerRule(byte[] gray,int width,int y,Component head,float gap) {
        int center=Math.max(0,Math.min(width-1,Math.round(head.centerX)));
        if((gray[y*width+center]&255)>=165)return false;
        int left=center,right=center,limit=Math.max(4,Math.round(gap*4.5f));
        while(left>0&&center-left<=limit&&(gray[y*width+left-1]&255)<165)left--;
        while(right+1<width&&right-center<=limit&&(gray[y*width+right+1]&255)<165)right++;
        return right-left+1<=limit;
    }

    private static int attachedStemReach(byte[] labels, int width, int height,
                                         Component head, float gap, boolean upward) {
        int left = Math.max(0, Math.round(head.minX - gap * .42f));
        int right = Math.min(width - 1, Math.round(head.maxX + gap * .42f));
        int first = upward ? head.minY - 1 : head.maxY + 1;
        int limit = upward
                ? Math.max(0, Math.round(head.centerY - gap * 4.8f))
                : Math.min(height - 1, Math.round(head.centerY + gap * 4.8f));
        int reach = 0, blanks = 0;
        for (int y = first; upward ? y >= limit : y <= limit; y += upward ? -1 : 1) {
            boolean stem = false;
            for (int x = left; x <= right; x++)
                if (labels[y * width + x] == OmrMeasurePostProcessor.STEM_OR_REST) {
                    stem = true;
                    break;
                }
            if (stem) { reach++; blanks = 0; }
            else if (++blanks > 1) break;
        }
        return reach;
    }

    private static Staff nearestStaff(List<Staff> staffs, float y, float maximumGapDistance) {
        Staff best = null;
        float distance = Float.MAX_VALUE;
        for (Staff staff : staffs) {
            float candidate = y < staff.top ? staff.top - y : y > staff.bottom ? y - staff.bottom : 0;
            if (candidate < distance) { distance = candidate; best = staff; }
        }
        return best != null && distance <= best.gap * maximumGapDistance ? best : null;
    }

    private static boolean hasLedgerInk(byte[] gray,int width,int height,Component head,float gap) {
        boolean stemless=attachedRawStem(gray,width,height,head,gap)==null;
        int left=Math.max(0,Math.round(head.centerX-gap*1.2f));
        int right=Math.min(width-1,Math.round(head.centerX+gap*1.2f));
        for(int y=Math.max(0,Math.round(head.centerY-gap*.65f));
                y<=Math.min(height-1,Math.round(head.centerY+gap*.65f));y++) {
            int run=0;
            for(int x=left;x<=right;x++) {
                boolean dark=(gray[y*width+x]&255)<160;
                if(!dark&&y>0&&y+1<height)dark=(gray[(y-1)*width+x]&255)<160
                        ||(gray[(y+1)*width+x]&255)<160;
                run=dark?run+1:0;
                // A horizontal instruction arrow also ends in a head-like blob.
                // A stemless ledger note has rule ink on both sides of its oval.
                if(run>=gap*1.5f&&(!stemless||(x-run+1<head.minX&&x>head.maxX)))return true;
            }
        }
        return false;
    }

    private static boolean hasInnerLedgerInk(byte[] gray,int width,int height,Component head,Staff staff) {
        // Beyond two staff spaces, real notation needs another ledger toward
        // the staff. One instruction arrow or underline is insufficient.
        float direction=head.centerY<staff.top?1:-1;
        float center=head.centerY+direction*staff.gap;
        int left=Math.max(0,Math.round(head.centerX-staff.gap*1.2f));
        int right=Math.min(width-1,Math.round(head.centerX+staff.gap*1.2f));
        for(int y=Math.max(1,Math.round(center-staff.gap*.55f));
                y<=Math.min(height-2,Math.round(center+staff.gap*.55f));y++) {
            int run=0;
            for(int x=left;x<=right;x++) {
                boolean dark=(gray[y*width+x]&255)<160||(gray[(y-1)*width+x]&255)<160
                        ||(gray[(y+1)*width+x]&255)<160;
                run=dark?run+1:0;
                if(run>=staff.gap*1.5f)return true;
            }
        }
        return false;
    }

    /** A thin slur fragment joined to a staff line can form a false semantic oval.
     * Require a stem for this unusually flat shape; normal whole notes are taller. */
    private static boolean flatStemlessFragment(byte[] gray, int width, int height,
                                                Component head, float gap) {
        float w = head.maxX - head.minX + 1f, h = head.maxY - head.minY + 1f;
        return gray != null && h < gap * .65f && w > h * 2.2f
                && attachedRawStem(gray, width, height, head, gap) == null;
    }

    /** Tremolo strokes cross both sides of a stem. A fragmented mask may join
     * two strokes into a plausible small head; the repeated short raw bands and
     * the larger head owning that stem distinguish them from a chord. */
    private static List<Component> stemSlashFragments(byte[] gray, int width, int height,
                                                      List<Component> heads, List<Staff> staffs) {
        List<Component> fragments = new ArrayList<>();
        if (gray == null) return fragments;
        for (Component candidate : heads) {
            Staff staff = nearestHeadStaff(staffs, candidate.centerY);
            if (staff == null) continue;
            float gap = staff.gap;
            if (candidate.area > gap * gap * .65f
                    || candidate.maxX-candidate.minX+1 > gap * 1.2f) continue;
            for (Component main : heads) {
                float distance = Math.abs(main.centerY-candidate.centerY);
                if (main == candidate || main.area < candidate.area * 2.2f
                        || main.maxX-main.minX+1 < gap
                        || distance < gap * 1.2f || distance > gap * 4.5f
                        || Math.abs(main.centerX-candidate.centerX) > gap) continue;
                int[] stem = attachedRawStem(gray, width, height, main, gap);
                if (stem == null || (candidate.centerY-main.centerY)*stem[2] <= 0
                        || (candidate.centerY-stem[1])*stem[2] > gap * .3f
                        || Math.abs(candidate.centerX-stem[0]) > gap * .9f) continue;
                int edge = stem[2] > 0 ? main.maxY : main.minY;
                int first = Math.max(1, Math.min(edge + stem[2]*Math.round(gap*.25f), stem[1]));
                int last = Math.min(height-2, Math.max(edge + stem[2]*Math.round(gap*.25f), stem[1]));
                int side = Math.max(2, Math.round(gap*.42f)), far = Math.round(gap*2f);
                if (stem[0]-far < 0 || stem[0]+far >= width) continue;
                int run = 0, bands = 0; boolean touches = false;
                for (int y=first; y<=last+1; y++) {
                    boolean shortStroke = y<=last
                            && rawColumnInk(gray,width,stem[0]-side,y)
                            && rawColumnInk(gray,width,stem[0]+side,y)
                            && !(rawColumnInk(gray,width,stem[0]-far,y)
                            && rawColumnInk(gray,width,stem[0]+far,y));
                    if (shortStroke) run++;
                    else {
                        if (run >= Math.max(2, Math.round(gap*.18f)) && run <= gap*.85f) {
                            bands++;
                            float center = y-(run+1)*.5f;
                            if (Math.abs(center-candidate.centerY) < gap*.75f) touches=true;
                        }
                        run=0;
                    }
                }
                if (bands >= 2 && touches) { fragments.add(candidate); break; }
            }
        }
        return fragments;
    }

    /** Reduced heads in a close, stemmed prefix are ornaments, not extra metrical beats. */
    private static void markGraceHeads(byte[] labels, byte[] gray, int width, int height,
            List<DetectedNote> detected, List<ScoreNoteEvent> events) {
        for (int i=0;i<detected.size();i++) {
            DetectedNote first=detected.get(i);
            if (!smallGraceHead(first) || (gray != null
                    ? attachedRawStem(gray,width,height,first.head,first.staffGap*.65f)==null
                    : !hasAttachedStem(labels,width,height,first.head,first.staffGap))) continue;
            List<Integer> prefix=new ArrayList<>(); prefix.add(i);
            DetectedNote previous=first;
            for(int j=i+1;j<detected.size();j++) {
                DetectedNote next=detected.get(j);
                if(next.event.measureIndex()!=first.event.measureIndex())break;
                if(next.event.staffIndex()!=first.event.staffIndex()
                        ||next.event.staffCount()!=first.event.staffCount())continue;
                float dx=next.head.centerX-previous.head.centerX;
                if(dx<first.staffGap*.65f || dx>first.staffGap*2.8f
                        ||Math.abs(next.head.centerY-previous.head.centerY)>first.staffGap*2.5f)break;
                if(smallGraceHead(next)) {
                    if(prefix.size()>=4 || (gray!=null
                            ? attachedRawStem(gray,width,height,next.head,next.staffGap*.65f)==null
                            : !hasAttachedStem(labels,width,height,next.head,next.staffGap)))break;
                    prefix.add(j);previous=next;continue;
                }
                if(next.head.area>first.head.area*1.65f
                        &&next.head.maxX-next.head.minX+1>first.staffGap*1.05f)
                    for(int index:prefix)events.set(index,events.get(index).withArticulations(
                            events.get(index).articulations()|NoteOrnament.GRACE));
                break;
            }
        }
    }

    private static boolean smallGraceHead(DetectedNote n) {
        return n.head.maxX-n.head.minX+1<=n.staffGap*.95f
                &&n.head.maxY-n.head.minY+1<=n.staffGap*.78f
                &&n.head.area<=n.staffGap*n.staffGap*.60f
                &&n.event.augmentationDots()==0
                &&n.event.unbeamedDurationBeats()<ScoreNoteEvent.DURATION_HALF;
    }

    private static boolean rawColumnInk(byte[] gray, int width, int x, int y) {
        return (gray[(y-1)*width+x]&255)<170 || (gray[y*width+x]&255)<170
                || (gray[(y+1)*width+x]&255)<170;
    }

    private static boolean plausibleHead(Component head, float gap) {
        float width = head.maxX - head.minX + 1f, height = head.maxY - head.minY + 1f;
        return width >= Math.max(2f, gap * .38f) && height >= Math.max(2f, gap * .30f)
                && width <= gap * 3.2f && height <= gap * 3.2f
                && head.area >= Math.max(4, Math.round(gap * gap * .11f));
    }

    private static List<Component> augmentationDotHeads(byte[] labels, int width, int height,
                                                         List<Component> heads,
                                                         List<Staff> staffs) {
        List<Component> dots = new ArrayList<>();
        for (Component candidate : heads) {
            Staff staff = nearestHeadStaff(staffs, candidate.centerY);
            if (staff == null) continue;
            float gap = staff.gap;
            float candidateWidth = candidate.maxX - candidate.minX + 1f;
            float candidateHeight = candidate.maxY - candidate.minY + 1f;
            if (candidateWidth > gap * .82f || candidateHeight > gap * .82f
                    || candidate.area > gap * gap * .48f
                    || hasAttachedStem(labels, width, height, candidate, gap)) continue;
            for (Component main : heads) {
                if (main == candidate || nearestHeadStaff(staffs, main.centerY) != staff) continue;
                float mainWidth = main.maxX - main.minX + 1f;
                float mainHeight = main.maxY - main.minY + 1f;
                float horizontal = candidate.minX - main.maxX;
                if (horizontal < gap * .10f || horizontal > gap * 2.20f
                        || Math.abs(candidate.centerY - main.centerY) > gap * .78f) continue;
                if (mainWidth < gap * .62f || mainHeight < gap * .44f
                        || main.area < candidate.area * 1.55f) continue;
                dots.add(candidate);
                break;
            }
        }
        return dots;
    }

    private static boolean hasAttachedStem(byte[] labels, int width, int height,
                                            Component head, float gap) {
        int left = Math.max(0, Math.round(head.minX - gap * .36f));
        int right = Math.min(width - 1, Math.round(head.maxX + gap * .36f));
        int top = Math.max(0, Math.round(head.minY - gap * 2.8f));
        int bottom = Math.min(height - 1, Math.round(head.maxY + gap * 2.8f));
        int rows = 0, above = 0, below = 0;
        for (int y = top; y <= bottom; y++) {
            boolean stem = false;
            for (int x = left; x <= right; x++) if (labels[y * width + x]
                    == OmrMeasurePostProcessor.STEM_OR_REST) { stem = true; break; }
            if (!stem) continue;
            rows++;
            if (y < head.minY) above++;
            if (y > head.maxY) below++;
        }
        return rows >= Math.max(4, Math.round(gap * .72f))
                && Math.max(above, below) >= Math.max(3, Math.round(gap * .42f));
    }

    /** A small isolated dot above/below a full head is an articulation, not another pitch. */
    private static List<Component> articulationDotHeads(byte[] labels, byte[] gray,
            int width, int height, List<Component> heads, List<Staff> staffs) {
        List<Component> dots = new ArrayList<>();
        for (Component candidate : heads) {
            Staff staff = nearestHeadStaff(staffs, candidate.centerY);
            if (staff == null) continue;
            float gap = staff.gap;
            float w = candidate.maxX - candidate.minX + 1f;
            float h = candidate.maxY - candidate.minY + 1f;
            if (w > gap * .75f || h > gap * .75f || candidate.area > gap * gap * .36f
                    || Math.max(w,h) > Math.min(w,h) * 1.6f) continue;
            // The broad semantic stem window can borrow the main note's stem.
            // Trace contiguous raw ink from this component when pixels exist.
            if (gray != null ? attachedRawStem(gray,width,height,candidate,gap) != null
                    : hasAttachedStem(labels,width,height,candidate,gap)) continue;
            for (Component main : heads) {
                if (main == candidate || nearestHeadStaff(staffs,main.centerY) != staff
                        || main.area < candidate.area * 2.5f) continue;
                float distance = Math.abs(main.centerY-candidate.centerY);
                if (Math.abs(main.centerX-candidate.centerX) <= gap * .90f
                        && distance >= gap * .78f && distance <= gap * 2.5f
                        && main.maxX-main.minX+1 >= gap * .85f) {
                    dots.add(candidate);
                    break;
                }
            }
        }
        return dots;
    }

    /**
     * Beams already encode eighth notes and shorter values. For otherwise identical un-beamed
     * notes, read the two features that spacing cannot provide: a stem distinguishes whole notes,
     * and an open notehead distinguishes half/whole notes from quarters. Keeping this written
     * value on the event prevents engraving spacing from randomly changing playback speed.
     */
    private static float detectUnbeamedDuration(byte[] labels, byte[] gray, int width, int height,
                                                 Component head, float gap, int beamCount) {
        boolean open = hasOpenCenter(labels, gray, width, height, head, gap);
        boolean stem = hasAttachedStem(labels, width, height, head, gap);
        if (open) return stem ? ScoreNoteEvent.DURATION_HALF : ScoreNoteEvent.DURATION_WHOLE;
        if (beamCount > 0) return ScoreNoteEvent.DURATION_UNKNOWN;
        // A filled head is a quarter even when a thin stem was missed by segmentation. Treating it
        // as unknown would send the renderer back to noisy horizontal spacing.
        return ScoreNoteEvent.DURATION_QUARTER;
    }

    private static boolean hasOpenCenter(byte[] labels, byte[] gray, int width, int height,
                                         Component head, float gap) {
        float headWidth = head.maxX - head.minX + 1f;
        float headHeight = head.maxY - head.minY + 1f;
        float semanticFill = head.area / Math.max(1f, headWidth * headHeight);
        // A merged pair of filled seconds has low rectangular fill too. When raw ink is
        // available require a real white pocket, not just the model component's outline.
        if (gray == null || gray.length != width * height) return semanticFill <= .62f;

        int left = Math.max(0, Math.round(head.centerX - headWidth * .23f));
        int right = Math.min(width - 1, Math.round(head.centerX + headWidth * .23f));
        int top = Math.max(0, Math.round(head.centerY - headHeight * .28f));
        int bottom = Math.min(height - 1, Math.round(head.centerY + headHeight * .28f));
        int samples = 0, brightHole = 0;
        for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
            samples++;
            int index = y * width + x;
            // The segmentation model sometimes paints the complete oval as NOTEHEAD even when
            // the rendered page has a plainly white center. The raw page pixel is the direct
            // evidence for a hollow half/whole head; requiring a non-NOTEHEAD semantic label here
            // converted those sustained notes to one-beat quarters and left silent measure tails.
            if ((gray[index] & 0xff) >= 185) brightHole++;
        }
        if (samples >= 3 && brightHole >= Math.max(2, Math.round(samples * .34f))) return true;
        // A tiny grace head or a fragmented model component can enclose a few antialiased
        // corner pixels without being a hollow half. Expanded pocket recovery is full-size only.
        if (headWidth < gap * 1.05f || headHeight < gap * .85f) return false;
        // Tinted scans and staff lines leave gray, rather than white, enclosed pockets.
        // Adapt only this four-sided pocket test; the simple center test stays conservative.
        int[] shades=new int[256];int shadeCount=0;
        int margin=Math.max(2,Math.round(gap));
        for(int y=Math.max(0,head.minY-margin);y<=Math.min(height-1,head.maxY+margin);y++)
            for(int x=Math.max(0,head.minX-margin);x<=Math.min(width-1,head.maxX+margin);x++) {
                shades[gray[y*width+x]&255]++;shadeCount++;
            }
        int background=255,seen=0;
        for(int value=0;value<256;value++)if((seen+=shades[value])>=shadeCount*.8){background=value;break;}
        int pocketThreshold=Math.min(185,Math.max(145,background-40));
        // A thick ledger/staff line can cover the middle of a hollow oval. Look for the
        // remaining enclosed white pockets above/below it, not just an average over its centre.
        // Requiring dark ink on both sides rejects the exterior corners of a filled/slanted head.
        int enclosed = 0, pocketRows = 0;
        for (int y = Math.max(head.minY + 1, top - 2);
             y <= Math.min(head.maxY - 1, bottom + 2); y++) {
            int rowHoles = 0;
            for (int x = Math.max(head.minX + 1, left - 1);
                 x <= Math.min(head.maxX - 1, right + 1); x++) {
                if ((gray[y * width + x] & 0xff) < pocketThreshold) continue;
                boolean inkLeft = false, inkRight = false, inkAbove = false, inkBelow = false;
                for (int xx = head.minX; xx < x; xx++)
                    if ((gray[y * width + xx] & 0xff) <= 135) { inkLeft = true; break; }
                for (int xx = x + 1; xx <= head.maxX; xx++)
                    if ((gray[y * width + xx] & 0xff) <= 135) { inkRight = true; break; }
                for (int yy = head.minY; yy < y; yy++)
                    if ((gray[yy * width + x] & 0xff) <= 135) { inkAbove = true; break; }
                for (int yy = y + 1; yy <= head.maxY; yy++)
                    if ((gray[yy * width + x] & 0xff) <= 135) { inkBelow = true; break; }
                if (inkLeft && inkRight && inkAbove && inkBelow) rowHoles++;
            }
            enclosed += rowHoles;
            if (rowHoles >= 2) pocketRows++;
        }
        return pocketRows >= 2 && enclosed >= Math.max(4, Math.round(headWidth * headHeight * .055f));
    }

    /** Reconnect narrow cuts in an accidental's semantic mask using the printed ink. */
    private static List<AccidentalCandidate> joinLocalAccidentalFragments(byte[] labels,
            byte[] gray, int width, int height, List<AccidentalCandidate> candidates,
            List<Staff> staffs) {
        List<AccidentalCandidate> sameLabel = joinAccidentalFragments(labels,gray,width,height,candidates,staffs,false);
        List<AccidentalCandidate> result = new ArrayList<>(sameLabel);
        // Keep the original glyphs: a cross-label union can also include a
        // nearby rule fragment and must not replace an already legible natural.
        for (AccidentalCandidate candidate : joinAccidentalFragments(labels,gray,width,height,sameLabel,staffs,true))
            if (candidate.label == 0) result.add(candidate);
        return result;
    }

    private static List<AccidentalCandidate> joinAccidentalFragments(byte[] labels,
            byte[] gray, int width, int height, List<AccidentalCandidate> candidates,
            List<Staff> staffs, boolean mixedLabels) {
        if (gray == null || gray.length != labels.length) return candidates;
        List<AccidentalCandidate> joined = new ArrayList<>(candidates);
        for (int i=0;i<joined.size();i++) {
            AccidentalCandidate a=joined.get(i);
            Staff staff=nearestHeadStaff(staffs,a.component.centerY);
            if(staff==null)continue;
            float gap=staff.gap;
            for(int j=i+1;j<joined.size();j++) {
                AccidentalCandidate b=joined.get(j);
                if (!mixedLabels && a.label != b.label) continue;
                Component ac=a.component,bc=b.component;
                int left=Math.min(ac.minX,bc.minX),right=Math.max(ac.maxX,bc.maxX);
                int top=Math.min(ac.minY,bc.minY),bottom=Math.max(ac.maxY,bc.maxY);
                int reach=Math.max(2,Math.round(gap*.42f));
                if(right-left+1>gap*1.65f||bottom-top+1>gap*3.70f
                        ||Math.max(ac.minX,bc.minX)-Math.min(ac.maxX,bc.maxX)>reach
                        ||Math.max(ac.minY,bc.minY)-Math.min(ac.maxY,bc.maxY)>reach)continue;
                boolean connected=false;
                for(int y=ac.minY;y<=ac.maxY&&!connected;y++)
                    for(int x=ac.minX;x<=ac.maxX&&!connected;x++) {
                        if(!a.matches(labels[y*width+x]))continue;
                        for(int by=Math.max(bc.minY,y-reach);by<=Math.min(bc.maxY,y+reach)&&!connected;by++)
                            for(int bx=Math.max(bc.minX,x-reach);bx<=Math.min(bc.maxX,x+reach);bx++) {
                                if(!b.matches(labels[by*width+bx]))continue;
                                int steps=Math.max(Math.abs(bx-x),Math.abs(by-y));
                                if(steps==0)continue;
                                boolean ink=true;
                                for(int k=1;k<steps;k++) {
                                    int px=Math.round(x+(bx-x)*(float)k/steps),py=Math.round(y+(by-y)*(float)k/steps);
                                    if((gray[py*width+px]&255)>180){ink=false;break;}
                                }
                                if(ink){connected=true;break;}
                            }
                    }
                if(!connected)continue;
                int area=ac.area+bc.area;
                a=new AccidentalCandidate(new Component(area,left,right,top,bottom,
                        (ac.centerX*ac.area+bc.centerX*bc.area)/area,
                        (ac.centerY*ac.area+bc.centerY*bc.area)/area),(byte)(a.label==b.label?a.label:0));
                joined.set(i,a);joined.remove(j);j=i;
            }
        }
        return joined;
    }

    /** Two surviving crossbars can locate a faded natural whose thin spines were
     * labelled as stems. Confirm the offset endpoints in the original pixels. */
    private static boolean rawNaturalFromCrossbars(byte[] gray,int width,int height,
            List<AccidentalCandidate> candidates,Component head,float gap) {
        if(gray==null)return false;
        for(AccidentalCandidate seed:candidates) {
            Component glyph=seed.component;
            if(head.minX-glyph.maxX < gap*.10f || head.minX-glyph.maxX > gap*1.35f
                    || Math.abs(glyph.centerY-head.centerY)>gap*.9f
                    || glyph.maxY-glyph.minY<gap*.65f
                    || glyph.maxX-glyph.minX<gap*.35f
                    || glyph.maxX-glyph.minX>gap*1.5f)continue;
            if(rawNaturalAtSeed(gray,width,height,glyph,head,gap))return true;
        }
        List<Component> bars=new ArrayList<>();
        for(var c:candidates) {
            Component g=c.component;
            if(g.maxX>head.minX+gap*.1f||head.minX-g.maxX>gap*1.6f
                    ||Math.abs(g.centerY-head.centerY)>gap*1.3f
                    ||g.maxY-g.minY>gap*.6f||g.maxX-g.minX<gap*.35f
                    ||g.maxX-g.minX>gap*1.25f)continue;
            bars.add(g);
        }
        for(Component a:bars)for(Component b:bars) {
            float dy=b.centerY-a.centerY;
            if(dy<gap*.65f||dy>gap*1.5f||Math.abs(a.centerX-b.centerX)>gap*.5f)continue;
            int left=Math.max(0,Math.round(Math.min(a.minX,b.minX)-gap*.2f));
            int right=Math.min(Math.round(head.minX-gap*.15f),Math.round(Math.max(a.maxX,b.maxX)));
            int top=Math.max(0,Math.round(a.minY-gap*.8f)),bottom=Math.min(height-1,Math.round(b.maxY+gap*.8f));
            int w=right-left+1,h=bottom-top+1;if(w<=0||h<=0)continue;
            byte[] mask=new byte[w*h];int area=0,minX=w,maxX=-1,minY=h,maxY=-1;long sx=0,sy=0;
            for(int y=top;y<=bottom;y++) {
                int outside=0,samples=0;
                for(int x=Math.max(0,left-Math.round(gap));x<=Math.min(width-1,right+Math.round(gap));x++)
                    if(x<left||x>right){samples++;if((gray[y*width+x]&255)<225)outside++;}
                if(samples>0&&outside>samples*.80f)continue;
                for(int x=left;x<=right;x++)if((gray[y*width+x]&255)<225) {
                    int xx=x-left,yy=y-top;mask[yy*w+xx]=OmrMeasurePostProcessor.SYMBOL;
                    area++;sx+=xx;sy+=yy;minX=Math.min(minX,xx);maxX=Math.max(maxX,xx);minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                }
            }
            if(area<3)continue;
            Component g=new Component(area,minX,maxX,minY,maxY,sx/(float)area,sy/(float)area);
            if(isNaturalGlyph(mask,w,h,new AccidentalCandidate(g,OmrMeasurePostProcessor.SYMBOL),gap))return true;
        }
        return false;
    }

    /** Thin natural spines can be assigned the stem label while their two
     * connectors retain the accidental label. Reconstruct only that narrow
     * printed column and require the natural's asymmetric spine endpoints. */
    private static boolean rawNaturalAtSeed(byte[] gray,int width,int height,
            Component seed,Component head,float gap) {
        return rawNaturalAtSeed(gray,width,height,seed,head,gap,0)
                ||rawNaturalAtSeed(gray,width,height,seed,head,gap,Math.max(1,Math.round(gap*.16f)));
    }

    private static boolean rawNaturalAtSeed(byte[] gray,int width,int height,
            Component seed,Component head,float gap,int margin) {
        int left=Math.max(0,seed.minX-margin);
        int right=Math.min(Math.min(width-1,seed.maxX+margin),Math.round(head.minX-gap*.15f));
        int top=Math.max(0,Math.round(head.centerY-gap*1.8f));
        int bottom=Math.min(height-1,Math.round(head.centerY+gap*1.8f));
        int w=right-left+1,h=bottom-top+1;
        byte[] ink=new byte[w*h];
        int area=0,minX=w,maxX=-1,minY=h,maxY=-1;long sx=0,sy=0;
        int reach=Math.max(3,Math.round(gap*.6f));
        int verticalProbe=Math.max(2,Math.round(gap*.2f));
        for(int y=top;y<=bottom;y++) {
            int outside=0,dark=0;
            for(int x=Math.max(0,left-reach);x<=Math.min(width-1,right+reach);x++)
                if(x<left||x>right){outside++;if((gray[y*width+x]&255)<=225)dark++;}
            boolean rule=outside>0&&dark>=outside*.8f;
            for(int x=left;x<=right;x++) {
                if((gray[y*width+x]&255)>225)continue;
                // Preserve a vertical spine where a rule crosses it.
                if(rule&&(y<verticalProbe||y+verticalProbe>=height
                        ||(gray[(y-verticalProbe)*width+x]&255)>225
                        ||(gray[(y+verticalProbe)*width+x]&255)>225))continue;
                int xx=x-left,yy=y-top;ink[yy*w+xx]=OmrMeasurePostProcessor.SYMBOL;
                area++;sx+=xx;sy+=yy;minX=Math.min(minX,xx);maxX=Math.max(maxX,xx);
                minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
            }
        }
        if(area==0)return false;
        Component glyph=new Component(area,minX,maxX,minY,maxY,sx/(float)area,sy/(float)area);
        return isNaturalGlyph(ink,w,h,new AccidentalCandidate(glyph,OmrMeasurePostProcessor.SYMBOL),gap);
    }

    /** Returns a local accidental immediately left of this head, or key-signature fallback. */
    private static int detectWrittenAccidental(byte[] labels, int width, int height,
                                               List<AccidentalCandidate> candidates,
                                               Component head, float gap) {
        AccidentalCandidate best = null;
        int bestAccidental = ScoreNoteEvent.ACCIDENTAL_FROM_KEY;
        float bestDistance = Float.MAX_VALUE;
        for (AccidentalCandidate candidate : candidates) {
            Component glyph = candidate.component;
            float horizontal = head.minX - glyph.maxX;
            if (horizontal < -gap * .35f || horizontal > gap * 1.35f
                    ||head.centerX-glyph.centerX<gap*.65f) continue;
            if (Math.abs(glyph.centerY - head.centerY) > gap * 1.8f) continue;
            float sharpCenter=sharpPitchCenter(labels,width,height,candidate,gap);
            int accidental = isNaturalGlyph(labels, width, height, candidate, gap)
                    ? ScoreNoteEvent.ACCIDENTAL_NATURAL
                    : Float.isFinite(sharpCenter)
                    ? ScoreNoteEvent.ACCIDENTAL_SHARP
                    : isFlatGlyph(labels, width, height, candidate, gap)
                    ? ScoreNoteEvent.ACCIDENTAL_FLAT
                    : ScoreNoteEvent.ACCIDENTAL_FROM_KEY;
            if (accidental == ScoreNoteEvent.ACCIDENTAL_FROM_KEY) continue;
            // A sharp belongs at the centre of its crossbars. Extra staff ink can
            // shift its pixel centroid toward another head in the same chord.
            float pitchCenter=accidental==ScoreNoteEvent.ACCIDENTAL_SHARP?sharpCenter:
                    accidental==ScoreNoteEvent.ACCIDENTAL_FLAT?flatPitchCenter(labels,width,candidate,gap):glyph.centerY;
            float tolerance=accidental==ScoreNoteEvent.ACCIDENTAL_NATURAL?.90f:.65f;
            if(Math.abs(pitchCenter-head.centerY)>gap*tolerance)continue;
            if (horizontal < bestDistance) {
                best = candidate;
                bestAccidental = accidental;
                bestDistance = horizontal;
            }
        }
        return best == null ? ScoreNoteEvent.ACCIDENTAL_FROM_KEY : bestAccidental;
    }

    /**
     * A flat has one tall left spine and a lower-right bowl. Naturals have a second upper-right
     * stem, while sharps are much wider; comparing the glyph's own semantic pixels avoids staff
     * lines and artwork texture in the grayscale page.
     */
    private static boolean isFlatGlyph(byte[] labels, int width, int height,
                                       AccidentalCandidate candidate, float gap) {
        Component glyph = candidate.component;
        int glyphWidth = glyph.maxX - glyph.minX + 1;
        int glyphHeight = glyph.maxY - glyph.minY + 1;
        if (glyphHeight < gap * 1.20f || glyphHeight > gap * 3.35f
                || glyphWidth < gap * .24f || glyphWidth > gap * 1.75f
                || glyphHeight < glyphWidth * 1.28f
                || glyph.area < gap * gap * .16f || glyph.area > gap * gap * 1.65f)
            return false;
        int[] columns = new int[glyphWidth];
        int spine = 0;
        for (int y = Math.max(0, glyph.minY); y <= Math.min(height - 1, glyph.maxY); y++)
            for (int x = Math.max(0, glyph.minX); x <= Math.min(width - 1, glyph.maxX); x++)
                if (candidate.matches(labels[y * width + x])) columns[x - glyph.minX]++;
        for (int column = 1; column < columns.length; column++)
            if (columns[column] > columns[spine]) spine = column;
        // A slur tail or grace flag bends rightward without a sustained left spine.
        if (spine > Math.round((glyphWidth - 1) * .48f)
                || columns[spine] < glyphHeight * .68f) return false;
        int splitY = glyph.minY + Math.round(glyphHeight * .45f);
        int upperEnd=glyph.minY+Math.max(1,Math.round(glyphHeight*.25f));
        int[] upperReach=new int[upperEnd-glyph.minY];
        for(int y=glyph.minY;y<upperEnd;y++)for(int x=glyph.minX+spine;x<=glyph.maxX;x++)
            if(candidate.matches(labels[y*width+x]))upperReach[y-glyph.minY]=x;
        java.util.Arrays.sort(upperReach);
        int upperEdge=upperReach[upperReach.length/2],expandedRows=0;
        for(int y=splitY;y<=glyph.maxY;y++)for(int x=Math.max(glyph.minX,upperEdge+Math.max(2,Math.round(gap*.2f)));x<=glyph.maxX;x++)
            if(candidate.matches(labels[y*width+x])){expandedRows++;break;}
        // A cut note stem can widen by a pixel at a staff crossing. A flat has
        // a distinct bowl projecting beyond the width of its upper stem.
        if(expandedRows<Math.max(2,Math.round(glyphHeight*.1f)))return false;
        int rightStart = glyph.minX + spine + Math.max(1, Math.round(glyphWidth * .16f));
        int upperRight = 0, lowerRight = 0, wideLowerRows = 0, bowlRows = 0;
        for (int y = glyph.minY; y <= glyph.maxY; y++) {
            int rowMin = glyph.maxX + 1, rowMax = glyph.minX - 1;
            for (int x = Math.max(glyph.minX, rightStart); x <= glyph.maxX; x++) {
                if (x < 0 || x >= width || y < 0 || y >= height
                        || !candidate.matches(labels[y * width + x])) continue;
                if (y < splitY) upperRight++; else lowerRight++;
                rowMin = Math.min(rowMin, x); rowMax = Math.max(rowMax, x);
            }
            if (y >= splitY && rowMax >= rowMin
                    && rowMax - rowMin + 1 >= glyphWidth * .42f) wideLowerRows++;
            // Narrow engraved flats have a hollow bowl: its curved outer edge can be just
            // one pixel wide on a row. Measure its reach from the spine as well as its ink.
            if (y >= splitY && rowMax - (glyph.minX + spine)
                    >= Math.max(gap * .30f, glyphWidth * .55f)) bowlRows++;
        }
        return lowerRight >= upperRight + Math.max(2, Math.round(gap * .16f))
                && lowerRight >= glyph.area * .13f
                && (wideLowerRows >= Math.max(2, Math.round(glyphHeight * .10f))
                    || bowlRows >= Math.max(3, Math.round(glyphHeight * .16f)));
    }

    /** A flat changes the note beside its bowl, not the note beside its tall spine. */
    private static float flatPitchCenter(byte[] labels,int width,AccidentalCandidate candidate,float gap) {
        Component glyph=candidate.component;
        int glyphWidth=glyph.maxX-glyph.minX+1,glyphHeight=glyph.maxY-glyph.minY+1;
        int[] columns=new int[glyphWidth];
        for(int y=glyph.minY;y<=glyph.maxY;y++)for(int x=glyph.minX;x<=glyph.maxX;x++)
            if(candidate.matches(labels[y*width+x]))columns[x-glyph.minX]++;
        int spine=0;
        for(int x=1;x<glyphWidth;x++)if(columns[x]>columns[spine])spine=x;
        int start=glyph.minX+spine+(int)Math.ceil(Math.max(gap*.30f,glyphWidth*.55f));
        int first=-1,last=-1;
        for(int y=glyph.minY+Math.round(glyphHeight*.45f);y<=glyph.maxY;y++)
            for(int x=start;x<=glyph.maxX;x++)if(candidate.matches(labels[y*width+x])) {
                if(first<0)first=y;last=y;break;
            }
        return first<0?glyph.minY+glyphHeight*.75f:(first+last)*.5f;
    }

    /**
     * A natural has two offset vertical spines: the left one extends above the right, while the
     * right one extends below the left. Two separated connectors join them. Checking those
     * asymmetric endpoints before flat/sharp classification keeps a natural from looking like a
     * flat's lower bowl or a sharp with unusually thin crossbars.
     */
    private static boolean isNaturalGlyph(byte[] labels, int width, int height,
                                          AccidentalCandidate candidate, float gap) {
        Component glyph = candidate.component;
        int glyphWidth = glyph.maxX - glyph.minX + 1;
        int glyphHeight = glyph.maxY - glyph.minY + 1;
        if (glyphHeight < gap * 1.40f || glyphHeight > gap * 3.70f
                || glyphWidth < gap * .48f || glyphWidth > gap * 1.55f
                || glyphHeight < glyphWidth * 1.45f
                || glyphHeight > glyphWidth * 4.50f
                || glyph.area < gap * gap * .30f || glyph.area > gap * gap * 2.30f)
            return false;

        int[] columns = new int[glyphWidth];
        int[] rowMin = new int[glyphHeight];
        int[] rowMax = new int[glyphHeight];
        java.util.Arrays.fill(rowMin, glyphWidth);
        java.util.Arrays.fill(rowMax, -1);
        for (int y = Math.max(0, glyph.minY); y <= Math.min(height - 1, glyph.maxY); y++)
            for (int x = Math.max(0, glyph.minX); x <= Math.min(width - 1, glyph.maxX); x++)
                if (candidate.matches(labels[y * width + x])) {
                    int localX = x - glyph.minX, localY = y - glyph.minY;
                    columns[localX]++;
                    rowMin[localY] = Math.min(rowMin[localY], localX);
                    rowMax[localY] = Math.max(rowMax[localY], localX);
                }

        int leftSpine = 0;
        int rightSpine = Math.max(0, glyphWidth / 2);
        for (int column = 0; column < Math.max(1, glyphWidth / 2); column++)
            if (columns[column] > columns[leftSpine]) leftSpine = column;
        for (int column = Math.max(0, glyphWidth / 2); column < glyphWidth; column++)
            if (columns[column] > columns[rightSpine]) rightSpine = column;
        if (rightSpine - leftSpine < glyphWidth * .28f
                || columns[leftSpine] < glyphHeight * .38f
                || columns[rightSpine] < glyphHeight * .38f) return false;

        int radius = Math.max(0, Math.round(glyphWidth * .09f));
        int leftTop = glyphHeight, leftBottom = -1, rightTop = glyphHeight, rightBottom = -1;
        for (int row = 0; row < glyphHeight; row++) {
            for (int column = Math.max(0, leftSpine - radius);
                 column <= Math.min(glyphWidth - 1, leftSpine + radius); column++)
                if (candidate.matches(labels[(glyph.minY + row) * width + glyph.minX + column])) {
                    leftTop = Math.min(leftTop, row); leftBottom = Math.max(leftBottom, row);
                }
            for (int column = Math.max(0, rightSpine - radius);
                 column <= Math.min(glyphWidth - 1, rightSpine + radius); column++)
                if (candidate.matches(labels[(glyph.minY + row) * width + glyph.minX + column])) {
                    rightTop = Math.min(rightTop, row); rightBottom = Math.max(rightBottom, row);
                }
        }
        int endpointOffset = Math.max(1, Math.round(glyphHeight * .05f));
        if (rightTop - leftTop < endpointOffset
                || rightBottom - leftBottom < endpointOffset) return false;

        int firstConnector = -1, lastConnector = -1;
        int connectorSpan = Math.max(2, Math.round((rightSpine - leftSpine) * .75f));
        for (int row = 0; row < glyphHeight; row++)
            if (rowMax[row] >= rowMin[row] && rowMax[row] - rowMin[row] + 1 >= connectorSpan) {
                if (firstConnector < 0) firstConnector = row;
                lastConnector = row;
            }
        return firstConnector >= 0
                && lastConnector - firstConnector >= glyphHeight * .16f;
    }

    /** A sharp has two full-height vertical spines crossed by two separated wide strokes. */
    private static boolean isSharpGlyph(byte[] labels,int width,int height,
                                        AccidentalCandidate candidate,float gap) {
        return Float.isFinite(sharpPitchCenter(labels,width,height,candidate,gap));
    }

    private static float sharpPitchCenter(byte[] labels, int width, int height,
                                        AccidentalCandidate candidate, float gap) {
        Component glyph = candidate.component;
        int glyphWidth = glyph.maxX - glyph.minX + 1;
        int glyphHeight = glyph.maxY - glyph.minY + 1;
        if (glyphHeight < gap * 1.55f || glyphHeight > gap * 3.65f
                || glyphWidth < gap * .65f || glyphWidth > gap * 1.80f
                || glyphHeight < glyphWidth * 1.45f
                || glyphHeight > glyphWidth * 4.50f
                || glyph.area < gap * gap * .42f || glyph.area > gap * gap * 2.45f)
            return Float.NaN;

        int[] columns = new int[glyphWidth];
        int[] rows = new int[glyphHeight];
        for (int y = Math.max(0, glyph.minY); y <= Math.min(height - 1, glyph.maxY); y++)
            for (int x = Math.max(0, glyph.minX); x <= Math.min(width - 1, glyph.maxX); x++)
                if (candidate.matches(labels[y * width + x])) {
                    columns[x - glyph.minX]++;
                    rows[y - glyph.minY]++;
                }

        int leftSpine = 0;
        int rightSpine = Math.max(0, glyphWidth / 2);
        for (int column = 0; column < Math.max(1, glyphWidth / 2); column++)
            if (columns[column] > columns[leftSpine]) leftSpine = column;
        for (int column = Math.max(0, glyphWidth / 2); column < glyphWidth; column++)
            if (columns[column] > columns[rightSpine]) rightSpine = column;
        int coreLeft=0,coreRight=glyphWidth-1;
        while(coreLeft<coreRight&&columns[coreLeft]<glyphHeight*.30f)coreLeft++;
        while(coreRight>coreLeft&&columns[coreRight]<glyphHeight*.30f)coreRight--;
        if (rightSpine - leftSpine < (coreRight-coreLeft+1) * .28f
                || columns[leftSpine] < glyphHeight * .46f
                || columns[rightSpine] < glyphHeight * .46f) return Float.NaN;

        // Measure crossbars against the two spines, not stray ink at the glyph edge.
        int wideThreshold = Math.max(2, Math.round((rightSpine-leftSpine+1) * 1.15f));
        List<int[]> crossbars=new ArrayList<>();
        int terminalMargin=Math.max(2,Math.round(glyphHeight*.10f));
        for (int row = terminalMargin; row < rows.length-terminalMargin;) {
            if(rows[row]<wideThreshold){row++;continue;}
            int first=row;
            while(row<rows.length-terminalMargin&&rows[row]>=wideThreshold)row++;
            // A painted staff stripe at a spine tip is not a sharp crossbar.
            if(row-first<Math.max(2,Math.round(gap*.12f)))continue;
            crossbars.add(new int[]{first,row-1});
        }
        float bestCenter=Float.NaN;int bestSupport=-1;
        for(int i=0;i<crossbars.size();i++)pair:for(int j=i+1;j<crossbars.size();j++) {
        int[] firstBand=crossbars.get(i),lastBand=crossbars.get(j);
        int firstCrossbar=firstBand[0],lastCrossbar=lastBand[1];
        if(lastCrossbar-firstCrossbar<glyphHeight*.18f)continue;
        // Both sharp spines protrude through both crossbars. A flat's bowl can
        // supply a long second column, but cannot supply its upper extension.
        int radius = Math.max(1, Math.round(glyphWidth * .09f));
        int required = Math.max(1, Math.round(glyphHeight * .05f));
        int firstTop = -1, firstBottom = -1;
        for (int spine : new int[]{leftSpine, rightSpine}) {
            int above = 0, below = 0, top = -1, bottom = -1;
            for (int row = 0; row < glyphHeight; row++) {
                boolean ink = false;
                for (int col = Math.max(0, spine - radius); col <= Math.min(glyphWidth - 1, spine + radius); col++)
                    if (candidate.matches(labels[(glyph.minY + row) * width + glyph.minX + col])) ink = true;
                if (ink) { if (top < 0) top = row; bottom = row; }
                if (ink && row < firstCrossbar) above++;
                if (ink && row > lastCrossbar) below++;
            }
            if (above < required || below < required) continue pair;
            if (firstTop < 0) { firstTop = top; firstBottom = bottom; }
            else if (Math.abs(top - firstTop) > glyphHeight * .22f
                    || Math.abs(bottom - firstBottom) > glyphHeight * .22f) continue pair;
        }
        // A hairpin or staff stripe can add a third short band near a tip.
        // Prefer the two substantial crossbars with support on both spines.
        int firstSize=firstBand[1]-firstBand[0]+1,lastSize=lastBand[1]-lastBand[0]+1;
        int support=Math.min(firstSize,lastSize)*100+firstSize+lastSize;
        if(support>bestSupport){bestSupport=support;bestCenter=glyph.minY+(firstCrossbar+lastCrossbar)*.5f;}
        }
        return bestCenter;
    }

    /** Uses the staff line beside the note instead of the page-wide average on skewed scans. */
    private static float[] localStaffPitch(byte[] labels, byte[] gray, int width, int height, Staff staff,
                                         Component head) {
        float gap=staff.pitchGap, referenceBottom=staff.pitchBottom+staff.pitchSlope*(head.centerX-width*.5f);
        if(staff.pitchTrack!=null){float[] local=staff.pitchTrack.at(head.centerX);referenceBottom=local[0];gap=local[1];}
        boolean shaded=StaffPitchTrack.needsContrast(gray,width,height,head.centerX,referenceBottom,gap);
        float[] complete=shaded?StaffPitchTrack.localRules(labels,gray,width,height,head.centerX,head.minX,head.maxX,referenceBottom,gap,staff.pitchTrack!=null):null;
        if(complete!=null)return complete;
        int radius = Math.max(4, Math.round(gap * 3.5f));
        int left = Math.max(0, Math.round(head.centerX) - radius);
        int right = Math.min(width - 1, Math.round(head.centerX) + radius);
        int top = Math.max(0, Math.round(referenceBottom - gap * .58f));
        int bottom = Math.min(height - 1, Math.round(referenceBottom + gap * .58f));
        int exclusion = Math.max(1, Math.round(gap * .45f));
        if(gray!=null) {
            // A beam can merge with one rule and leave another thin edge half a
            // space away. Require agreement from several of the five printed
            // rules before using that edge as a local pitch reference.
            // If dark ink is too sparse, require four matching light-ink rules.
            for(float support:new float[]{.70f,.40f}) {
                List<Float> offsets = new ArrayList<>();
                List<float[]> rules=new ArrayList<>();
                for (int line=0;line<5;line++) {
                    float reference=referenceBottom-line*gap;
                    int first=Math.max(0,Math.round(reference-gap*.58f));
                    int last=Math.min(height-1,Math.round(reference+gap*.58f));
                    float closest=Float.NaN,distance=Float.MAX_VALUE;int start=-1;
                    for(int y=first;y<=last+1;y++) {
                        int dark=0,samples=0;
                        if(y<=last)for(int x=left;x<=right;x++) {
                            if(x>=head.minX-exclusion&&x<=head.maxX+exclusion)continue;
                            samples++;int ink=gray[y*width+x]&255,flank=Math.max(2,Math.round(gap*.32f));
                            if(ink<=(support<.7f?205:165)&&(!shaded||(y>=flank&&y+flank<height
                                    &&(gray[(y-flank)*width+x]&255)>=ink+12
                                    &&(gray[(y+flank)*width+x]&255)>=ink+12)))dark++;
                        }
                        boolean rule=samples>=8&&dark>=samples*support;
                        if(rule&&start<0)start=y;
                        if(!rule&&start>=0) {
                            float offset=(start+y-1)*.5f-reference;
                            if(y-start<=Math.max(3,gap*.38f)&&Math.abs(offset)<distance) {
                                closest=offset;distance=Math.abs(offset);
                            }
                            start=-1;
                        }
                    }
                    if(Float.isFinite(closest)){offsets.add(closest);rules.add(new float[]{line,reference+closest});}
                }
                // Derive spacing locally as well as the bottom rule. Semantic
                // stripes can contract on a skewed scan; using that contracted gap
                // still moves ledger pitches even after the bottom rule is corrected.
                if(rules.size()>=4) {
                    List<Float> slopes=new ArrayList<>();
                    for(float[] a:rules)for(float[] b:rules)if(b[0]>a[0])
                        slopes.add((a[1]-b[1])/(b[0]-a[0]));
                    slopes.sort(Float::compare);float localGap=slopes.get(slopes.size()/2);
                    List<Float> bottoms=new ArrayList<>();
                    for(float[] rule:rules)bottoms.add(rule[1]+rule[0]*localGap);
                    bottoms.sort(Float::compare);float localBottom=bottoms.get(bottoms.size()/2);
                    int consistent=0;for(float value:bottoms)if(Math.abs(value-localBottom)<=gap*.12f)consistent++;
                    if(consistent>=4&&localGap>=gap*.88f&&localGap<=gap*1.12f)
                        return new float[]{localBottom,localGap};
                }
                List<Float> agreed=List.of();float bestDistance=Float.MAX_VALUE;
                for(float candidate:offsets) {
                    List<Float> cluster=new ArrayList<>();
                    for(float offset:offsets)if(Math.abs(offset-candidate)<=gap*.18f)cluster.add(offset);
                    float distance=Math.abs(candidate);
                    if(cluster.size()>agreed.size()||cluster.size()==agreed.size()&&distance<bestDistance) {
                        agreed=cluster;bestDistance=distance;
                    }
                }
                if(agreed.size()>=(support<.7f?4:3)) {
                    agreed.sort(Float::compare);
                    return new float[]{referenceBottom+agreed.get(agreed.size()/2),gap};
                }
            }
            // Faded rules may not support a local correction. A single beam
            // edge or semantic smear is weaker evidence than the page's five-
            // rule reference, especially for ledger notes. Keep that reference.
            return new float[]{referenceBottom,gap};
        }
        long weightedY = 0;
        int pixels = 0;
        for (int x = left; x <= right; x++) {
            if (x >= head.minX - exclusion && x <= head.maxX + exclusion) continue;
            for (int y = top; y <= bottom; y++) {
                if (labels[y * width + x] != OmrMeasurePostProcessor.STAFF) continue;
                weightedY += y;
                pixels++;
            }
        }
        return new float[]{pixels >= Math.max(4, radius / 3) ? weightedY / (float) pixels : referenceBottom,gap};
    }

    /** Augmentation dots are small, aligned components to the right of a real notehead. */
    /** The round bulbs of a recognized rest belong to that rest, even if a staff
     * crossing separates their darkest pixels into a dot-shaped island. */
    private static int dotsOutsideRests(List<Component> candidates, DetectedNote note,
            List<ScoreRestEvent> rests, List<MeasureRegion> measures, byte[] gray, int width, int height,
            List<Component> accidentalInk) {
        if(note.event.augmentationDots()==0 || rests.isEmpty())return note.event.augmentationDots();
        List<Component> excluded=new ArrayList<>(accidentalInk);
        for(ScoreRestEvent rest:rests)if(rest.measureIndex()==note.event.measureIndex()
                &&rest.staffIndex()==note.event.staffIndex()&&rest.staffCount()==note.event.staffCount()) {
            MeasureRegion bar=measures.get(rest.measureIndex());
            float x=(bar.left()+rest.positionInMeasure()*(bar.right()-bar.left()))*width;
            float y=rest.pageY()*height,half=rest.pageHeight()*height*.5f;
            excluded.add(new Component(1,Math.round(x-note.staffGap*.65f),Math.round(x+note.staffGap*.65f),
                    Math.round(y-half),Math.round(y+half),x,y));
        }
        return excluded.isEmpty()?note.event.augmentationDots():countAugmentationDots(candidates,note.head,
                note.staffGap,gray,width,height,note.event.unbeamedDurationBeats()>=ScoreNoteEvent.DURATION_HALF,excluded);
    }

    private static int countAugmentationDots(List<Component> candidates, Component head, float gap,
                                             byte[] gray, int width, int height,boolean hollowHead) {
        return countAugmentationDots(candidates, head, gap, gray, width, height, hollowHead, List.of());
    }

    private static int countAugmentationDots(List<Component> candidates, Component head, float gap,
                                             byte[] gray, int width, int height,boolean hollowHead, List<Component> excluded) {
        // Semantic boundaries can cut a tiny round island out of a slur, stem
        // or ledger line. When the page is available, require an isolated raw
        // ink component; the model's artificial boundary is not a printed dot.
        List<Component> combined = gray != null && gray.length == width * height
                ? findDarkDotComponents(gray, width, height, head, gap)
                : new ArrayList<>(candidates);
        // Antialiasing can join a round dot to a nearby tie. Its dark core remains
        // separate; retain the same bounds, shape and engraving-slot checks below.
        if (gray != null && gray.length == width * height)
            for (Component core : findDarkDotComponents(gray, width, height, head, gap, 70))
                if (core.area >= gap*gap*.06f && core.maxX-core.minX+1 >= gap*.22f
                        && core.maxY-core.minY+1 >= gap*.22f) combined.add(core);
        List<Component> aligned = new ArrayList<>();
        for (Component dot : combined) {
            if (excluded.stream().anyMatch(c->dot.centerX>=c.minX&&dot.centerX<=c.maxX
                    &&dot.centerY>=c.minY&&dot.centerY<=c.maxY)) continue;
            float dotWidth = dot.maxX - dot.minX + 1f;
            float dotHeight = dot.maxY - dot.minY + 1f;
            float horizontal = dot.centerX - head.maxX;
            if (horizontal < gap * .12f || horizontal > gap * 2.2f) continue;
            // Raw antialiasing often leaves a 2-4 px island just beyond the semantic oval.
            // A printed augmentation dot has its own engraving slot at least one staff gap
            // from the head centre; the edge island is still part of the notehead.
            if (dot.centerX - head.centerX < gap) continue;
            // Augmentation dots sit beside the head (with at most the usual line-to-space
            // engraving offset). A detached bowing/staccato mark near the next note is not a dot.
            if (Math.abs(dot.centerY - head.centerY) > gap * .65f) continue;
            if (dotWidth < Math.max(1f, gap * .10f) || dotHeight < Math.max(1f, gap * .10f)
                    || dotWidth > gap * .68f || dotHeight > gap * .68f
                    || dot.area < Math.max(1, Math.round(gap * gap * .018f))
                    // Run's dotted half has a round 8x8, 52-pixel dot at a 13.75-pixel staff gap.
                    // Allow that slightly heavier ink only beside a verified hollow head.
                    || dot.area > gap * gap * (hollowHead?.34f:.26f)) continue;
            float dotFill = dot.area / Math.max(1f, dotWidth * dotHeight);
            if (Math.max(dotWidth, dotHeight) / Math.max(1f, Math.min(dotWidth, dotHeight)) > 1.5f
                    || dotFill < .44f) continue;
            boolean duplicate = false;
            for (Component existing : aligned) if (Math.abs(existing.centerX - dot.centerX) <= gap * .28f
                    && Math.abs(existing.centerY - dot.centerY) <= gap * .28f) { duplicate = true; break; }
            if (!duplicate) aligned.add(dot);
        }
        aligned.sort(Comparator.comparingDouble(Component::centerX));
        if (aligned.isEmpty()) return 0;
        Component first = aligned.get(0);
        if (first.centerX - head.maxX > gap * 1.55f) return 0;
        if (aligned.size() == 1) return 1;
        Component second = aligned.get(1);
        float spacing = second.centerX - first.centerX;
        return spacing >= gap * .18f && spacing <= gap * 1.45f
                && Math.abs(second.centerY - first.centerY) <= gap * .40f ? 2 : 1;
    }

    private static List<Component> findDarkDotComponents(byte[] gray, int width, int height,
                                                          Component head, float gap) {
        return findDarkDotComponents(gray, width, height, head, gap, 135);
    }

    private static List<Component> findDarkDotComponents(byte[] gray, int width, int height,
                                                          Component head, float gap, int threshold) {
        int left = Math.max(0, Math.round(head.maxX + gap * .08f));
        int right = Math.min(width - 1, Math.round(head.maxX + gap * 2.3f));
        // Dots beside line notes sit in the next space (half a staff gap away). Include the
        // entire dot there; clipping its edge made the bounded-component guard discard it.
        int top = Math.max(0, Math.round(head.centerY - gap * .92f));
        int bottom = Math.min(height - 1, Math.round(head.centerY + gap * .92f));
        int localWidth = right - left + 1, localHeight = bottom - top + 1;
        if (localWidth <= 0 || localHeight <= 0) return List.of();
        // A real dot can touch a thin staff rule after downsampling. Remove only
        // rows supported across the entire search width, never a rounded local mark.
        boolean[] ruleRows = new boolean[localHeight];
        for (int y = 0; y < localHeight;) {
            int start = y;
            while (y < localHeight) {
                int ink = 0;
                for (int x = left; x <= right; x++)
                    if ((gray[(top + y) * width + x] & 255) <= threshold) ink++;
                if (ink < localWidth * .90f) break;
                y++;
            }
            if (threshold <= 70 && y > start && y - start <= Math.max(2, Math.round(gap * .23f)))
                java.util.Arrays.fill(ruleRows, start, y, true);
            if (y == start) y++;
        }
        boolean[] visited = new boolean[localWidth * localHeight];
        int[] stack = new int[visited.length];
        List<Component> result = new ArrayList<>();
        for (int localOrigin = 0; localOrigin < visited.length; localOrigin++) {
            int originX = localOrigin % localWidth, originY = localOrigin / localWidth;
            if (visited[localOrigin] || ruleRows[originY] || (gray[(top + originY) * width + left + originX] & 0xff) > threshold) continue;
            int stackSize = 0; stack[stackSize++] = localOrigin; visited[localOrigin] = true;
            int area = 0, minX = right, maxX = left, minY = bottom, maxY = top;
            long sumX = 0, sumY = 0;
            while (stackSize > 0) {
                int current = stack[--stackSize]; int lx = current % localWidth, ly = current / localWidth;
                int x = left + lx, y = top + ly; area++; sumX += x; sumY += y;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = lx + dx, ny = ly + dy;
                    if ((dx == 0 && dy == 0) || nx < 0 || nx >= localWidth || ny < 0 || ny >= localHeight) continue;
                    int next = ny * localWidth + nx;
                    if (!visited[next] && !ruleRows[ny] && (gray[(top + ny) * width + left + nx] & 0xff) <= threshold) {
                        visited[next] = true; stack[stackSize++] = next;
                    }
                }
            }
            // Removing a rule can expose the tiny end of a curved flag. A newly
            // separated mark needs a substantial round core to count as a dot.
            boolean ruleCut = minY > top && ruleRows[minY-top-1]
                    || maxY < bottom && ruleRows[maxY-top+1];
            if (ruleCut && area < gap*gap*.06f) continue;
            // A component cut by the search window is not an isolated dot. In particular the
            // protruding end of a whole note's ledger line used to become a false dot here.
            if (minX > left && maxX < right && minY > top && maxY < bottom)
                result.add(new Component(area, minX, maxX, minY, maxY,
                        sumX / (float) area, sumY / (float) area));
        }
        return result;
    }

    /** Counts horizontal beam bands at the far end of the stem: 1=eighth through 3=32nd. */
    private static int detectBeamCount(byte[] labels, byte[] gray, int width, int height,
                                       Component head, Staff staff) {
        float gap=staff.gap;
        // A stem attaches to this oval's edge. A wider window can borrow the preceding
        // triplet's stem and assign its beams to the following ordinary quarter note.
        boolean smallHead = head.maxX - head.minX + 1 < gap * .95f
                || head.maxY - head.minY + 1 < gap * .70f;
        float stemSearchPadding = smallHead ? .8f : .28f;
        int searchLeft = Math.max(0, Math.round(head.minX - gap * stemSearchPadding));
        int searchRight = Math.min(width - 1, Math.round(head.maxX + gap * stemSearchPadding));
        int aboveTop = Math.max(0, Math.round(head.centerY - gap * 4.8f));
        int aboveBottom = Math.max(0, Math.round(head.centerY - gap * .15f));
        int belowTop = Math.min(height - 1, Math.round(head.centerY + gap * .15f));
        int belowBottom = Math.min(height - 1, Math.round(head.centerY + gap * 4.8f));
        int bestX = Math.round(head.centerX), bestAbove = 0, bestBelow = 0;
        for (int x = searchLeft; x <= searchRight; x++) {
            int above = countVertical(labels, width, x, aboveTop, aboveBottom);
            int below = countVertical(labels, width, x, belowTop, belowBottom);
            if (Math.max(above, below) > Math.max(bestAbove, bestBelow)) {
                bestX = x; bestAbove = above; bestBelow = below;
            }
        }
        boolean upward = bestAbove >= bestBelow;
        if (Math.max(bestAbove, bestBelow) < Math.max(3, Math.round(gap * .75f))) return 0;
        int stemEnd = findStemEnd(labels, width, bestX, upward,
                upward ? aboveTop : belowTop, upward ? aboveBottom : belowBottom);
        // Trace the attached ink, not a fixed-height semantic window. In a wide chord the
        // upper head lies inside that window and used to masquerade as the lower head's beam.
        int[] attached = attachedRawStem(gray, width, height, head, gap);
        if (attached != null) {
            bestX = attached[0]; stemEnd = attached[1]; upward = attached[2] < 0;
        }
        // Secondary beams sit just inside the stem endpoint. Searching symmetrically toward
        // the notehead reaches ordinary staff lines and makes every note look beamed.
        int outside = Math.max(2, Math.round(gap * .50f));
        int inside = Math.max(4, Math.round(gap * 1.85f));
        int top = Math.max(0, stemEnd - (upward ? outside : inside));
        int bottom = Math.min(height - 1, stemEnd + (upward ? inside : outside));
        int horizontalLeft = Math.max(0, Math.round(bestX - gap * 4.2f));
        int horizontalRight = Math.min(width - 1, Math.round(bestX + gap * 4.2f));
        // A beam or flag must extend far enough away from its stem to establish horizontal
        // topology. Short dark corners left where one thick beam crosses a semantic staff row
        // otherwise look like an extra beam after that staff row is suppressed.
        int minimumHorizontal = Math.max(4, Math.round(gap * .85f));
        // the model often leaves a narrow background seam where a beam meets its stem. Allow that
        // local seam, but stay well below the roughly 3-4 staff-gap distance to the next stem.
        int stemTolerance = Math.max(2, Math.round(gap * 1.10f));
        int allowedRunGap = Math.max(1, Math.round(gap * .22f));
        int beams = 0; boolean inBand = false;
        for (int y = top; y <= bottom; y++) {
            // A beam belongs to this note only when its horizontal run reaches this note's
            // own stem. Counting all ink in a wide window assigns a neighbour's partial
            // 16th/32nd beams to the wrong note (the Boulevard regression).
            boolean staffLine = gray != null && rowLabelCount(labels, width, y,
                    horizontalLeft, horizontalRight, OmrMeasurePostProcessor.STAFF)
                    >= minimumHorizontal;
            int run = gray == null
                    ? horizontalRunAtStem(labels, width, y, horizontalLeft,
                            horizontalRight, bestX, stemTolerance, allowedRunGap)
                    : darkRunAtStem(gray, width, y, horizontalLeft, horizontalRight,
                            bestX, Math.max(2, Math.round(gap * .28f)), 1);
            // A staff-labelled row can still be white between two genuine beams. Only bridge
            // the semantic occlusion when raw ink really spans it; otherwise end the band.
            if (staffLine && !smallHead && run >= minimumHorizontal) continue;
            if (staffLine) run = 0;
            boolean band = run >= minimumHorizontal;
            if (band && !inBand) { beams++; inBand = true; }
            else if (!band) inBand = false;
        }
        // At this render resolution a thick 32nd beam can be segmented into four dark bands.
        // Four-band optical results are therefore not safe evidence of a true 64th note.
        if (beams == 0 && !smallHead && gray != null && hasCurvedFlag(labels, gray, width, height,
                head, gap, bestX, stemEnd, upward)) return 1;
        // A thin staff line through the white gap can fuse two sloped beams at the stem.
        // Recover only when thick ink bands on BOTH sides independently prove two beams;
        // this cannot borrow a neighbour's one-sided partial beam.
        if (beams == 1 && gray != null && !smallHead) {
            boolean leftProof=false,rightProof=false;
            for(float distance:new float[]{.65f,1f,1.35f}) {
                int offset=Math.max(3,Math.round(gap*distance));
                leftProof |= thickBeamBands(gray,width,height,bestX-offset,top,bottom,gap)==2;
                rightProof |= thickBeamBands(gray,width,height,bestX+offset,top,bottom,gap)==2;
            }
            if(leftProof&&rightProof)beams=2;
        }
        if (attached != null && gray != null) {
            int thick = 0;
            for (float distance : new float[]{-.65f, -.4f, .4f, .65f}) {
                int x = bestX + Math.round(distance * gap);
                int near = Math.max(0, stemEnd - (upward ? Math.round(gap*.2f) : inside));
                int far = Math.min(height-1, stemEnd + (upward ? inside : Math.round(gap*.2f)));
                // Do not count any head on the chord's attached stem as a beam.
                thick = Math.max(thick, thickNonHeadBands(gray, labels, width, height, x, near, far, staff));
            }
            if (thick == 0 && hasCurvedFlag(labels, gray, width, height, head, gap, bestX, stemEnd, upward)) thick = 1;
            return Math.min(3, thick);
        }
        return Math.min(3, beams);
    }

    private static int[] attachedRawStem(byte[] gray,int width,int height,Component head,float gap) {
        if(gray==null)return null;
        int bestLength=0;int[] best=null;
        for(int direction:new int[]{-1,1}) {
            int edge=direction<0?head.maxX:head.minX;
            for(int x=Math.max(1,edge-Math.round(gap*.3f));x<=Math.min(width-2,edge+Math.round(gap*.3f));x++) {
                int blank=0,end=Math.round(head.centerY);
                for(int d=0;d<Math.round(gap*9);d++) {
                    int y=Math.round(head.centerY)+direction*d;
                    if(y<0||y>=height)break;
                    boolean ink=(gray[y*width+x]&255)<170;
                    if(ink){end=y;blank=0;}else if(++blank>Math.max(1,Math.round(gap*.16f)))break;
                }
                int length=Math.abs(end-Math.round(head.centerY));
                if(length>bestLength){bestLength=length;best=new int[]{x,end,direction};}
            }
        }
        return bestLength>=gap*2.3f?best:null;
    }

    private static int thickNonHeadBands(byte[] gray,byte[] labels,int width,int height,int x,int top,int bottom,Staff staff) {
        float gap=staff.gap;
        if(x<1||x>=width-1)return 0;
        int bands=0,strongBands=0,run=0,staffRows=0,staffEdges=0;
        for(int y=top;y<=bottom+1;y++) {
            boolean ink=y<=bottom && (gray[y*width+x-1]&255)<165
                    &&(gray[y*width+x]&255)<165&&(gray[y*width+x+1]&255)<165
                    &&labels[y*width+x]!=OmrMeasurePostProcessor.NOTEHEAD;
            if(ink) {
                run++;
                int span=Math.round(gap*2),leftInk=0,rightInk=0;
                for(int dx=Math.round(gap);dx<Math.round(gap)+span;dx++) {
                    if(x-dx>=0&&(gray[y*width+x-dx]&255)<165)leftInk++;
                    if(x+dx<width&&(gray[y*width+x+dx]&255)<165)rightInk++;
                }
                if(leftInk>=span*.85f&&rightInk>=span*.85f)staffRows++;
                else if(Math.max(leftInk,rightInk)>=span*.8f)staffEdges++;
            } else {
                // Thick antialiased staff lines are not beams. Preserve thicker real beams
                // crossing a staff, where most of the band extends beyond the staff ink.
                float bandCenter=y-(run+1)*.5f;
                boolean onStaff=false;
                for(int line=0;line<5;line++)if(Math.abs(bandCenter-(staff.top+line*gap))<=gap*.3f)onStaff=true;
                // The middle of a long beam also has ink on both sides. Suppress it only
                // where an actual staff line runs; horizontal shape alone loses inner eighths.
                boolean onlyStaff=onStaff&&(staffRows==run||(staffRows>=3&&staffEdges==1&&staffRows+1==run))
                        &&run<=gap*.55f;
                if(run>=Math.max(3,Math.round(gap*.30f))&&!onlyStaff)bands++;
                if(run>=Math.max(3,Math.ceil(gap*.30f))&&!onlyStaff)strongBands++;
                run=0;staffRows=0;staffEdges=0;
            }
        }
        // Preserve a single narrow flag. Adding a second beam needs the full
        // thickness threshold so a thinner slur terminal cannot shorten the note.
        return bands > 1 ? Math.max(1,strongBands) : bands;
    }

    private static int thickBeamBands(byte[] gray,int width,int height,int x,int top,int bottom,float gap) {
        if(x<1||x>=width-1)return 0;
        int bands=0,run=0;
        for(int y=top;y<=Math.min(height,bottom+1);y++) {
            boolean ink=y<=bottom && (gray[y*width+x-1]&255)<165
                    && (gray[y*width+x]&255)<165 && (gray[y*width+x+1]&255)<165;
            if(ink)run++;
            else {if(run>=Math.max(3,Math.round(gap*.30f)))bands++;run=0;}
        }
        return bands;
    }

    /** A single flag is a narrow curved hook, not a horizontal beam-width run. */
    private static boolean hasCurvedFlag(byte[] labels, byte[] gray, int width, int height,
                                          Component head, float gap, int stemX, int end,
                                          boolean upward) {
        int left = Math.max(0, Math.round(stemX + gap * .25f));
        int right = Math.min(width - 1, Math.round(stemX + gap * 1.5f));
        int top = Math.max(0, Math.round(upward ? end : end - gap * 2.3f));
        int bottom = Math.min(height - 1, Math.round(upward ? end + gap * 2.3f : end));
        int rows = 0, nearEnd = 0, bulge = 0, exterior = 0;
        for (int y = top; y <= bottom; y++) {
            if (Math.abs(y - head.centerY) < gap * .65f) continue;
            // Do not count staff/ledger strokes as the hook's side wall.
            if (rowLabelCount(labels, width, y, Math.max(0, stemX - Math.round(gap)),
                    Math.min(width - 1, stemX + Math.round(gap * 3)),
                    OmrMeasurePostProcessor.STAFF) > gap) continue;
            int minX = right + 1, maxX = left - 1;
            for (int x = left; x <= right; x++) {
                if ((gray[y * width + x] & 0xff) > 165
                        || labels[y * width + x] == OmrMeasurePostProcessor.NOTEHEAD) continue;
                // A nearby rest can enter this window beyond the curved hook.
                // Measure the nearest ink run, not the disconnected rest beside it.
                if (maxX >= left && x - maxX > 2) break;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            }
            if (maxX < minX) continue;
            rows++;
            if (Math.abs(y - end) <= gap * 1.15f) nearEnd++;
            if (maxX - stemX >= gap * .55f) bulge++;
            if (maxX >= right - 1) exterior++;
        }
        return rows >= Math.max(4, Math.round(gap * .65f))
                && nearEnd >= Math.max(2, Math.round(gap * .16f))
                && bulge >= Math.max(3, Math.round(gap * .30f))
                && exterior <= Math.max(1, Math.round(rows * .15f));
    }

    private static int rowLabelCount(byte[] labels, int width, int y, int left, int right,
                                     byte wanted) {
        int count = 0;
        for (int x = left; x <= right; x++) if (labels[y * width + x] == wanted) count++;
        return count;
    }

    private static int findStemEnd(byte[] labels, int width, int stemX, boolean upward,
                                   int top, int bottom) {
        int end = upward ? bottom : top;
        for (int y = top; y <= bottom; y++) {
            boolean stem = false;
            for (int dx = -2; dx <= 2; dx++) {
                int x = stemX + dx;
                if (x >= 0 && x < width
                        && labels[y * width + x] == OmrMeasurePostProcessor.STEM_OR_REST) {
                    stem = true;
                    break;
                }
            }
            if (stem && (upward ? y < end : y > end)) end = y;
        }
        return end;
    }

    private static int darkRunAtStem(byte[] gray, int width, int y, int left, int right,
                                     int stemX, int tolerance, int allowedGap) {
        int best = 0;
        int clusterLeft = -1, clusterRight = -1, previousInk = -1;
        for (int x = left; x <= right; x++) {
            if ((gray[y * width + x] & 0xff) > 165) continue;
            if (clusterLeft < 0 || x - previousInk > allowedGap + 1) {
                if (clusterLeft >= 0 && clusterLeft <= stemX + tolerance
                        && clusterRight >= stemX - tolerance)
                    best = Math.max(best, clusterRight - clusterLeft + 1);
                clusterLeft = x;
            }
            clusterRight = x;
            previousInk = x;
        }
        if (clusterLeft >= 0 && clusterLeft <= stemX + tolerance
                && clusterRight >= stemX - tolerance)
            best = Math.max(best, clusterRight - clusterLeft + 1);
        return best;
    }

    private static int horizontalRunAtStem(byte[] labels, int width, int y, int left, int right,
                                            int stemX, int tolerance, int allowedGap) {
        int best = 0;
        int clusterLeft = -1, clusterRight = -1, previousInk = -1;
        for (int x = left; x <= right; x++) {
            if (labels[y * width + x] != OmrMeasurePostProcessor.STEM_OR_REST) continue;
            if (clusterLeft < 0 || x - previousInk > allowedGap + 1) {
                if (clusterLeft >= 0 && clusterLeft <= stemX + tolerance
                        && clusterRight >= stemX - tolerance)
                    best = Math.max(best, clusterRight - clusterLeft + 1);
                clusterLeft = x;
            }
            clusterRight = x;
            previousInk = x;
        }
        if (clusterLeft >= 0 && clusterLeft <= stemX + tolerance
                && clusterRight >= stemX - tolerance)
            best = Math.max(best, clusterRight - clusterLeft + 1);
        return best;
    }

    private static int countVertical(byte[] labels, int width, int x, int top, int bottom) {
        int count = 0;
        for (int y = top; y <= bottom; y++) {
            boolean ink = false;
            for (int dx = -1; dx <= 1; dx++) {
                int check = x + dx;
                if (check >= 0 && check < width
                        && labels[y * width + check] == OmrMeasurePostProcessor.STEM_OR_REST) { ink = true; break; }
            }
            if (ink) count++;
        }
        return count;
    }

    private static int containingMeasure(List<MeasureRegion> measures, float x, float y,
                                         float verticalTolerance) {
        int result = -1;
        float smallest = Float.MAX_VALUE;
        for (int index = 0; index < measures.size(); index++) {
            MeasureRegion measure = measures.get(index);
            if (x < measure.left() - .004f || x > measure.right() + .004f
                    || y < measure.top() - verticalTolerance
                    || y > measure.bottom() + verticalTolerance) continue;
            float area = (measure.right() - measure.left()) * (measure.bottom() - measure.top());
            if (area < smallest) { smallest = area; result = index; }
        }
        return result;
    }

    /**
     * Keeps a ledger-line note with the measure row that owns its staff. A high violin head can
     * sit outside the measure rectangle and inside the preceding row's padded rectangle; choosing
     * by page Y alone then turns an upper-register note into a low note on the row above. The
     * staff center is unambiguous even when the head itself is not.
     */
    private static int containingMeasureForStaff(List<MeasureRegion> measures, float x, float y,
                                                  Staff staff, int pageHeight) {
        float staffCenter = (staff.top + staff.bottom) * .5f / pageHeight;
        float outside = y * pageHeight < staff.top ? staff.top - y * pageHeight
                : y * pageHeight > staff.bottom ? y * pageHeight - staff.bottom : 0f;
        float verticalTolerance = Math.max(staff.gap * 3.5f,
                outside + staff.gap * .75f) / pageHeight;
        int result = -1;
        float smallest = Float.MAX_VALUE;
        for (int index = 0; index < measures.size(); index++) {
            MeasureRegion measure = measures.get(index);
            if (x < measure.left() - .004f || x > measure.right() + .004f
                    || staffCenter < measure.top() - staff.gap / pageHeight
                    || staffCenter > measure.bottom() + staff.gap / pageHeight
                    || y < measure.top() - verticalTolerance
                    || y > measure.bottom() + verticalTolerance) continue;
            float area = (measure.right() - measure.left()) * (measure.bottom() - measure.top());
            if (area < smallest) { smallest = area; result = index; }
        }
        return result;
    }

    /** Printed accidentals carry to the same written pitch until the next barline. */
    private static List<DetectedNote> applyAccidentalState(List<DetectedNote> source) {
        Map<AccidentalStateKey, Integer> measureState = new HashMap<>();
        Map<PitchKey, Integer> lastResolved = new HashMap<>();
        List<DetectedNote> result = new ArrayList<>(source.size());
        for (DetectedNote note : source) {
            ScoreNoteEvent event = note.event;
            AccidentalStateKey stateKey = new AccidentalStateKey(event.measureIndex(),
                    event.staffIndex(), event.staffCount(), event.diatonicPitchIdentity());
            PitchKey pitchKey = new PitchKey(event.staffIndex(), event.staffCount(),
                    event.diatonicPitchIdentity());
            int accidental = event.writtenAccidental();
            if (accidental != ScoreNoteEvent.ACCIDENTAL_FROM_KEY)
                measureState.put(stateKey, accidental);
            else if (measureState.containsKey(stateKey)) accidental = measureState.get(stateKey);
            else if (event.tiedFromPrevious() && lastResolved.containsKey(pitchKey))
                accidental = lastResolved.get(pitchKey);
            lastResolved.put(pitchKey, accidental);
            if (accidental != event.writtenAccidental()) {
                event = new ScoreNoteEvent(event.measureIndex(), event.positionInMeasure(),
                        event.staffStep(), event.staffIndex(), event.staffCount(), event.pageY(),
                        event.tiedFromPrevious(), event.augmentationDots(), event.beamCount(),
                        accidental, event.unbeamedDurationBeats(), event.tupletDivisor(),
                        event.followingRestBeats(),event.articulations(),event.clefBottomDiatonic());
                note = new DetectedNote(event, note.head, note.staffGap);
            }
            result.add(note);
        }
        return result;
    }

    private static List<DetectedNote> removeSplitDuplicates(List<DetectedNote> source) {
        List<DetectedNote> result = new ArrayList<>();
        for (DetectedNote detected : source) {
            ScoreNoteEvent event = detected.event;
            boolean duplicate = false;
            for (int index = result.size() - 1; index >= 0; index--) {
                ScoreNoteEvent previous = result.get(index).event;
                if (previous.measureIndex() != event.measureIndex()
                        || event.positionInMeasure() - previous.positionInMeasure() > .018f) break;
                if (previous.staffIndex() == event.staffIndex()
                        && previous.staffStep() == event.staffStep()) {
                    Component priorHead=result.get(index).head;
                    boolean separateUnison=ScoreNoteTiming.hasIndependentSustain(previous)
                            !=ScoreNoteTiming.hasIndependentSustain(event)
                            && (priorHead.maxX<detected.head.minX||detected.head.maxX<priorHead.minX)
                            && Math.abs(priorHead.centerY-detected.head.centerY)<detected.staffGap*.3f;
                    if(!separateUnison) { duplicate = true; break; }
                }
            }
            if (!duplicate) result.add(detected);
        }
        return result;
    }

    private static List<DetectedNote> markTieContinuations(byte[] labels, byte[] gray,
                                                            int width, int height,
                                                            List<DetectedNote> source) {
        List<DetectedNote> result = new ArrayList<>(source);
        for (int currentIndex = 1; currentIndex < result.size(); currentIndex++) {
            DetectedNote current = result.get(currentIndex);
            int previousIndex = previousSamePitch(result, currentIndex, width);
            if (previousIndex < 0) continue;
            DetectedNote previous = result.get(previousIndex);
            if (!hasTieArc(labels, gray, width, height, previous, current)) continue;
            ScoreNoteEvent event = current.event;
            result.set(currentIndex, new DetectedNote(new ScoreNoteEvent(event.measureIndex(),
                    event.positionInMeasure(), event.staffStep(), event.staffIndex(),
                    event.staffCount(), event.pageY(), true, event.augmentationDots(),
                    event.beamCount(), event.writtenAccidental(), event.unbeamedDurationBeats(),
                    event.tupletDivisor(), event.followingRestBeats(),event.articulations(),event.clefBottomDiatonic()),
                    current.head, current.staffGap));
        }
        return result;
    }

    private static int previousSamePitch(List<DetectedNote> notes, int currentIndex, int width) {
        DetectedNote current = notes.get(currentIndex);
        ScoreNoteEvent previousOnset = null;
        for (int index = currentIndex - 1; index >= 0; index--) {
            DetectedNote previous = notes.get(index);
            if (previous.event.staffIndex() != current.event.staffIndex()
                    || previous.event.staffCount() != current.event.staffCount()) continue;
            if(current.event.measureIndex()-previous.event.measureIndex()>1)break;
            if (sameOnset(previous.event, current.event)) continue;
            if (previousOnset == null) previousOnset = previous.event;
            // A held voice can bridge a barline while another voice keeps moving. Both
            // endpoints must be sustained to search past those intervening attacks.
            else if (!sameOnset(previous.event, previousOnset)
                    &&!(ScoreNoteTiming.hasIndependentSustain(previous.event)
                    &&(previous.event.measureIndex()==current.event.measureIndex()
                    ||ScoreNoteTiming.hasIndependentSustain(current.event)))) continue;
            int measureDistance = current.event.measureIndex() - previous.event.measureIndex();
            if (measureDistance > 1) break;
            // An arc is only a tie when its endpoints are the same written pitch. Slurs can have
            // the identical curved shape, so never use the arc itself to bridge staff positions.
            if (previous.event.diatonicPitchIdentity() != current.event.diatonicPitchIdentity()) continue;
            if (measureDistance == 0
                    && current.event.positionInMeasure() - previous.event.positionInMeasure() > .68f
                    && !ScoreNoteTiming.hasIndependentSustain(previous.event))
                continue;
            // A hollow half/whole note can occupy the entire preceding bar. Its tie starts
            // near that bar's left edge, not necessarily in the final half of the engraving.
            if (measureDistance == 1 && ((previous.event.positionInMeasure() < .42f
                    && !ScoreNoteTiming.hasIndependentSustain(previous.event))
                    || current.event.positionInMeasure() > .58f)) continue;
            float gap = (previous.staffGap + current.staffGap) * .5f;
            int horizontal = current.head.minX - previous.head.maxX;
            if (horizontal < gap * 1.3f || horizontal > width * .34f) continue;
            // Quantization alone can occasionally put two heads near a step boundary in the same
            // bucket. A real repeated pitch remains within less than half a staff-space vertically.
            if (Math.abs(current.head.centerY - previous.head.centerY) > gap * .45f) continue;
            return index;
        }
        return -1;
    }

    private static boolean sameOnset(ScoreNoteEvent first, ScoreNoteEvent second) {
        return first.measureIndex() == second.measureIndex()
                && Math.abs(first.positionInMeasure() - second.positionInMeasure()) <= .018f;
    }

    private static boolean hasTieArc(byte[] labels, byte[] gray, int width, int height,
                                     DetectedNote previous, DetectedNote current) {
        int left = Math.max(0, previous.head.maxX + 1);
        int right = Math.min(width - 1, current.head.minX - 1);
        float gap = Math.max(2f, (previous.staffGap + current.staffGap) * .5f);
        if (right <= left || right - left < gap * 1.3f) return false;
        float centerY = (previous.head.centerY + current.head.centerY) * .5f;
        if (gray != null && gray.length == labels.length) {
            if (hasContinuousTieArc(labels, gray, width, height, left, right, centerY, gap))return true;
            if (!ScoreNoteTiming.hasIndependentSustain(previous.event))return false;
        }
        ArcStats above = arcStats(labels, gray, width, height, left, right,
                Math.round(centerY - gap * 3f), Math.round(centerY - gap * .12f));
        ArcStats below = arcStats(labels, gray, width, height, left, right,
                Math.round(centerY + gap * .12f), Math.round(centerY + gap * 3f));
        return plausibleArc(above, left, right, gap) || plausibleArc(below, left, right, gap);
    }

    /** Follow one returning curve; averaging nearby slurs, stems and ledger lines loses short ties. */
    private static boolean hasContinuousTieArc(byte[] labels, byte[] gray, int width, int height,
            int left, int right, float centerY, float gap) {
        int radius=Math.max(1,Math.round(gap*.09f));
        boolean[] straightRows=new boolean[height];
        for(int y=Math.max(0,Math.round(centerY-gap*3.2f));y<=Math.min(height-1,Math.round(centerY+gap*3.2f));y++) {
            int dark=0;
            for(int x=left;x<=right;x++)if((gray[y*width+x]&255)<=165)dark++;
            straightRows[y]=dark>=(right-left+1)*.85f;
        }
        for(int side:new int[]{-1,1}) for(float offset=.2f;offset<=1.15f;offset+=.15f)
            for(float bend=-.75f;bend<=1.8f;bend+=.1f) {
                if(Math.abs(bend)<.24f || offset+bend<.12f)continue;
                int hits=0,obscured=0;int[] bins=new int[5],coveredBins=new int[5];
                float[] centers=new float[50],supportedCenters=new float[50];
                java.util.Arrays.fill(centers,Float.NaN);
                java.util.Arrays.fill(supportedCenters,Float.NaN);
                for(int sample=0;sample<50;sample++) {
                    float t=(sample+.5f)/50f;
                    int x=Math.round(left+t*(right-left));
                    int y=Math.round(centerY+side*gap*(offset+bend*4*t*(1-t)));
                    boolean ink=false;int obscuredY=-1;
                    for(int search=0;search<=radius*2;search++) {
                        int dy=(search+1)/2*(search%2==0?1:-1);
                        int yy=y+dy;if(yy<0||yy>=height)continue;
                        int at=yy*width+x;
                        if((gray[at]&255)>165||labels[at]==OmrMeasurePostProcessor.NOTEHEAD)continue;
                        if(straightRows[yy]||labels[at]==OmrMeasurePostProcessor.STAFF) {
                            if(obscuredY<0)obscuredY=yy;
                        } else {ink=true;centers[sample]=yy;supportedCenters[sample]=yy;break;}
                    }
                    if(ink){hits++;bins[sample/10]++;coveredBins[sample/10]++;}
                    else if(obscuredY>=0){obscured++;coveredBins[sample/10]++;supportedCenters[sample]=obscuredY;}
                }
                if(hits>=43&&bins[0]>=7&&bins[1]>=7&&bins[2]>=7&&bins[3]>=7&&bins[4]>=7
                        &&arcCurvature(centers,0,0,49)>=Math.max(.8f,gap*.12f))return true;
                // A short returning arc can cross a staff rule at one end. Treat a
                // few such pixels as occluded only when the remaining curve and
                // both endpoints are independently visible away from the rule.
                if(hits>=40&&obscured>0&&obscured<=6&&hits+obscured>=43
                        &&bins[0]>=5&&bins[4]>=5&&coveredBins[0]>=7&&coveredBins[1]>=7
                        &&coveredBins[2]>=7&&coveredBins[3]>=7&&coveredBins[4]>=7
                        &&arcCurvature(supportedCenters,0,0,49)>=Math.max(1.2f,gap*.15f))return true;
            }
        return false;
    }

    private static ArcStats arcStats(byte[] labels, byte[] gray, int width, int height,
                                     int left, int right, int top, int bottom) {
        int safeTop = Math.max(0, top), safeBottom = Math.min(height - 1, bottom);
        int pixels = 0, columns = 0, minX = right + 1, maxX = left - 1;
        int minY = safeBottom + 1, maxY = safeTop - 1;
        boolean rawAvailable = gray != null && gray.length == labels.length;
        boolean[] horizontalRows = new boolean[Math.max(0,safeBottom-safeTop+1)];
        if(rawAvailable)for(int y=safeTop;y<=safeBottom;y++) {
            int dark=0;
            for(int x=left;x<=right;x++)if((gray[y*width+x]&0xff)<=165)dark++;
            horizontalRows[y-safeTop]=dark>=(right-left+1)*.85f;
        }
        float[] columnCenters = new float[right - left + 1];
        java.util.Arrays.fill(columnCenters, Float.NaN);
        for (int x = left; x <= right; x++) {
            int columnPixels = 0, columnY = 0;
            for (int y = safeTop; y <= safeBottom; y++) {
                byte label = labels[y * width + x];
                boolean semanticArc = label == OmrMeasurePostProcessor.SYMBOL
                        || label == OmrMeasurePostProcessor.STEM_OR_REST
                        || label == OmrMeasurePostProcessor.CLEF_OR_KEY;
                boolean rawArc = rawAvailable
                        && (gray[y * width + x] & 0xff) <= 165
                        && !horizontalRows[y-safeTop]
                        && label != OmrMeasurePostProcessor.STAFF
                        && label != OmrMeasurePostProcessor.NOTEHEAD;
                if (rawAvailable ? !rawArc : !semanticArc) continue;
                columnPixels++; columnY += y; pixels++;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
            if (columnPixels > 0) {
                columns++;
                columnCenters[x - left] = columnY / (float) columnPixels;
            }
        }
        float curvature = arcCurvature(columnCenters, left, minX, maxX);
        return new ArcStats(pixels, columns, minX, maxX, minY, maxY, curvature);
    }

    private static float arcCurvature(float[] centers, int left, int minX, int maxX) {
        if (maxX <= minX) return 0f;
        float span = maxX - minX;
        float leftSum = 0f, rightSum = 0f, middleSum = 0f;
        int leftCount = 0, rightCount = 0, middleCount = 0;
        for (int x = minX; x <= maxX; x++) {
            float y = centers[x - left];
            if (!Float.isFinite(y)) continue;
            float position = (x - minX) / span;
            if (position <= .28f) {
                leftSum += y; leftCount++;
            } else if (position >= .72f) {
                rightSum += y; rightCount++;
            } else if (position >= .38f && position <= .62f) {
                middleSum += y; middleCount++;
            }
        }
        if (leftCount == 0 || rightCount == 0 || middleCount == 0) return 0f;
        float middle = middleSum / middleCount;
        float fromLeft = middle - leftSum / leftCount;
        float fromRight = middle - rightSum / rightCount;
        // A real tie bows away from BOTH ends, then returns. Averaging the two endpoints
        // accepted a one-sided step where a sixteenth's extra beam begins halfway across
        // an eighth/sixteenth pair (Feliz). Straight/sloped beams and steps are not arcs.
        if (fromLeft * fromRight <= 0f) return 0f;
        return Math.min(Math.abs(fromLeft), Math.abs(fromRight));
    }

    private static boolean plausibleArc(ArcStats arc, int left, int right, float gap) {
        if (arc.pixels < Math.max(6, Math.round(gap * .8f)) || arc.columns <= 0) return false;
        int span = right - left + 1;
        int inkSpan = arc.maxX - arc.minX + 1;
        int verticalSpan = arc.maxY - arc.minY + 1;
        int endpointTolerance = Math.max(Math.round(gap * 2.2f), Math.round(span * .28f));
        return inkSpan >= Math.max(gap * 1.8f, span * .48f)
                && arc.columns >= Math.max(gap * 1.3f, span * .24f)
                && arc.minX - left <= endpointTolerance && right - arc.maxX <= endpointTolerance
                && verticalSpan >= Math.max(2, Math.round(gap * .12f))
                && verticalSpan <= gap * 3f
                && arc.curvature >= Math.max(.65f, gap * .07f);
    }

    private static float median(float[] values) {
        float[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        return (sorted[1] + sorted[2]) * .5f;
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    private static final class Staff {
        final float top, bottom, gap;
        float pitchBottom, pitchGap, pitchSlope;
        StaffPitchTrack pitchTrack;
        int index, count = 1;
        Staff(float top, float bottom, float gap) {
            this.top = top; this.bottom = bottom; this.gap = gap;
            this.pitchBottom=bottom; this.pitchGap=gap;
        }
    }

    record Analysis(List<ScoreNoteEvent> notes, List<ScoreKeyChange> keyChanges, List<ScoreRestEvent> rests) {
        Analysis(List<ScoreNoteEvent> notes, List<ScoreKeyChange> keyChanges) {
            this(notes, keyChanges, List.of());
        }
        Analysis {
            notes = notes == null ? List.of() : List.copyOf(notes);
            keyChanges = keyChanges == null ? List.of() : List.copyOf(keyChanges);
            rests = rests == null ? List.of() : List.copyOf(rests);
        }
    }
    private record SignatureGlyph(float x, int accidental) { }
    private record Component(int area, int minX, int maxX, int minY, int maxY,
                             float centerX, float centerY) { }
    private record AccidentalCandidate(Component component, byte label) {
        boolean matches(byte value) {
            return label == 0 ? value == OmrMeasurePostProcessor.SYMBOL
                    || value == OmrMeasurePostProcessor.CLEF_OR_KEY : label == value;
        }
    }
    private record AccidentalStateKey(int measureIndex, int staffIndex, int staffCount,
                                      int staffStep) { }
    private record PitchKey(int staffIndex, int staffCount, int staffStep) { }
    private record DetectedNote(ScoreNoteEvent event, Component head, float staffGap) { }
    private record ArcStats(int pixels, int columns, int minX, int maxX, int minY, int maxY,
                            float curvature) { }
}
