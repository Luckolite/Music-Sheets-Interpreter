// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class OcrCtcDecoderTest {
    private static final List<String> DICT=List.of("", "0", "o", "p", " ", "é");
    private static float[][] sequence(int... indexes) {
        float[][] result=new float[indexes.length][DICT.size()];
        for(int i=0;i<indexes.length;i++)result[i][indexes[i]]=1;
        return result;
    }
    @Test public void blankSeparatesRepeatedDynamics() {
        var result=OcrCtcDecoder.decode(sequence(3,3,0,3,3),DICT,0);
        assertEquals("pp",result.text());assertEquals(2,result.tokens().size());
        assertEquals(0,result.tokens().get(0).startStep());assertEquals(2,result.tokens().get(0).endStep());
        assertEquals(3,result.tokens().get(1).startStep());assertEquals(5,result.tokens().get(1).endStep());
    }
    @Test public void noGuessedNumericCorrectionsOrTrim() {
        assertEquals(" 0oé ",OcrCtcDecoder.decode(sequence(4,1,2,5,4),DICT,0).text());
    }
    @Test public void allBlankDoesNotInventTextOrConfidence() {
        var result=OcrCtcDecoder.decode(sequence(0,0),DICT,0);
        assertEquals("",result.text());assertTrue(result.tokens().isEmpty());assertEquals(0,result.confidence(),0);
    }
    @Test public void rejectsMismatchedVocabularyAndNonFiniteProbabilities() {
        assertThrows(IllegalArgumentException.class,()->OcrCtcDecoder.decode(new float[][]{{1}},DICT,0));
        var bad=sequence(1);bad[0][0]=Float.NaN;
        assertThrows(IllegalArgumentException.class,()->OcrCtcDecoder.decode(bad,DICT,0));
        assertThrows(IllegalArgumentException.class,()->OcrCtcDecoder.decode(sequence(1),DICT,-1));
    }
}
