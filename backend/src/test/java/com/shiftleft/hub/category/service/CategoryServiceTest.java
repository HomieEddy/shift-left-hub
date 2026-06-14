package com.shiftleft.hub.category.service;

import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.category.api.dto.CategoryRequest;
import com.shiftleft.hub.category.api.dto.CategoryResponse;
import com.shiftleft.hub.category.domain.Category;
import com.shiftleft.hub.category.domain.CategoryRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private DocumentRepository documentRepository;

    @InjectMocks private CategoryService categoryService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
    }

    private Category createCategory(UUID id, String nameEn, String nameFr, Category parent) {
        Category cat = Category.builder()
            .id(id)
            .nameEn(nameEn)
            .nameFr(nameFr)
            .parent(parent)
            .children(new HashSet<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        cat.setWorkspaceId(workspaceId);
        return cat;
    }

    @Test
    void getAllCategories_shouldReturnFlatList() {
        Category cat = createCategory(categoryId, "Hardware", "Matériel", null);
        when(categoryRepository.findByWorkspaceIdOrderByNameEnAsc(workspaceId)).thenReturn(List.of(cat));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertEquals(1, responses.size());
        assertEquals("Hardware", responses.getFirst().nameEn());
        assertNull(responses.getFirst().parentId());
    }

    @Test
    void getCategory_shouldSucceed() {
        Category cat = createCategory(categoryId, "Network", "Réseau", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));

        CategoryResponse response = categoryService.getCategory(categoryId);

        assertNotNull(response);
        assertEquals(categoryId, response.id());
    }

    @Test
    void getCategory_shouldThrowWhenNotFound() {
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategory(categoryId));
    }

    @Test
    void createCategory_shouldSucceed() {
        CategoryRequest request = new CategoryRequest("Printers", "Imprimantes", null);
        Category saved = createCategory(categoryId, "Printers", "Imprimantes", null);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Printers", response.nameEn());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldSetParent() {
        Category parent = createCategory(parentId, "Hardware", "Matériel", null);
        CategoryRequest request = new CategoryRequest("Printers", "Imprimantes", parentId);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        verify(categoryRepository).findByWorkspaceIdAndId(workspaceId, parentId);
    }

    @Test
    void createCategory_shouldThrowWhenParentNotFound() {
        CategoryRequest request = new CategoryRequest("Printers", "Imprimantes", parentId);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, parentId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void updateCategory_shouldSucceed() {
        Category cat = createCategory(categoryId, "Old", "Ancien", null);
        CategoryRequest request = new CategoryRequest("New", "Nouveau", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any(Category.class))).thenReturn(cat);

        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        assertNotNull(response);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteCategory_shouldSucceedWhenUnused() {
        Category cat = createCategory(categoryId, "Test", "Test", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
        when(articleRepository.countByCategoryId(categoryId)).thenReturn(0L);
        when(documentRepository.countByCategoryId(categoryId)).thenReturn(0L);

        categoryService.deleteCategory(categoryId, null);

        verify(categoryRepository).delete(cat);
    }

    @Test
    void deleteCategory_shouldThrowWhenHasChildren() {
        Category cat = createCategory(categoryId, "Test", "Test", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(true);

        assertThrows(CategoryInUseException.class, () -> categoryService.deleteCategory(categoryId, null));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_shouldThrowWhenContentExistsWithoutReassign() {
        Category cat = createCategory(categoryId, "Test", "Test", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
        when(articleRepository.countByCategoryId(categoryId)).thenReturn(3L);

        assertThrows(CategoryInUseException.class, () -> categoryService.deleteCategory(categoryId, null));
    }

    @Test
    void deleteCategory_shouldReassignContent() {
        Category cat = createCategory(categoryId, "Source", "Source", null);
        Category target = createCategory(parentId, "Target", "Cible", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, parentId)).thenReturn(Optional.of(target));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
        when(articleRepository.countByCategoryId(categoryId)).thenReturn(2L);
        when(documentRepository.countByCategoryId(categoryId)).thenReturn(1L);

        categoryService.deleteCategory(categoryId, parentId);

        verify(articleRepository).reassignCategory(categoryId, parentId);
        verify(documentRepository).reassignCategory(categoryId, parentId);
        verify(categoryRepository).delete(cat);
    }

    @Test
    void mergeCategories_shouldReassignContentAndDeleteSource() {
        Category source = createCategory(categoryId, "Source", "Source", null);
        Category target = createCategory(parentId, "Target", "Cible", null);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(source));
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, parentId)).thenReturn(Optional.of(target));
        when(categoryRepository.findByParentId(categoryId)).thenReturn(List.of());

        categoryService.mergeCategories(categoryId, parentId);

        verify(articleRepository).reassignCategory(categoryId, parentId);
        verify(documentRepository).reassignCategory(categoryId, parentId);
        verify(categoryRepository).delete(source);
    }

    @Test
    void mergeCategories_shouldThrowWhenSourceNotFound() {
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
            () -> categoryService.mergeCategories(categoryId, parentId));
    }

    // ── createCategory: validation ─────────────────────────────

    @Test
    void createCategory_shouldThrowWhenNameBlank() {
        CategoryRequest request = new CategoryRequest("", "Imprimantes", null);

        assertThrows(IllegalArgumentException.class,
            () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    // ── getCategories: empty ──────────────────────────────────

    @Test
    void getCategories_shouldReturnEmptyListWhenNoneExist() {
        when(categoryRepository.findByWorkspaceIdOrderByNameEnAsc(workspaceId)).thenReturn(List.of());

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertTrue(responses.isEmpty());
    }

    // ── updateCategory: circular parent ────────────────────────

    @Test
    void updateCategory_shouldRejectCircularParent() {
        Category cat = createCategory(categoryId, "Self", "Soi", null);
        CategoryRequest request = new CategoryRequest("Self", "Soi", categoryId);
        when(categoryRepository.findByWorkspaceIdAndId(workspaceId, categoryId)).thenReturn(Optional.of(cat));

        assertThrows(IllegalArgumentException.class,
            () -> categoryService.updateCategory(categoryId, request));
    }
}