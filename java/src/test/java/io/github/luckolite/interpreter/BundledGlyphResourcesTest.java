// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Licensed single-glyph resources on original blank canvases; no score fixtures. */
public class BundledGlyphResourcesTest {
    static byte[] gray(java.awt.image.BufferedImage image){
        int w=image.getWidth(),h=image.getHeight();byte[] result=new byte[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)result[y*w+x]=(byte)((image.getRGB(x,y)>>>16)&255);
        return result;
    }
    @Test public void allTwentySevenBundledTemplatesAreReadable()throws Exception{
        int count=0;
        for(String name:new String[]{"p","pp","ppp","m","mp","mf","f","ff","fff"}){
            assertEquals(64,GlyphResources.image("dynamic_templates/"+name+".png").getHeight());count++;
        }
        for(String name:new String[]{"trill","turn","inverted_turn","inverted_turn_slash","mordent","inverted_mordent","flat","natural","sharp"})
            for(String suffix:new String[]{"","_leland"}){assertEquals(64,GlyphResources.image("ornament_templates/"+name+suffix+".png").getHeight());count++;}
        assertEquals(27,count);
    }
    @Test public void bothFontsMatchEveryOrnamentType()throws Exception{
        String[] names={"trill","turn","inverted_turn","inverted_turn_slash","mordent","inverted_mordent"};
        int[] types={NoteOrnament.TRILL,NoteOrnament.TURN,NoteOrnament.INVERTED_TURN,NoteOrnament.INVERTED_TURN,NoteOrnament.MORDENT,NoteOrnament.INVERTED_MORDENT};
        for(int i=0;i<names.length;i++)for(String suffix:new String[]{"","_leland"}){
            var image=GlyphResources.image("ornament_templates/"+names[i]+suffix+".png");
            var match=GlyphResources.ornaments().match(gray(image),image.getWidth(),new PortableNoteOrnaments.Bounds(0,0,image.getWidth(),image.getHeight()));
            assertEquals(names[i]+suffix,types[i],match.kind());assertTrue(names[i]+suffix,match.accepted());
        }
    }
    @Test public void auxiliaryAccidentalsMatchWithoutChangingMainNotePitch()throws Exception{
        String[] names={"flat","natural","sharp"};
        for(int i=0;i<names.length;i++)for(String suffix:new String[]{"","_leland"}){
            var image=GlyphResources.image("ornament_templates/"+names[i]+suffix+".png");
            var match=GlyphResources.ornaments().accidental(gray(image),image.getWidth(),new PortableNoteOrnaments.Bounds(0,0,image.getWidth(),image.getHeight()));
            assertEquals(i+1,match.kind());assertTrue(match.accepted());
        }
    }
    @Test public void dynamicRasterIsRecognizedWithoutAnOcrWord()throws Exception{
        var image=GlyphResources.image("dynamic_templates/mf.png");
        int w=640,h=420,left=240,top=250;byte[] page=new byte[w*h];Arrays.fill(page,(byte)255);
        byte[] glyph=gray(image);for(int y=0;y<image.getHeight();y++)System.arraycopy(glyph,y*image.getWidth(),page,(top+y)*w+left,image.getWidth());
        var before=page.clone();var staffs=List.of(new PlayingTechniqueDetector.Staff(80,208,32,0,1));
        var found=GlyphResources.dynamics().recognize(page,w,h,staffs,List.of());
        assertTrue(found.toString(),found.stream().anyMatch(word->word.text().equals("mf")));
        assertArrayEquals(before,page);
    }
    @Test public void immutableTemplateBackendsAreReused(){assertSame(GlyphResources.dynamics(),GlyphResources.dynamics());assertSame(GlyphResources.ornaments(),GlyphResources.ornaments());}
    @Test public void packagedNoticesAndManifestRemainAvailable()throws Exception{
        for(String file:new String[]{"BRAVURA-OFL.txt","LELAND-OFL.txt","manifest.json"})
            try(var stream=GlyphResources.class.getResourceAsStream("glyphs/"+file)){assertNotNull(file,stream);assertTrue(stream.readAllBytes().length>100);}
    }
}
