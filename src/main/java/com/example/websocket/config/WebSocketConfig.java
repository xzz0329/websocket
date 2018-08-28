package com.example.websocket.config;

import com.example.websocket.interceptor.HandShakeInterceptor;
import com.example.websocket.service.SystemWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebMvc
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(systemWebSocketHandler(), "/wss").addInterceptors(new HandShakeInterceptor()).setAllowedOrigins("*");

        System.out.println("websocketIntercept has registed");
    }

    private SystemWebSocketHandler systemWebSocketHandler(){

        return new SystemWebSocketHandler();
    }


}
