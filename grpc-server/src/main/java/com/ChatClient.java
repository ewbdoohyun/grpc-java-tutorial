package com;

import com.common.Constant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.chat.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatClient {

    private static Logger logger = LoggerFactory.getLogger(ChatClient.class);
    private final ManagedChannel channel;
    private ChatRoomGrpc.ChatRoomBlockingStub blockingStub;
    private StreamObserver<ChatRequest> chat;
    private String userId = "";
    private boolean Loggined = false;
    public ChatClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    private ChatClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = ChatRoomGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public boolean login(String userId,String chatRoomId,String name) {
        AddUserRequest request = AddUserRequest.newBuilder()
                .setUserId(userId)
                .setChatRoomId(chatRoomId)
                .setDisplayName(name)
                .build();
        AddUserResponse response;
//        try {
//            response = blockingStub.addUser(request);
//        } catch (StatusRuntimeException e) {
//            logger.error("rpc failed with status:" + e.getStatus() + " message:" + e.getMessage());
//            return false;
//        }
        this.userId = userId;
        this.Loggined = true;
        startReceive();
        logger.info("login with name {} OK!",name);
        return true;
    }

    private void startReceive(){
        Metadata meta = new Metadata();
        meta.put(Constant.HEADER_ROLE,this.userId);

        chat =  MetadataUtils.attachHeaders(ChatRoomGrpc.newStub(this.channel),meta).chat(new StreamObserver<ChatResponse>() {
            @Override
            public void onNext(ChatResponse value) {
                logger.info("Event : {}",value.toString());
                switch (value.getEventCase()){
                    case ROLE_LOGIN:
                    {
                        logger.info("user {}:login!!",value.getRoleLogin().getName());
                    }
                    break;
                    case ROLE_LOGOUT:
                    {
                        logger.info("user {}:logout!!",value.getRoleLogout().getName());
                    }
                    break;
                    case ROLE_MESSAGE:
                    {
                        logger.info("user {}:{}",value.getRoleMessage().getUserId(),value.getRoleMessage().getMsg());
                    }
                    break;
                    case EVENT_NOT_SET:
                    {
                        logger.error("receive event error:{}",value);
                    }
                    break;
                    case SERVER_SHUTDOWN:
                    {
                        logger.info("server closed!");
                        logout();
                    }
                    break;
                }
            }
            @Override
            public void onError(Throwable t) {
                logger.error("got error from server:{}",t.getMessage(),t);
            }

            @Override
            public void onCompleted() {
                logger.info("closed by server");
            }
        });
    }

    public void sendMessage(String userId,String chatRoomId,String msg) throws InterruptedException {
        if("LOGOUT".equals(msg)){
            this.chat.onCompleted();
            this.logout();
            this.Loggined = false;
            shutdown();
        }else{
            if(this.chat != null) this.chat.onNext(
                    ChatRequest.newBuilder()
                            .setUserId(userId)
                            .setChatRoomId(chatRoomId)
                            .setMessage(msg)
                            .build());
        }
    }

    public void logout(){
//        LogoutResponse resp = blockingStub.logout(Chat.LogoutRequest.newBuilder().build());
//        logger.info("logout result:{}",resp);
    }

    public static void main(String[] args) throws InterruptedException {
        ChatClient client = new ChatClient("localhost", 9090);
        try {
            String name = "안녕";
            String userId = "user_id2";
            String chatRoomId = "chatRoomId";
            Scanner sc = new Scanner(System.in);
            do{
                System.out.println("please input your nickname");
                name = sc.nextLine();
                System.out.println("please input your userId");
                userId = sc.nextLine();
            }while (!client.login(userId,chatRoomId,name));
            String msg = "";
            while(true){
                msg = sc.nextLine();
                client.sendMessage(userId,chatRoomId,msg);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
