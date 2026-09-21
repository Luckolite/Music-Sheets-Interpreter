// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;

public final class PortableOcrProbe {
    public static void main(String[] args)throws Exception {
        var image=ImageIO.read(new File(args[2]));
        int w=image.getWidth(),h=image.getHeight();
        int[] pixels=image.getRGB(0,0,w,h,null,0,w);
        try(var inference=new OnnxOcrInference(args[0],args[1])) {
            if(Boolean.getBoolean("ocr.regions")) {
                int dw=Math.max(32,(int)Math.round(w/32.0)*32),dh=Math.max(32,(int)Math.round(h/32.0)*32);
                var map=inference.detect(PortableOcr.normalize(pixels,w,h,0,0,w,h,dw,dh,dw,true),dw,dh);
                for(var box:PortableOcr.regions(map,w,h))System.err.println("REGION "+box);
            }
            var engine=new PortableOcr(inference);
            for(int i=0;i<2;i++) {
                long started=System.nanoTime();var result=engine.read(pixels,w,h);
                if(args.length>3)java.nio.file.Files.writeString(java.nio.file.Path.of(args[3]),result.toString(),StandardCharsets.UTF_8);
                System.out.println("PASS\t"+i+"\t"+(System.nanoTime()-started)/1_000_000);
                for(var block:result.blocks())for(var line:block.lines())for(var word:line.elements()) {
                    if(Boolean.getBoolean("ocr.symbols")) {
                        for(var symbol:word.symbols()) {
                            var box=symbol.box();System.out.println(Base64.getEncoder().encodeToString(symbol.text().getBytes(StandardCharsets.UTF_8))+"\t"+box.left+"\t"+box.top+"\t"+box.right+"\t"+box.bottom);
                        }
                        continue;
                    }
                    var box=word.box();System.out.println(Base64.getEncoder().encodeToString(word.text().getBytes(StandardCharsets.UTF_8))+"\t"+box.left+"\t"+box.top+"\t"+box.right+"\t"+box.bottom);
                }
            }
        }
    }
}
