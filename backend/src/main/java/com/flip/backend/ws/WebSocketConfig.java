package com.flip.backend.ws;

import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import com.flip.backend.security.AuthFeatureProperties;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompSecurityInterceptor security;
    private final AuthFeatureProperties properties;

    public WebSocketConfig(StompSecurityInterceptor security, AuthFeatureProperties properties) {
        this.security = security;
        this.properties = properties;
    }

    @Override public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(properties.publicUrl());
    }
    @Override public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(security);
    }
}
