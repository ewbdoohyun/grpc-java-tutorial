package com.service;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import proto.hello.HelloRequest;
import proto.hello.HelloResponse;
import proto.hello.HelloServiceGrpc;

@Service
@Slf4j
public class HelloService {
    @GrpcClient("test")
    private HelloServiceGrpc.HelloServiceBlockingStub simpleStub;

    public String sendMessage(final String name) {
        try{
            HelloResponse response = this.simpleStub.sayHello(HelloRequest.newBuilder().setName(name).build());
            return response.getResponse();
        } catch (StatusRuntimeException e) {
            log.error("error : ",e);
            return "FAILED with " + e.getStatus().getCode().name();
        }
    }
}
