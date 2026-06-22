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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;

    /**
     * Retrieves all tags with their article counts.
     *
     * @return the list of all tag responses
     */
    public List<TagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        Map<UUID, Long> articleCounts = countArticlesPerTag(tags);
        return tags.stream()
            .map(tag -> TagResponse.from(tag, articleCounts.getOrDefault(tag.getId(), 0L)))
            .toList();
    }

    /**
     * Retrieves a tag by its ID.
     *
     * @param id the tag UUID
     * @return the tag response
     */
    public TagResponse getTagById(UUID id) {
        return tagRepository.findById(id)
            .map(tag -> TagResponse.from(tag, getArticleCount(tag)))
            .orElseThrow(() -> new TagNotFoundException(id));
    }

    /**
     * Creates a new tag from the given request.
     *
     * @param request the create tag request
     * @return the created tag response
     */
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        if (request.nameEn() == null || request.nameEn().isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (workspaceId != null && tagRepository.findByNameEnIn(List.of(request.nameEn()))
            .stream().anyMatch(t -> workspaceId.equals(t.getWorkspaceId()))) {
            throw new IllegalArgumentException("Tag already exists in this workspace: " + request.nameEn());
        }
        Tag tag = Tag.builder()
            .nameEn(request.nameEn())
            .nameFr(request.nameFr())
            .color(request.color())
            .build();
        tag = tagRepository.save(tag);
        return TagResponse.from(tag, 0L);
    }

    /**
     * Updates an existing tag.
     *
     * @param id      the tag UUID
     * @param request the update tag request
     * @return the updated tag response
     */
    @Transactional
    public TagResponse updateTag(UUID id, UpdateTagRequest request) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        tag.setNameEn(request.nameEn());
        tag.setNameFr(request.nameFr());
        tag.setColor(request.color());
        tag = tagRepository.save(tag);
        return TagResponse.from(tag, getArticleCount(tag));
    }

    /**
     * Deletes a tag by its ID.
     *
     * @param id the tag UUID
     * @throws TagInUseException if the tag is still in use by articles
     */
    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        long count = getArticleCount(tag);
        if (count > 0) {
            throw new TagInUseException(tag.getId(), tag.getNameEn(), count);
        }
        tagRepository.delete(tag);
    }

    private long getArticleCount(Tag tag) {
        return articleRepository.countByTagId(tag.getId());
    }

    private Map<UUID, Long> countArticlesPerTag(List<Tag> tags) {
        List<UUID> ids = tags.stream().map(Tag::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return articleRepository.countByTagIds(ids).stream()
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long) row[1]));
    }
}
