// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import java.util.Objects;

/** Stateless, platform-independent decoder for six-class masks and their source grayscale pixels. */
public final class SheetInterpreter {
    private SheetInterpreter() { }
    public record NumberToken(int value, float left, float top, float right, float bottom,
                              float annotationLeft) {
        public NumberToken {
            checkBox(left,top,right,bottom);
            if(!Float.isFinite(annotationLeft)||annotationLeft<0||annotationLeft>1)
                throw new IllegalArgumentException("annotationLeft must be a normalized coordinate");
        }
        MeasureNumberReconciler.NumberToken internal() {
            return new MeasureNumberReconciler.NumberToken(value,left,top,right,bottom,annotationLeft);
        }
    }
    public record Word(String text,float left,float top,float right,float bottom) {
        public Word { Objects.requireNonNull(text); checkBox(left,top,right,bottom); }
        PlayingTechniqueDetector.Word internal() {
            return new PlayingTechniqueDetector.Word(text,left,top,right,bottom);
        }
    }
    /** Optional output from the caller's OCR engine, with normalized 0..1 page boxes.
     * Meter changes are explicit, validated readings at zero-based logical measure indices. */
    public record Annotations(List<NumberToken> measureNumbers, List<NumberToken> tempoNumbers,
                              List<NumberToken> restCounts, List<Word> words,
                              List<ScoreMeterChange> meters) {
        public static final Annotations EMPTY=new Annotations(List.of(),List.of(),List.of(),List.of(),List.of());
        public Annotations {
            measureNumbers=List.copyOf(measureNumbers);tempoNumbers=List.copyOf(tempoNumbers);
            restCounts=List.copyOf(restCounts);words=List.copyOf(words);meters=List.copyOf(meters);
        }
    }
    private static void checkBox(float left,float top,float right,float bottom) {
        if(!Float.isFinite(left)||!Float.isFinite(top)||!Float.isFinite(right)||!Float.isFinite(bottom)
                ||left<0||top<0||right>1||bottom>1||left>=right||top>=bottom)
            throw new IllegalArgumentException("OCR boxes must be ordered normalized page coordinates");
    }
    public static ScorePageInterpretation analyze(byte[] labels,byte[] gray,int width,int height) {
        return analyze(labels,gray,width,height,Annotations.EMPTY);
    }
    public static ScorePageInterpretation analyze(byte[] labels,byte[] gray,int width,int height,
                                                 Annotations annotations) {
        long pixels=(long)width*height;
        if(width<=0||height<=0||pixels>20_000_000||labels==null||gray==null
                ||labels.length!=pixels||gray.length!=pixels)
            throw new IllegalArgumentException("Expected equally sized masks and grayscale, at most 20 million pixels");
        for(byte label:labels)if(label<0||label>5)throw new IllegalArgumentException("Labels must be in 0..5");
        Objects.requireNonNull(annotations);
        var measures=OmrMeasurePostProcessor.process(labels,gray,width,height);
        byte[] prepared=OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,width,height,measures);
        // Removing a header head can move the playable edge and the rest-count OCR crop.
        if(prepared!=labels) {
            measures=OmrMeasurePostProcessor.process(labels,gray,width,height,prepared);
            labels=prepared;
        }
        var numbers=annotations.measureNumbers.stream().map(NumberToken::internal).toList();
        var rests=MultiMeasureRestDetector.detect(labels,gray,width,height,measures,
                annotations.restCounts.stream().map(NumberToken::internal).toList());
        if(!numbers.isEmpty()||!rests.isEmpty())
            measures=MeasureNumberReconciler.reconcile(measures,numbers,rests);
        var score=OmrScoreInterpreter.analyze(labels,gray,width,height,measures);
        var notes=TripletRhythmDetector.apply(score.notes(),measures,gray,width,height);
        var staffs=ScoreDynamicsDetector.alignStaffs(
                OmrScoreInterpreter.techniqueStaffs(labels,gray,width,height,measures),notes,height);
        var words=annotations.words.stream().map(Word::internal).toList();
        for(var meter:annotations.meters)if(meter.measureIndex()>=measures.size())
            throw new IllegalArgumentException("Meter change is outside the detected measure range");
        return new ScorePageInterpretation(measures,notes,
                MeasureNumberReconciler.firstMeasureNumber(measures,numbers),score.keyChanges(),
                TempoChangeDetector.detect(annotations.tempoNumbers.stream().map(NumberToken::internal).toList(),
                        gray,width,height,measures),
                annotations.meters,score.rests(),
                PlayingTechniqueDetector.detect(words,staffs,measures,notes,width,height),
                ScoreDynamicsDetector.detect(words,staffs,measures,notes,gray,width,height));
    }
}
