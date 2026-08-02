package com.igarciamen.users.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Deque;

// Limitador basico en memoria: maximo MAX_ATTEMPTS intentos de login por IP,
// dentro de una ventana deslizante de WINDOW_MS. Con una sola instancia (como
// aqui) es suficiente; con varias instancias en paralelo haria falta un
// almacen compartido (Redis), fuera de alcance para este TFG.
@Component
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minuto

    private final Map<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = attemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}