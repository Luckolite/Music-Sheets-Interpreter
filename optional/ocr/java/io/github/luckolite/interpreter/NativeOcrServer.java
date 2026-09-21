// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Hub-owned CPU OCR process. Models and dictionary are checksum verified by the binding. */
public final class NativeOcrServer {
    public static void main(String[] args)throws Exception {
        var parent=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        String token=parent.readLine();
        if(token==null||!token.matches("[a-fA-F0-9]{64}"))throw new IOException("Missing worker credential");
        File models=new File(args.length==1?args[0]:"ocr");
        var engines=new ConcurrentLinkedQueue<OnnxOcrInference>();
        var perThread=new ThreadLocal<PortableOcr>();
        var executor=new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(3),task->{
            var thread=new Thread(task,"native-ocr");thread.setDaemon(true);return thread;
        });
        try(var server=new ServerSocket()) {
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),NativeOcrWire.PORT));
            var watcher=new Thread(()->{try{while(parent.readLine()!=null){}}catch(IOException ignored){}
                try{server.close();}catch(IOException ignored){}},"ocr-parent-lifetime");
            watcher.setDaemon(true);watcher.start();
            System.out.println("Native OCR ready source="+NativeDecoderBuild.SOURCE_SHA256);
            while(!server.isClosed()) {
                Socket socket;
                try{socket=server.accept();}catch(SocketException closed){if(server.isClosed())break;throw closed;}
                try {executor.execute(()->{
                    try(socket) {
                        socket.setSoTimeout(30_000);
                        var in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                        var out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        if(!NativeOcrWire.authenticate(in,out,token,NativeDecoderBuild.SOURCE_SHA256))return;
                        var raster=NativeOcrWire.readRaster(in);
                        var engine=perThread.get();
                        if(engine==null) {
                            var inference=new OnnxOcrInference(new File(models,"ch_PP-OCRv5_det_mobile.onnx").getPath(),new File(models,"latin_PP-OCRv5_rec_mobile.onnx").getPath());
                            engines.add(inference);engine=new PortableOcr(inference);perThread.set(engine);
                        }
                        long started=System.nanoTime();
                        NativeOcrWire.writeText(out,engine.read(raster.pixels(),raster.width(),raster.height()));out.flush();
                        System.out.println("Native OCR completed elapsed_ms="+(System.nanoTime()-started)/1_000_000);
                        // Flush and close orderly: waiting for a NAT-delayed client FIN would
                        // occupy all three workers between otherwise fast crop requests.
                    }catch(Exception failure){System.err.println("Native OCR request failed: "+failure.getClass().getSimpleName());}
                });}catch(RejectedExecutionException busy){socket.close();}
            }
        }finally {
            executor.shutdownNow();
            if(executor.awaitTermination(2,TimeUnit.SECONDS))for(var engine:engines)try{engine.close();}catch(Exception ignored){}
        }
    }
}
