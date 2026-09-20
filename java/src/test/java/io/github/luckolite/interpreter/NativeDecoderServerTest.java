// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Original synthetic request: authentication, version mismatch, concurrency and stdin lifetime. */
public final class NativeDecoderServerTest {
    @Test public void servicePreservesParityAndStopsWithParent()throws Exception {
        int port;try(var reserve=new ServerSocket(0)){port=reserve.getLocalPort();}
        String java=System.getProperty("java.home")+"/bin/"+(System.getProperty("os.name").startsWith("Windows")?"java.exe":"java"),secret="1".repeat(64);
        var process=new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),
                NativeDecoderServer.class.getName(),Integer.toString(port)).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            var stdin=new PrintWriter(process.getOutputStream(),true);stdin.println(secret);
            var stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ready=CompletableFuture.supplyAsync(()->{try{return stdout.readLine();}catch(IOException e){throw new UncheckedIOException(e);}}).get(15,TimeUnit.SECONDS);
            if(!ready.equals("READY "+NativeDecoderBuild.SOURCE_SHA256))throw new AssertionError("Not ready");
            byte[] labels=new byte[1024],gray=new byte[1024];Arrays.fill(gray,(byte)255);
            var request=new NativeDecoderWire.Request(labels,gray,32,32,List.of(new MeasureRegion(0,1,0,1)));
            var expected=OmrScoreInterpreter.analyze(labels,gray,32,32,request.measures());
            expectFailure(()->NativeDecoderWire.exchange("127.0.0.1",port,"2".repeat(64),NativeDecoderBuild.SOURCE_SHA256,request));
            expectFailure(()->NativeDecoderWire.exchange("127.0.0.1",port,secret,"0".repeat(64),request));
            var pool=Executors.newFixedThreadPool(3);
            try {
                var jobs=new ArrayList<Future<OmrScoreInterpreter.Analysis>>();
                for(int i=0;i<3;i++)jobs.add(pool.submit(()->NativeDecoderWire.exchange("127.0.0.1",port,secret,NativeDecoderBuild.SOURCE_SHA256,request)));
                for(var job:jobs)if(!expected.equals(job.get(15,TimeUnit.SECONDS)))throw new AssertionError("Changed synthetic output");
            }finally{pool.shutdownNow();}
            var geometryRequest=new NativeDecoderWire.Request(labels,gray,32,32,List.of(),NativeDecoderWire.GEOMETRY);
            var expectedGeometry=NativeDecoderStages.geometry(labels,gray,32,32);
            var actualGeometry=NativeDecoderWire.exchangeGeometry("127.0.0.1",port,secret,NativeDecoderBuild.SOURCE_SHA256,geometryRequest);
            if(!Arrays.equals(expectedGeometry.labels(),actualGeometry.labels())||!expectedGeometry.measures().equals(actualGeometry.measures()))
                throw new AssertionError("Changed geometry output");
            stdin.close();
            if(!process.waitFor(5,TimeUnit.SECONDS)||process.exitValue()!=0)throw new AssertionError("Server outlived Hub stdin");
            expectFailure(()->NativeDecoderWire.exchange("127.0.0.1",port,secret,NativeDecoderBuild.SOURCE_SHA256,request));
            System.out.println("PASS auth, source mismatch, concurrent requests, result parity, parent lifetime, unavailable endpoint");
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private interface Action {void run()throws Exception;}
    private static void expectFailure(Action action)throws Exception {try{action.run();throw new AssertionError("Expected rejection");}catch(IOException expected){}}
}
