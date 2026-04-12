package com.example.pleximporter.service;

import com.example.pleximporter.dto.ImportNotification;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportEventStreamService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        String emitterId = UUID.randomUUID().toString();
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError(error -> emitters.remove(emitterId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitter.complete();
            emitters.remove(emitterId);
        }

        return emitter;
    }

    public void publishImportComplete(ImportNotification notification) {
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("import-complete")
                        .data(notification));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitterId);
            }
        });
    }
}
