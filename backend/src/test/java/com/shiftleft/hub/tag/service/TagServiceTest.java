package com.shiftleft.hub.tag.service;

import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.tag.api.dto.CreateTagRequest;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import com.shiftleft.hub.tag.api.dto.UpdateTagRequest;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagInUseException;
import com.shiftleft.hub.tag.domain.TagNotFoundException;
import com.shiftleft.hub.tag.domain.TagRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private ArticleRepository articleRepository;

    @InjectMocks private TagService tagService;

    private final UUID tagId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String nameEn = "network";
    private final String nameFr = "réseau";
    private final String color = "#3498db";

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private Tag createTag() {
        return Tag.builder()
            .id(tagId)
            .nameEn(nameEn)
            .nameFr(nameFr)
            .color(color)
            .articles(new HashSet<>())
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── getAllTags (paginated) ───────────────────────────────

    @Test
    void getAllTags_shouldReturnPageWithArticleCounts() {
        Tag tag = createTag();
        when(tagRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(tag)));
        when(articleRepository.countByTagIds(List.of(tagId))).thenReturn(List.of());

        Page<TagResponse> page = tagService.getAllTags(0, 20);

        assertEquals(1, page.getContent().size());
        assertEquals(nameEn, page.getContent().get(0).nameEn());
        assertEquals(0L, page.getContent().get(0).articleCount());
    }

    @Test
    void getAllTags_shouldReturnEmptyPage() {
        when(tagRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        Page<TagResponse> page = tagService.getAllTags(0, 20);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void getAllTags_shouldPassPageableWithCorrectSize() {
        when(tagRepository.findAll(any(Pageable.class)))
            .thenAnswer(inv -> {
                Pageable p = inv.getArgument(0);
                assertEquals(1, p.getPageNumber());
                assertEquals(5, p.getPageSize());
                return new PageImpl<>(List.of());
            });

        tagService.getAllTags(1, 5);
    }

    // ── getTagById ──────────────────────────────────────────

    @Test
    void getTagById_shouldSucceed() {
        Tag tag = createTag();
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(articleRepository.countByTagId(tagId)).thenReturn(0L);

        TagResponse response = tagService.getTagById(tagId);

        assertNotNull(response);
        assertEquals(tagId, response.id());
        assertEquals(nameEn, response.nameEn());
    }

    @Test
    void getTagById_shouldThrowWhenNotFound() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class,
            () -> tagService.getTagById(tagId));
    }

    // ── createTag ───────────────────────────────────────────

    @Test
    void createTag_shouldSucceed() {
        CreateTagRequest request = new CreateTagRequest(nameEn, nameFr, color);
        when(tagRepository.save(any(Tag.class))).thenReturn(createTag());

        TagResponse response = tagService.createTag(request);

        assertNotNull(response);
        assertEquals(nameEn, response.nameEn());
        assertEquals(0L, response.articleCount());
        verify(tagRepository).save(any(Tag.class));
    }

    // ── updateTag ───────────────────────────────────────────

    @Test
    void updateTag_shouldSucceed() {
        Tag tag = createTag();
        UpdateTagRequest request = new UpdateTagRequest("updated-en", "updated-fr", "#ff0000");
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);
        when(articleRepository.countByTagId(tagId)).thenReturn(0L);

        TagResponse response = tagService.updateTag(tagId, request);

        assertNotNull(response);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void updateTag_shouldThrowWhenNotFound() {
        UpdateTagRequest request = new UpdateTagRequest("updated-en", "updated-fr", "#ff0000");
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class,
            () -> tagService.updateTag(tagId, request));
    }

    // ── deleteTag ───────────────────────────────────────────

    @Test
    void deleteTag_shouldSucceedWhenUnused() {
        Tag tag = createTag();
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(articleRepository.countByTagId(tagId)).thenReturn(0L);

        tagService.deleteTag(tagId);

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTag_shouldThrowWhenTagInUse() {
        Tag tag = createTag();
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(articleRepository.countByTagId(tagId)).thenReturn(5L);

        assertThrows(TagInUseException.class,
            () -> tagService.deleteTag(tagId));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void deleteTag_shouldThrowWhenNotFound() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class,
            () -> tagService.deleteTag(tagId));
    }

    // ── createTag: validation ────────────────────────────────

    @Test
    void createTag_shouldThrowWhenNameBlank() {
        CreateTagRequest request = new CreateTagRequest("", nameFr, color);

        assertThrows(IllegalArgumentException.class,
            () -> tagService.createTag(request));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createTag_shouldThrowWhenDuplicateInWorkspace() {
        Tag existingTag = createTag();
        existingTag.setWorkspaceId(workspaceId);
        CreateTagRequest request = new CreateTagRequest(nameEn, nameFr, color);
        when(tagRepository.findByNameEnIn(List.of(nameEn))).thenReturn(List.of(existingTag));

        assertThrows(IllegalArgumentException.class,
            () -> tagService.createTag(request));
        verify(tagRepository, never()).save(any());
    }
}
