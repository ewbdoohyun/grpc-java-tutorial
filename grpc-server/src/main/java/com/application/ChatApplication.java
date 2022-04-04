package com.application;

import io.grpc.stub.StreamObserver;
import com.google.protobuf.util.Timestamps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import proto.chat.*;

import java.util.*;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
public class ChatApplication extends ChatGrpc.ChatImplBase {

    // key =
//    final Map<String, UserVo> userMap = new HashMap<>();
    // key : chatRoom // value ==> ChatResponse
    // 어자피
    final Map<String, List<ChatInfo>> chatStreamMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void addUser(AddUserRequest request, StreamObserver<AddUserResponse> responseObserver) {

        for(String chatRoomId : request.getChatRoomIdList()){
            broadCast(chatRoomId,ChatResponse
                    .newBuilder()
                    .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                    .setRoleLogin(
                            ChatResponse.Login.newBuilder()
                                    .setName(request.getDisplayName())
                                    .build()
                    ).build());
        }
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     *  rpc CreateStream(Connect) returns (stream Message);
     * </pre>
     */
    public StreamObserver<ChatRequest> chat(StreamObserver<ChatResponse> responseObserver) {

        return new StreamObserver<ChatRequest>() {
            private String  userId;
            private String  chatRoomId;
            @Override
            public void onNext(ChatRequest value) {
                log.info("message from {}, room : {}, {}",value.getUserId(),value.getChatRoomId(),value.getMessage());
                userId = value.getUserId();
                chatRoomId = value.getChatRoomId();
                broadCast(value.getChatRoomId(),ChatResponse
                        .newBuilder()
                        .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                        .setRoleMessage(
                                ChatResponse.Message.newBuilder()
                                        .setMsg(value.getMessage())
                                        .setChatRoomId(value.getChatRoomId())
                                        .setUserId(value.getChatRoomId())
                                        .build()
                        ).build());
            }

            @Override
            public void onError(Throwable t) {
                doExit();
            }

            @Override
            public void onCompleted() {
                doExit();
            }

            private void doExit(){
                // todo : logout 처리하여, push로 메세지 받을 수 있도록 전환해야 한다.
            }
        };
    }

    private void broadCast(String chatRoomId, ChatResponse msg){
        List<StreamObserver<ChatResponse>> clients = chatStreamMap.get(chatRoomId).stream().map(ChatInfo::getStreamObserver)
                .collect(Collectors.toList());
        for(StreamObserver<ChatResponse> res : clients){
            res.onNext(msg);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ChatInfo{
        private String userId;
        private StreamObserver<ChatResponse> streamObserver;
    }
}