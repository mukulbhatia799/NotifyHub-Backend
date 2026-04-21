package com.notifyhub.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil            jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketJwtHandshakeInterceptor(JwtUtil jwtUtil,
                                            UserDetailsService userDetailsService) {
        this.jwtUtil            = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        if (query == null) {
            log.warn("WebSocket handshake rejected: no query string");
            return false;
        }

        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
                break;
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing token param");
            return false;
        }

        try {
            String      username    = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(token, userDetails)) {
                attributes.put("username", username);
                return true;
            }
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: invalid token — {}", e.getMessage());
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // nothing needed
    }
}
