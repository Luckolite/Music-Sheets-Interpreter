// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** Persistent playing instruction for one printed staff; positions are in engraving space. */
public record ScoreTechniqueChange(int measureIndex,float positionInMeasure,int staffIndex,
                                   int staffCount,int technique) {
    public static final int ARCO=0, PIZZICATO=1;
    public static final int CANTABILE=2, SOSTENUTO=3, MARCATO=4, ORDINARIO=5;
    public ScoreTechniqueChange {
        if(measureIndex<0||!Float.isFinite(positionInMeasure)||positionInMeasure<0||positionInMeasure>1
                ||staffCount<1||staffIndex<0||staffIndex>=staffCount||technique<0||technique>ORDINARIO)
            throw new IllegalArgumentException("Invalid playing-technique change");
    }
}
