package com.shiftleft.hub.tag.service;

import com.shiftleft.hub.tag.api.dto.CreateTagRequest;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import com.shiftleft.hub.tag.api.dto.UpdateTagRequest;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagNotFoundException;
import com.shiftleft.hub.tag.domain.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
            .map(tag -> TagResponse.from(tag, getArticleCount(tag.getId())))
            .toList();
    }

    public TagResponse getTagById(UUID id) {
        return tagRepository.findById(id)
            .map(tag -> TagResponse.from(tag, getArticleCount(tag.getId())))
            .orElseThrow(() -> new TagNotFoundException(id));
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        Tag tag = Tag.builder()
            .nameEn(request.nameEn())
            .nameFr(request.nameFr())
            .color(request.color())
            .build();
        tag = tagRepository.save(tag);
        return TagResponse.from(tag, 0L);
    }

    @Transactional
    public TagResponse updateTag(UUID id, UpdateTagRequest request) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        tag.setNameEn(request.nameEn());
        tag.setNameFr(request.nameFr());
        tag.setColor(request.color());
        tag = tagRepository.save(tag);
        return TagResponse.from(tag, getArticleCount(tag.getId()));
    }

    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
        long count = getArticleCount(id);
        if (count > 0) {
            throw new IllegalStateException(
                "Cannot delete tag '" + tag.getNameEn() + "': used by " + count + " article(s)");
        }
        tagRepository.delete(tag);
    }

    private long getArticleCount(UUID tagId) {
        return tagRepository.findById(tagId)
            .map(tag -> (long) tag.getArticles().size())
            .orElse(0L);
    }
}
