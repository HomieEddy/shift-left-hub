package com.shiftleft.hub.category.service;

import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.category.api.dto.CategoryRequest;
import com.shiftleft.hub.category.api.dto.CategoryResponse;
import com.shiftleft.hub.category.domain.Category;
import com.shiftleft.hub.category.domain.CategoryRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final DocumentRepository documentRepository;

    public List<CategoryResponse> getAllCategories() {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        return categoryRepository.findByWorkspaceIdOrderByNameEnAsc(workspaceId).stream()
            .map(cat -> CategoryResponse.from(cat, cat.getChildren().size()))
            .toList();
    }

    public CategoryResponse getCategory(UUID id) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category category = categoryRepository.findByWorkspaceIdAndId(workspaceId, id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        return CategoryResponse.from(category, category.getChildren().size());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category.CategoryBuilder builder = Category.builder()
            .nameEn(request.nameEn())
            .nameFr(request.nameFr())
            .workspaceId(workspaceId);

        if (request.parentId() != null) {
            Category parent = categoryRepository.findByWorkspaceIdAndId(workspaceId, request.parentId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
            builder.parent(parent);
        }

        Category saved = categoryRepository.save(builder.build());
        return CategoryResponse.from(saved, 0L);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category category = categoryRepository.findByWorkspaceIdAndId(workspaceId, id)
            .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setNameEn(request.nameEn());
        category.setNameFr(request.nameFr());

        if (request.parentId() != null) {
            Category parent = categoryRepository.findByWorkspaceIdAndId(workspaceId, request.parentId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved, saved.getChildren().size());
    }

    @Transactional
    public void deleteCategory(UUID id, UUID reassignToId) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category category = categoryRepository.findByWorkspaceIdAndId(workspaceId, id)
            .orElseThrow(() -> new CategoryNotFoundException(id));

        if (categoryRepository.existsByParentId(id)) {
            throw new CategoryInUseException(id, category.getNameEn(),
                categoryRepository.findByParentId(id).size());
        }

        long contentCount = countContentByCategory(id);
        if (contentCount > 0 && reassignToId == null) {
            throw new CategoryInUseException(id, category.getNameEn(), contentCount);
        }

        if (contentCount > 0 && reassignToId != null) {
            Category target = categoryRepository.findByWorkspaceIdAndId(workspaceId, reassignToId)
                .orElseThrow(() -> new CategoryNotFoundException(reassignToId));

            reassignContent(id, target);
        }

        categoryRepository.delete(category);
        log.info("Category {} deleted from workspace {}", id, workspaceId);
    }

    @Transactional
    public CategoryResponse mergeCategories(UUID sourceId, UUID targetId) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category source = categoryRepository.findByWorkspaceIdAndId(workspaceId, sourceId)
            .orElseThrow(() -> new CategoryNotFoundException(sourceId));
        Category target = categoryRepository.findByWorkspaceIdAndId(workspaceId, targetId)
            .orElseThrow(() -> new CategoryNotFoundException(targetId));

        reassignContent(sourceId, target);

        categoryRepository.delete(source);
        log.info("Category {} merged into {} in workspace {}", sourceId, targetId, workspaceId);

        return CategoryResponse.from(target, target.getChildren().size());
    }

    private long countContentByCategory(UUID categoryId) {
        return articleRepository.countByCategoryId(categoryId)
            + documentRepository.countByCategoryId(categoryId);
    }

    private void reassignContent(UUID sourceId, Category target) {
        articleRepository.reassignCategory(sourceId, target.getId());
        documentRepository.reassignCategory(sourceId, target.getId());
    }
}