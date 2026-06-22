package com.shiftleft.hub.document.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.util.SlugUtils;
import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentNotFoundException;
import com.shiftleft.hub.document.domain.DocumentProcessingException;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * Converts a READY {@link Document} into a DRAFT {@link Article} for the KB.
 *
 * <p>Single responsibility: parse the document, derive title/slug, look up
 * the author, persist the article. Does not touch upload or storage concerns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentConverter {

    private final DocumentParserService documentParserService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final DocumentWorkspaceAccess workspaceAccess;

    /**
     * Parses the file content and creates a DRAFT article linked to the source document.
     *
     * @param documentId  the source document's UUID
     * @param authorEmail the email of the user creating the article
     * @return the created article UUID
     * @throws DocumentNotFoundException       if the document is not in the current workspace
     * @throws DocumentProcessingException     if the document isn't READY or parsing fails
     */
    @Transactional
    public UUID convertToArticle(UUID documentId, String authorEmail) {
        Document document = workspaceAccess.requireInCurrentWorkspace(documentId);

        if (document.getStatus() != com.shiftleft.hub.document.domain.DocumentStatus.READY) {
            throw new DocumentProcessingException("Document must be in READY status to convert to article");
        }

        String content = parse(document);
        String title = deriveTitle(document);
        String slug = uniqueSlug(title);
        User author = userRepository.findByEmail(authorEmail)
            .orElseThrow(() -> new DocumentProcessingException("Author not found: " + authorEmail));

        Article article = Article.builder()
            .titleEn(title)
            .contentEn(content)
            .slug(slug)
            .status(ArticleStatus.DRAFT)
            .author(author)
            .build();
        article.setWorkspaceId(document.getWorkspaceId());
        article = articleRepository.save(article);

        log.info("Article created from document {} (article id: {})", documentId, article.getId());
        return article.getId();
    }

    private String parse(Document document) {
        try {
            return documentParserService.parse(
                Paths.get(document.getFilePath()),
                document.getMimeType()
            );
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to parse document for article conversion", e);
        }
    }

    private String deriveTitle(Document document) {
        String filename = document.getFilename();
        return filename != null ? filename.replaceFirst("\\.[^.]+$", "") : "Untitled";
    }

    private String uniqueSlug(String title) {
        String slug = SlugUtils.slugify(title);
        if (slug.isEmpty()) {
            slug = "untitled";
        }
        if (articleRepository.findBySlug(slug).isPresent()) {
            slug = SlugUtils.withUniqueSuffix(slug);
        }
        return slug;
    }
}
