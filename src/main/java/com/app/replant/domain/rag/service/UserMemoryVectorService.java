package com.app.replant.domain.rag.service;

import com.app.replant.domain.diary.entity.Diary;
import com.app.replant.domain.rag.enums.UserMemoryCategory;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.global.util.UuidV5;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMemoryVectorService {

    private final VectorStore vectorStore;

    public void upsertDiary(Diary diary) {
        if (diary == null || diary.getId() == null) {
            return;
        }
        String content = normalizeContent(diary.getContent());
        if (content.isBlank()) {
            return;
        }

        String id = UuidV5.fromCategoryAndOrigin(UserMemoryCategory.DIARY.value(), diary.getId()).toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", diary.getUser().getId().toString());
        metadata.put("category", UserMemoryCategory.DIARY.value());
        metadata.put("origin_id", diary.getId());
        metadata.put("content", content);
        if (diary.getDate() != null) {
            metadata.put("date", diary.getDate().toString());
        }

        Document document = Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .build();

        vectorStore.add(List.of(document));
    }

    public void upsertUserMission(UserMission userMission) {
        if (userMission == null || userMission.getId() == null || userMission.getUser() == null) {
            return;
        }
        if (userMission.getMission() == null) {
            return;
        }

        String content = normalizeContent(buildMissionContent(userMission));
        if (content.isBlank()) {
            return;
        }

        String id = UuidV5.fromCategoryAndOrigin(UserMemoryCategory.MISSION.value(), userMission.getId()).toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", userMission.getUser().getId().toString());
        metadata.put("category", UserMemoryCategory.MISSION.value());
        metadata.put("origin_id", userMission.getId());
        metadata.put("content", content);
        if (userMission.getAssignedAt() != null) {
            metadata.put("date", userMission.getAssignedAt().toString());
        }

        Document document = Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .build();

        vectorStore.add(List.of(document));
    }

    public void deleteDiary(Long diaryId) {
        deleteByCategoryAndOrigin(UserMemoryCategory.DIARY, diaryId);
    }

    public void deleteUserMission(Long userMissionId) {
        deleteByCategoryAndOrigin(UserMemoryCategory.MISSION, userMissionId);
    }

    public List<Document> searchUserMemory(Long userId, String query, UserMemoryCategory category, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        String filterExpression = category == null
                ? builder.eq("user_id", userId.toString()).toString()
                : builder.and(
                        builder.eq("user_id", userId.toString()),
                        builder.eq("category", category.value())
                ).toString();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();

        return vectorStore.similaritySearch(request);
    }

    private void deleteByCategoryAndOrigin(UserMemoryCategory category, Long originId) {
        if (originId == null) {
            return;
        }
        String id = UuidV5.fromCategoryAndOrigin(category.value(), originId).toString();
        vectorStore.delete(List.of(id));
    }

    private String buildMissionContent(UserMission userMission) {
        String title = userMission.getMission().getTitle();
        String description = userMission.getMission().getDescription();
        if (title == null && description == null) {
            return "";
        }
        if (title == null) {
            return description;
        }
        if (description == null) {
            return title;
        }
        return title + "\n" + description;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.trim();
    }
}
