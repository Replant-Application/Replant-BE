package com.app.replant.controller.dto;

import com.app.replant.entity.type.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "필수 카테고리 업데이트 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEssentialCategoriesRequest {

    @Schema(description = "필수 카테고리 목록", example = "[\"HEALTH\", \"STUDY\", \"HOBBY\"]")
    private List<CategoryType> essentialCategories;
}
