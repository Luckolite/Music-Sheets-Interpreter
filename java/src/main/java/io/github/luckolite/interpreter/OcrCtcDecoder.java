// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared greedy CTC decoding for probability output, without musical text corrections. */
public final class OcrCtcDecoder {
    private OcrCtcDecoder(){}
    /** Time spans are model steps, NOT measured character bounding boxes. */
    public record Token(String text,int startStep,int endStep,float confidence){}
    public record Result(String text,List<Token> tokens,float confidence) {
        public Result { tokens=List.copyOf(tokens); }
    }
    public static Result decode(float[][] probabilities,List<String> dictionary,int blankIndex) {
        Objects.requireNonNull(probabilities);dictionary=List.copyOf(dictionary);
        if(dictionary.isEmpty()||blankIndex<0||blankIndex>=dictionary.size())
            throw new IllegalArgumentException("Invalid CTC dictionary/blank index");
        var tokens=new ArrayList<Token>();int previous=blankIndex;float total=0;
        for(int t=0;t<probabilities.length;t++) {
            var step=probabilities[t];
            if(step==null||step.length!=dictionary.size())throw new IllegalArgumentException("CTC vocabulary mismatch");
            int best=0;
            for(int k=0;k<step.length;k++) {
                if(!Float.isFinite(step[k])||step[k]<0||step[k]>1)
                    throw new IllegalArgumentException("Expected finite CTC probabilities");
                if(step[k]>step[best])best=k; // Stable lowest-index tie break on every platform.
            }
            if(best!=blankIndex) {
                if(best!=previous) {
                    tokens.add(new Token(dictionary.get(best),t,t+1,step[best]));total+=step[best];
                } else {
                    var old=tokens.remove(tokens.size()-1);
                    tokens.add(new Token(old.text(),old.startStep(),t+1,old.confidence()));
                }
            }
            previous=best;
        }
        var text=new StringBuilder();for(var token:tokens)text.append(token.text());
        return new Result(text.toString(),tokens,tokens.isEmpty()?0:total/tokens.size());
    }
}
