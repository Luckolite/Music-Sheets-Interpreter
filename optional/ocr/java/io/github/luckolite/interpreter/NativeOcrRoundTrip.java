// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Local synthetic-only service acceptance: identity, concurrency, parity and parent lifetime. */
public final class NativeOcrRoundTrip {
    public static void main(String[] args)throws Exception {
        File models=new File(args[0]);var image=ImageIO.read(new File(args[1]));
        int w=image.getWidth(),h=image.getHeight();int[] pixels=image.getRGB(0,0,w,h,null,0,w);
        String token=UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");
        String source=NativeDecoderBuild.SOURCE_SHA256;
        var command=new ProcessBuilder(args.length>2?args[2]:new File(System.getProperty("java.home"),"bin/java").getPath(),"-cp",
                System.getProperty("java.class.path"),NativeOcrServer.class.getName(),models.getAbsolutePath()).redirectError(ProcessBuilder.Redirect.INHERIT);
        Process child=command.start();var parent=child.getOutputStream();
        try {
            parent.write((token+"\n").getBytes(StandardCharsets.UTF_8));parent.flush();
            var lines=new BufferedReader(new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8));
            if(!lines.readLine().startsWith("Native OCR ready"))throw new AssertionError("Server did not start");
            var log=new Thread(()->{try{while(lines.readLine()!=null){}}catch(IOException ignored){}});log.setDaemon(true);log.start();
            try {NativeOcrWire.exchange("127.0.0.1","0".repeat(64),source,pixels,w,h);throw new AssertionError("Accepted wrong token");}catch(IOException expected){}
            try {NativeOcrWire.exchange("127.0.0.1",token,"0".repeat(64),pixels,w,h);throw new AssertionError("Accepted wrong source");}catch(IOException expected){}
            OcrText direct;
            try(var inference=new OnnxOcrInference(new File(models,"ch_PP-OCRv5_det_mobile.onnx").getPath(),new File(models,"latin_PP-OCRv5_rec_mobile.onnx").getPath())) {
                direct=new PortableOcr(inference).read(pixels,w,h);
            }
            var pool=Executors.newFixedThreadPool(3);
            try {
                var jobs=new ArrayList<Future<OcrText>>();
                for(int i=0;i<3;i++)jobs.add(pool.submit(()->NativeOcrWire.exchange("127.0.0.1",token,source,pixels,w,h)));
                for(var job:jobs)if(!direct.equals(job.get(45,TimeUnit.SECONDS)))throw new AssertionError("OCR service changed evidence");
            }finally{pool.shutdownNow();}
            parent.close();if(!child.waitFor(8,TimeUnit.SECONDS))throw new AssertionError("Worker survived parent EOF");
            if(child.exitValue()!=0)throw new AssertionError("Worker failed shutdown");
            System.out.println("PASS exact OCR evidence, three concurrent clients, auth/source rejection and parent lifetime");
        }finally{parent.close();child.destroyForcibly();}
    }
}
