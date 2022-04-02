package com.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import proto.hello.HelloRequest;
import proto.hello.HelloResponse;
import proto.hello.HelloServiceGrpc;


@GrpcService
public class HelloGrpcServer extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        HelloResponse reply = HelloResponse.newBuilder()
                .setResponse("Hello ==> " + request.getName())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

}
