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

/**
 * Service for managing workspace-scoped category taxonomy.
 * Provides CRUD operations, merge, and reassign-on-delete functionality.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final DocumentRepository documentRepository;

    /**
     * Returns all categories for the current workspace as a flat list.
     *
     * @return flat list of category responses
     */
    public List<CategoryResponse> getAllCategories() {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        return categoryRepository.findByWorkspaceIdOrderByNameEnAsc(workspaceId).stream()
            .map(cat -> CategoryResponse.from(cat, cat.getChildren().size()))
            .toList();
    }

    /**
     * Returns a single category by ID within the current workspace.
     *
     * @param id the category UUID
     * @return the category response
     */
    public CategoryResponse getCategory(UUID id) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category category = categoryRepository.findByWorkspaceIdAndId(workspaceId, id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        return CategoryResponse.from(category, category.getChildren().size());
    }

    /**
     * Creates a new category with optional parent reference.
     *
     * @param request the category creation request
     * @return the created category response
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.nameEn() == null || request.nameEn().isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category.CategoryBuilder builder = Category.builder()
            .nameEn(request.nameEn())
            .nameFr(request.nameFr());

        if (request.parentId() != null) {
            Category parent = categoryRepository.findByWorkspaceIdAndId(workspaceId, request.parentId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
            builder.parent(parent);
        }

        Category saved = builder.build();
        saved.setWorkspaceId(workspaceId);
        saved = categoryRepository.save(saved);
        return CategoryResponse.from(saved, 0L);
    }

    /**
     * Updates an existing category's name and parent reference.
     *
     * @param id      the category UUID
     * @param request the category update request
     * @return the updated category response
     */
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category category = categoryRepository.findByWorkspaceIdAndId(workspaceId, id)
            .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setNameEn(request.nameEn());
        category.setNameFr(request.nameFr());

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findByWorkspaceIdAndId(workspaceId, request.parentId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved, saved.getChildren().size());
    }

    /**
     * Deletes a category. If it has content and reassignToId is provided,
     * reassigns all articles and documents to the target category.
     *
     * @param id           the category UUID
     * @param reassignToId the target category UUID for content reassignment, or null
     */
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

    /**
     * Merges source category into target, reassigning all content and child categories.
     *
     * @param sourceId the source category UUID
     * @param targetId the target category UUID
     * @return the target category response after merge
     */
    @Transactional
    public CategoryResponse mergeCategories(UUID sourceId, UUID targetId) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Category source = categoryRepository.findByWorkspaceIdAndId(workspaceId, sourceId)
            .orElseThrow(() -> new CategoryNotFoundException(sourceId));
        Category target = categoryRepository.findByWorkspaceIdAndId(workspaceId, targetId)
            .orElseThrow(() -> new CategoryNotFoundException(targetId));

        // Reassign child categories to target before deleting source
        categoryRepository.findByParentId(sourceId).forEach(child -> {
            child.setParent(target);
            categoryRepository.save(child);
        });

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