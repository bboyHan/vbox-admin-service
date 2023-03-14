//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.vbox.service.websocket;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisListener implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RedisListener.class);

    public RedisListener() {
    }

    public void onMessage(Message message, byte[] pattern) {
        String messageContext = new String(message.getBody());

        try {
            WebSocketServer.sendInfo(messageContext, (String)null);
        } catch (IOException var5) {
            throw new RuntimeException(var5);
        }
    }
}
