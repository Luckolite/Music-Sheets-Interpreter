// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Immutable, lazily loaded recognition templates bundled with the standalone JAR. */
final class GlyphResources {
    private GlyphResources() {}
    static BufferedImage image(String relative) throws IOException {
        try(var input=GlyphResources.class.getResourceAsStream("glyphs/"+relative)) {
            if(input==null)throw new IOException("Missing bundled glyph template: "+relative);
            var image=ImageIO.read(input);
            if(image==null)throw new IOException("Invalid bundled glyph template: "+relative);
            return image;
        }
    }
    private static final class DynamicHolder {
        static final NativeDynamicGlyphs INSTANCE=create();
        private static NativeDynamicGlyphs create() {
            try{return new NativeDynamicGlyphs();}
            catch(IOException failure){throw new IllegalStateException("Cannot load dynamic templates",failure);}
        }
    }
    private static final class OrnamentHolder {
        static final NativeOrnamentGlyphs INSTANCE=create();
        private static NativeOrnamentGlyphs create() {
            try{return new NativeOrnamentGlyphs();}
            catch(IOException failure){throw new IllegalStateException("Cannot load ornament templates",failure);}
        }
    }
    static NativeDynamicGlyphs dynamics(){return DynamicHolder.INSTANCE;}
    static PortableOrnamentGlyphs ornaments(){return OrnamentHolder.INSTANCE.portable();}
}
