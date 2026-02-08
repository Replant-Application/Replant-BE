package com.app.replant.domain.rag.service;

import com.app.replant.domain.diary.entity.Diary;
import com.app.replant.domain.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OneTimeRagSyncRunner implements ApplicationRunner {

    private final DiaryRepository diaryRepository;
    private final UserMemoryVectorService userMemoryVectorService;

    @Value("${rag.sync.once:false}")
    private boolean syncOnce;

    @Value("${rag.sync.limit:0}")
    private int syncLimit;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnce) {
            return;
        }

        log.warn("RAG one-time sync is enabled. This will read from DB and upsert into Qdrant.");
        syncDiaries();
        // Mission sync disabled for now
        log.warn("RAG one-time sync completed. Disable rag.sync.once after this run.");
    }

    private void syncDiaries() {
        int pageSize = syncLimit > 0 ? syncLimit : 500;
        int page = 0;
        long total = 0;

        while (true) {
            Page<Diary> diaries = diaryRepository.findAll(
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "id")));
            if (diaries.isEmpty()) {
                break;
            }
            for (Diary diary : diaries) {
                userMemoryVectorService.upsertDiary(diary);
                total++;
                if (syncLimit > 0 && total >= syncLimit) {
                    log.info("RAG sync (diary) reached limit={}", syncLimit);
                    return;
                }
            }
            if (!diaries.hasNext() || syncLimit > 0) {
                break;
            }
            page++;
        }

        log.info("RAG sync (diary) done. total={}", total);
    }

}
