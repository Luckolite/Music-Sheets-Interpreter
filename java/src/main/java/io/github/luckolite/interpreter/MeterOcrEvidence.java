// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Conservative evidence from OCR of a verified, horizontally reflowed signature crop. */
public final class MeterOcrEvidence {
    private MeterOcrEvidence() {}

    public record Token(String text, int left, int top, int right, int bottom) {}

    /** Match the staff cleaner's ink decision before adding reconstructed black strokes. */
    public static boolean ink(int luminance) { return luminance >= 0 && luminance < 135; }

    public static String horizontalFraction(List<Token> input) {
        var tokens=new ArrayList<Token>();
        for(Token token:input)if(token!=null && token.text()!=null
                && token.right()>token.left() && token.bottom()>token.top()
                && token.text().trim().matches("[0-9/|]+"))tokens.add(token);
        tokens.sort(Comparator.comparingInt(Token::left));
        var choices=new HashSet<String>();
        for(int i=0;i<tokens.size();i++) {
            Token previous=tokens.get(i);String text=previous.text().trim();
            addFraction(choices,text);
            // OCR can split a printed fraction into '3', '/8' or '3', '/', '8'.
            for(int j=i+1;j<Math.min(tokens.size(),i+3);j++) {
                Token next=tokens.get(j);
                int overlap=Math.min(previous.bottom(),next.bottom())-Math.max(previous.top(),next.top());
                int height=Math.min(previous.bottom()-previous.top(),next.bottom()-next.top());
                int gap=next.left()-previous.right();
                if(gap<0 || gap>height*1.5f || overlap<height*.65f)break;
                text+=next.text().trim();addFraction(choices,text);previous=next;
            }
        }
        return choices.size()==1?choices.iterator().next():"";
    }

    private static void addFraction(HashSet<String> choices,String text) {
        // Never guess a slash from a '1', or turn arbitrary letters into digits.
        if(!text.matches("[0-9]{1,2}[/|][0-9]{1,2}"))return;
        String[] parts=text.split("[/|]");
        int numerator=Integer.parseInt(parts[0]),denominator=Integer.parseInt(parts[1]);
        if(numerator>=1 && numerator<=32 && denominator>=1 && denominator<=32
                && (denominator & (denominator-1))==0)choices.add(numerator+"/"+denominator);
    }

    /** Require two agreeing renderings and reject any competing valid reading. */
    public static String consensus(List<String> readings) {
        String result="";int votes=0;
        for(String reading:readings) {
            if(reading==null || reading.isBlank())continue;
            var valid=new HashSet<String>();addFraction(valid,reading);
            if(valid.size()!=1)continue;
            String meter=valid.iterator().next();
            if(!result.isEmpty() && !result.equals(meter))return "";
            result=meter;votes++;
        }
        return votes>=2?result:"";
    }
}
