package com.example.websocket.service;

import com.example.websocket.model.Message;
import com.example.websocket.utils.MD5Utils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import javax.validation.constraints.NotNull;
import java.util.*;

@Component
public class SystemWebSocketHandler implements WebSocketHandler {


    private static final Map<String, List> userSocketSessionMap;

    private static final Map<String, WebSocketSession> socketSessionMap;

    private ObjectMapper mapper = new ObjectMapper();


    static {

        userSocketSessionMap = new HashMap<>(16);
        socketSessionMap = new HashMap<>(16);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wss) throws Exception {

        String uid = wss.getAttributes().get("uid").toString();
        String sessionId = wss.getAttributes().get("HTTP.SESSION.ID").toString();
        String socketSessionId = MD5Utils.md5(sessionId);

        wss.getAttributes().put("socketSessionID", socketSessionId);

        List userSessionArray = new ArrayList();
        if (null == userSocketSessionMap.get(uid)) {

            if (wss.getAttributes().containsKey("socketSessionID")) {


                userSessionArray.add(socketSessionId);
                userSocketSessionMap.put(uid, userSessionArray);
            }
        } else {

            userSessionArray = userSocketSessionMap.get(uid);
            userSessionArray.add(socketSessionId);
        }

        socketSessionMap.put(socketSessionId, wss);
    }

    @Override
    public void handleMessage(WebSocketSession wss, WebSocketMessage<?> wsm)throws Exception {

        if (wsm.getPayloadLength() == 0) {
            return;
        }


        String payload = wsm.getPayload().toString();


        Boolean isHeartBeat = heartBeat(wsm);

        if (isHeartBeat) {
            wss.sendMessage(new TextMessage("pong"));
            return;
        }

        Message message = new Message();

        try {
            message = mapper.readValue(payload, Message.class);
        } catch (Exception e) {

            wss.sendMessage(new TextMessage("message is valid"));
            return;
        }

        long time = System.currentTimeMillis();
        String now = String.valueOf(time / 1000);

        wss.sendMessage(new TextMessage(now));

        message.setTimestamp(now);
        message.setMsgId(UUID.randomUUID().toString());

        Thread.sleep(1000);

        if(!socketSessionMap.containsKey(message.getToId())){

            wss.sendMessage(new TextMessage(message.getToId()+" is not online"));
            return;
        }

        broadcastMessageToUsers(userSocketSessionMap.get(message.getToId()), new TextMessage(mapper.writeValueAsBytes(message)));


    }

    @Override
    public void handleTransportError(WebSocketSession wss, Throwable throwable) throws Exception {

        if (wss.isOpen()) {
            wss.close();
        }

        System.out.println(wss.getHandshakeHeaders().getFirst("Cookie") + "出现故障，连接断开");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wss, CloseStatus closeStatus) {


        String sessionId = wss.getAttributes().get("socketSessionId").toString();

        String uid = wss.getAttributes().get("uid").toString();

        if (userSocketSessionMap.containsKey(uid)) {


            List sessionIdArray = userSocketSessionMap.get(uid);

            sessionIdArray.remove(sessionId);

            if (socketSessionMap.containsKey(sessionId)) {
                socketSessionMap.remove(sessionId);
            }
        }

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }


    @NotNull
    public static Boolean heartBeat(WebSocketMessage wsm) {

        String payload = wsm.getPayload().toString();

        if (payload != null && payload.equals("ping")) {
            return true;
        }


        return false;
    }

    private synchronized void broadcastMessageToUsers(List sessionIDArray, TextMessage message) throws Exception {


        if (sessionIDArray == null || sessionIDArray.isEmpty()) {

            return;
        }

        Iterator<String> iterator = sessionIDArray.iterator();

        while (iterator.hasNext()) {

            String sessionId = iterator.next();
            if (socketSessionMap.containsKey(sessionId)) {
                this.sendMessageToUser(sessionId, message);
            }
        }
    }

    private void sendMessageToUser(String sessionId, TextMessage message) throws Exception {

        WebSocketSession wss = socketSessionMap.get(sessionId);

        if (wss != null && wss.isOpen()) {
            wss.sendMessage(message);
        }
    }
}
