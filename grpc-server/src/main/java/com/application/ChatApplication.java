package com.application;

import io.grpc.stub.StreamObserver;
import com.google.protobuf.util.Timestamps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import proto.chat.*;

import java.util.*;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
public class ChatApplication extends ChatRoomGrpc.ChatRoomImplBase {


    final Map<String, List<ChatInfo>> chatRoomIdToInfosMap = Collections.synchronizedMap(new HashMap<>());
    final Map<String, Set<String>> chatRoomIdToUserIdsPushMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void addUser(AddUserRequest request, StreamObserver<AddUserResponse> responseObserver) {

        // 채팅방에 실제로 들어왔다. 라는 메시지를 보내야 하는데,
        log.info("addUser Req : {}", request.toString());
        String chatRoomId = request.getChatRoomId();
        broadCast(chatRoomId, ChatResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogin(ChatResponse.Login.newBuilder()
                        .setName(request.getDisplayName())
                        .build()
                ).build());
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     *  rpc CreateStream(Connect) returns (stream Message);
     * </pre>
     */
    public StreamObserver<ChatRequest> chat(StreamObserver<ChatResponse> responseObserver) {
        log.info("Chat : Hello");
        return new StreamObserver<ChatRequest>() {
            private String userId;
            private String chatRoomId;

            @Override
            public void onNext(ChatRequest value) {

                log.info("message from {}, room : {} message : {}", value.getUserId(), value.getChatRoomId(), value.getMessage());
                userId = value.getUserId();
                chatRoomId = value.getChatRoomId();

                List<ChatInfo> chatInfos = chatRoomIdToInfosMap.get(chatRoomId);
                if (chatInfos == null) {
                    chatInfos = new ArrayList<>();
                }
                Optional<ChatInfo> chatInfo = chatInfos.stream().filter(ci -> ci.getUserId().equals(userId)).findAny();
                if (!chatInfo.isPresent()) {
                    chatInfos.add(new ChatInfo(userId, responseObserver));
                    chatRoomIdToInfosMap.put(chatRoomId,chatInfos);
                } else {
                    log.info("change stream Observer");
                    chatInfo.get().setStreamObserver(responseObserver);
                }

                broadCast(value.getChatRoomId(), ChatResponse
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

            private void doExit() {
                log.info("called doExit {} {}", chatRoomId, userId);
                List<ChatInfo> chatInfos = chatRoomIdToInfosMap.get(chatRoomId);
                if (chatInfos == null) {
                    log.error("채팅 정보를 찾을 수 없습니다.");
                    return;
                }
                Optional<ChatInfo> chatInfo = chatInfos.stream().filter(ci -> ci.getUserId().equals(userId)).findAny();
                if (!chatInfo.isPresent()) {
                    log.error("채팅 정보를 찾을 수 없습니다.2");
                    return;
                }
                // 채팅 커넥션 삭제
                chatInfos.remove(chatInfo.get());
                Set<String> userIds = chatRoomIdToUserIdsPushMap.getOrDefault(chatRoomId, new HashSet<>());
                userIds.add(userId);
                chatRoomIdToUserIdsPushMap.put(chatRoomId, userIds);
                // todo : logout 처리하여, push로 메세지 받을 수 있도록 전환해야 한다.
            }
        };
    }

    private void broadCast(String chatRoomId, ChatResponse msg) {
        List<StreamObserver<ChatResponse>> clients = chatRoomIdToInfosMap
                .getOrDefault(chatRoomId, Collections.emptyList()).stream().map(ChatInfo::getStreamObserver)
                .collect(Collectors.toList());
        for (StreamObserver<ChatResponse> res : clients) {
            res.onNext(msg);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ChatInfo {
        private String userId;
        @Setter
        private StreamObserver<ChatResponse> streamObserver;
    }


}