// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Independent literal OCR can corroborate, but never replace, a bounded glyph comparison. */
final class DynamicGlyphEvidence {
    private DynamicGlyphEvidence() { }
    static boolean corroborated(String glyph,float score,float margin,String literal) {
        return (glyph.equals("mf")||glyph.equals("mp"))&&glyph.equals(literal)
                &&score>=.40f&&margin>=.08f;
    }

    /** A detached f/p component must not erase the independently read compound mark. */
    static boolean clippedCompound(String glyph,String literal,float glyphLeft,float glyphRight,
            float wordLeft,float wordRight) {
        if (glyph.isEmpty()||literal==null||!literal.matches("(?:m[fp]|f{2,3}|p{2,3})")
                ||literal.length()<=glyph.length()) return false;
        float width=glyphRight-glyphLeft;
        return width>0&&wordRight-wordLeft>1.25f*width
                &&(literal.endsWith(glyph)&&glyphLeft-wordLeft>.25f*width
                        &&Math.abs(wordRight-glyphRight)<.5f*width
                ||literal.startsWith(glyph)&&wordRight-glyphRight>.25f*width
                        &&Math.abs(wordLeft-glyphLeft)<.5f*width);
    }
}
