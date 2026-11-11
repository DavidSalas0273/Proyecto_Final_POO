package com.banco.ledger.dashboard.infrastructure.websocket;

import com.banco.ledger.dashboard.application.port.DifusorDashboard;
import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.banco.ledger.dashboard.domain.model.VistaMaterializada;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador WebSocket: al conectarse, el cliente recibe la instantánea completa;
 * después recibe cada actualización (UPSERT) en tiempo real.
 */
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler implements DifusorDashboard {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final VistaMaterializada vista;
    private final ObjectMapper objectMapper;

    public DashboardWebSocketHandler(VistaMaterializada vista, ObjectMapper objectMapper) {
        this.vista = vista;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, TIEMPO_ENVIO_MS, BUFFER_BYTES);
        sesiones.put(session.getId(), segura);
        log.info("Cliente de dashboard conectado: {} (total {})", session.getId(), sesiones.size());
        enviar(segura, serializar(MensajeDashboard.instantanea(vista.instantanea(), vista.cuentas())));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session.getId());
    }

    @Override
    public void difundir(TransaccionVista transaccion) {
        String json = serializar(MensajeDashboard.actualizacion(transaccion, vista.cuentas()));
        sesiones.values().forEach(sesion -> enviar(sesion, json));
    }

    private void enviar(WebSocketSession sesion, String json) {
        try {
            sesion.sendMessage(new TextMessage(json));
        } catch (IOException | IllegalStateException e) {
            log.warn("No se pudo enviar al cliente {}: {}", sesion.getId(), e.getMessage());
            sesiones.remove(sesion.getId());
        }
    }

    private String serializar(MensajeDashboard mensaje) {
        try {
            return objectMapper.writeValueAsString(mensaje);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el mensaje del dashboard", e);
        }
    }
}
