// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Desktop asset adapter; scoring and ownership are shared with Android. */
final class NativeOrnamentGlyphs {
    private final PortableOrnamentGlyphs core=new PortableOrnamentGlyphs();
    NativeOrnamentGlyphs() throws java.io.IOException {
        String[] names={"trill","turn","inverted_turn","inverted_turn_slash","mordent","inverted_mordent"};
        int[] types={NoteOrnament.TRILL,NoteOrnament.TURN,NoteOrnament.INVERTED_TURN,
                NoteOrnament.INVERTED_TURN,NoteOrnament.MORDENT,NoteOrnament.INVERTED_MORDENT};
        for(int i=0;i<names.length;i++)for(String suffix:new String[]{"","_leland"})load(names[i]+suffix,types[i],false);
        for(int i=0;i<3;i++)for(String suffix:new String[]{"","_leland"})load(new String[]{"flat","natural","sharp"}[i]+suffix,i+1,true);
        for(int style:new int[]{Font.ITALIC,Font.BOLD|Font.ITALIC})for(String text:new String[]{"tr","tr."}) {
            BufferedImage image=new BufferedImage(240,150,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,240,150);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.SERIF,style,96));g.setColor(Color.BLACK);g.drawString(text,24,112);g.dispose();
            int left=240,top=150,right=-1,bottom=-1;
            for(int y=0;y<150;y++)for(int x=0;x<240;x++)if(((image.getRGB(x,y)>>>16)&255)<250){
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            if(right>=left)add(image.getSubimage(left,top,right-left+1,bottom-top+1),NoteOrnament.TRILL,false);
        }
    }
    PortableOrnamentGlyphs portable(){return core;}
    private void load(String name,int kind,boolean accidental) throws java.io.IOException {
        BufferedImage image=GlyphResources.image("ornament_templates/"+name+".png");
        add(image,kind,accidental);
    }
    private void add(BufferedImage image,int kind,boolean accidental) {
        int width=image.getWidth(),height=image.getHeight();byte[] gray=new byte[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)gray[y*width+x]=(byte)((image.getRGB(x,y)>>>16)&255);
        core.add(gray,width,height,kind,accidental);
    }
}
