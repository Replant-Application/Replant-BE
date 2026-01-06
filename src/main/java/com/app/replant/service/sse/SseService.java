package com.app.replant.service.sse;

import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final ObjectMapper objectMapper;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long memberId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(memberId, emitter);

        emitter.onCompletion(() -> handleEmitterRemoval(memberId, "completion"));
        emitter.onTimeout(() -> handleEmitterRemoval(memberId, "timeout"));
        emitter.onError(ex -> handleEmitterRemoval(memberId, "error"));

        log.info("SSE emitter 등록 - memberId: {}, 현재 연결 수: {}", memberId, emitters.size());
        return emitter;
    }

    public void removeEmitter(Long memberId) {
        emitters.remove(memberId);
        log.info("SSE emitter 수동 제거 - memberId: {}, 현재 연결 수: {}", memberId, emitters.size());
    }

    public boolean isConnected(Long memberId) {
        return emitters.containsKey(memberId);
    }

    public Set<Long> getConnectedMemberIds() {
        return emitters.keySet();
    }

    public boolean sendToUser(Long memberId, String eventName, Object data) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        Objects.requireNonNull(data, "data must not be null");
        log.debug("SSE 알림 전송 시도 - memberId: {}, eventName: {}, 현재 연결된 사용자: {}",
                memberId, eventName, emitters.keySet());
        SseEmitter emitter = emitters.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("SSE 알림 전송 성공 - memberId: {}, eventName: {}", memberId, eventName);
                return true;
            } catch (IOException e) {
                log.error("SSE 메시지 전송 실패 - memberId: {}", memberId, e);
                emitters.remove(memberId);
                return false;
            }
        }

        log.warn("SSE 연결된 클라이언트 없음 - memberId: {}, eventName: {}, 현재 연결 수: {}, 연결된 사용자 ID 목록: {}",
                memberId, eventName, emitters.size(), emitters.keySet());
        return false;
    }

    public int getConnectedUserCount() {
        return emitters.size();
    }

    public void sendReportNotification(Long memberId, int month) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("title", "리포트 알림");
            data.put("message", "소비 리포트가 준비되었습니다.");
            data.put("month", month);
            data.put("actionUrl", "/report");

            String jsonData = objectMapper.writeValueAsString(data);
            sendToUser(memberId, "REPORT", jsonData);
            log.info("REPORT 알림 전송 - memberId: {}, month: {}", memberId, month);
        } catch (Exception e) {
            log.error("REPORT 알림 전송 실패 - memberId: {}", memberId, e);
        }
    }

    public void sendDiaryNotification(Long memberId) {
        try {
            String today = LocalDate.now().toString();
            Map<String, Object> data = new HashMap<>();
            data.put("title", "일기 알림");
            data.put("message", "오늘 하루는 어떠셨나요? 일기를 작성해보세요.");
            data.put("actionUrl", "/calendar/diary/emotion?date=" + today);

            String jsonData = objectMapper.writeValueAsString(data);
            sendToUser(memberId, "diary", jsonData);
            log.info("DIARY 알림 전송 - memberId: {}, date: {}", memberId, today);
        } catch (Exception e) {
            log.error("DIARY 알림 전송 실패 - memberId: {}", memberId, e);
        }
    }

    public void sendMissionNotification(Long memberId, String missionType, int missionCount) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("title", "미션 알림");
            data.put("message", String.format("%d개의 %s 미션이 배정되었습니다. 지금 확인해보세요!", missionCount, missionType));
            data.put("missionType", missionType);
            data.put("missionCount", missionCount);
            data.put("actionUrl", "/missions");

            String jsonData = objectMapper.writeValueAsString(data);
            sendToUser(memberId, "MISSION", jsonData);
            log.info("MISSION 알림 전송 - memberId: {}, type: {}, count: {}", memberId, missionType, missionCount);
        } catch (Exception e) {
            log.error("MISSION 알림 전송 실패 - memberId: {}", memberId, e);
        }
    }

    private void handleEmitterRemoval(Long memberId, String reason) {
        emitters.remove(memberId);
        log.info("SSE emitter 제거 - memberId: {}, 이유: {}, 현재 연결 수: {}", memberId, reason, emitters.size());
    }
}
