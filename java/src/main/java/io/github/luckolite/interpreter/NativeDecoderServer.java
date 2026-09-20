// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0

package io.github.luckolite.interpreter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Loopback-only, authenticated and bounded service; stdin closure ends its lifetime. */
public final class NativeDecoderServer {
    public static void main(String[] args)throws Exception {
        int port=args.length==0?NativeDecoderWire.PORT:Integer.parseInt(args[0]);
        var control=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        String secret=control.readLine();
        if(secret==null||!secret.matches("[a-fA-F0-9]{64}"))throw new IOException("Missing decoder credentials");
        byte[] expected=secret.getBytes(StandardCharsets.UTF_8);
        var server=new ServerSocket();server.setReuseAddress(false);server.bind(new InetSocketAddress("127.0.0.1",port),8);
        // Absorb a new stage while completed requests are still closing their sockets.
        // Concurrency remains three, and at most three further requests may wait.
        var pool=new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(3),r->{var t=new Thread(r,"native-score-decoder");t.setDaemon(true);return t;});
        var watchdog=new Thread(()->{try {while(control.readLine()!=null){} }catch(IOException ignored){}finally{try{server.close();}catch(IOException ignored){}}},"hub-lifetime");
        watchdog.setDaemon(true);watchdog.start();
        System.out.println("READY "+NativeDecoderBuild.SOURCE_SHA256);System.out.flush();
        try {
            while(!server.isClosed()) {
                Socket socket=server.accept();socket.setSoTimeout(15_000);socket.setTcpNoDelay(true);
                try {pool.execute(()->serve(socket,expected));}
                catch(RejectedExecutionException busy){socket.close();}
            }
        }catch(SocketException ended){if(!server.isClosed())throw ended;}
        finally{pool.shutdownNow();server.close();}
    }
    private static void serve(Socket socket,byte[] expected) {
        try(socket;var in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));var out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            if(in.readInt()!=NativeDecoderWire.MAGIC)return;
            if(!MessageDigest.isEqual(expected,in.readUTF().getBytes(StandardCharsets.UTF_8)))return;
            if(!NativeDecoderBuild.SOURCE_SHA256.equals(in.readUTF())){out.writeByte(1);out.flush();return;}
            out.writeByte(0);out.flush();
            NativeDecoderWire.Request request;
            try(var body=NativeDecoderWire.packet(in)){request=NativeDecoderWire.readRequest(body);}
            long started=System.nanoTime();
            if(request.operation()==NativeDecoderWire.GEOMETRY) {
                var geometry=NativeDecoderStages.geometry(request.labels(),request.gray(),request.width(),request.height());
                NativeDecoderWire.packet(out,data->NativeDecoderWire.writeGeometry(data,geometry));
                awaitClientClose(in);
                System.out.println("GEOMETRY ms="+(System.nanoTime()-started)/1_000_000);return;
            }
            var score=OmrScoreInterpreter.analyze(request.labels(),request.gray(),request.width(),request.height(),request.measures());
            NativeDecoderWire.packet(out,data->NativeDecoderWire.writeAnalysis(data,score));
            awaitClientClose(in);
            // No document identity, OCR text, credentials or score content in service logs.
            System.out.println("DECODE ms="+(System.nanoTime()-started)/1_000_000+" notes="+score.notes().size());
        }catch(Exception failure){System.err.println("Decoder request failed: "+failure.getClass().getSimpleName());}
    }
    private static void awaitClientClose(DataInputStream in)throws IOException {
        // Keep large responses alive through guest NAT until the client consumes them.
        // The accepted socket's timeout bounds clients that never close.
        if(in.read()!=-1)throw new IOException("Unexpected trailing request data");
    }
}
