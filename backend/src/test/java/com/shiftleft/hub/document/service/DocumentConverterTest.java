package com.shiftleft.hub.document.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentNotFoundException;
import com.shiftleft.hub.document.domain.DocumentProcessingException;
import com.shiftleft.hub.document.domain.DocumentRepository;
import com.shiftleft.hub.document.domain.DocumentStatus;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentConverterTest {

    @Mock private DocumentParserService documentParserService;
    @Mock private ArticleRepository articleRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;

    private DocumentWorkspaceAccess workspaceAccess;
    private DocumentConverter converter;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final String AUTHOR_EMAIL = "author@example.com";

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(WORKSPACE_ID);
        workspaceAccess = new DocumentWorkspaceAccess(documentRepository);
        converter = new DocumentConverter(documentParserService, articleRepository, userRepository, workspaceAccess);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private Document createDocument(DocumentStatus status) {
        Document doc = Document.builder()
            .id(DOCUMENT_ID)
            .filename("vpn-setup.md")
            .mimeType("text/markdown")
            .contentHash("abc")
            .status(status)
            .filePath("/tmp/test.md")
            .fileSize(1024L)
            .build();
        doc.setWorkspaceId(WORKSPACE_ID);
        return doc;
    }

    @Test
    void convertToArticle_shouldCreateDraftArticleFromReadyDocument() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));
        when(documentParserService.parse(any(), anyString())).thenReturn("# VPN Setup\n\nInstall the client.");
        User author = User.builder().id(AUTHOR_ID).email(AUTHOR_EMAIL).build();
        when(userRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(author));
        when(articleRepository.findBySlug("vpn-setup")).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        UUID articleId = converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL);

        assertNotNull(articleId);
    }

    @Test
    void convertToArticle_shouldDeriveSlugFromFilename() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));
        when(documentParserService.parse(any(), anyString())).thenReturn("content");
        when(userRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(User.builder().id(AUTHOR_ID).build()));
        when(articleRepository.findBySlug("vpn-setup")).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL);

        org.mockito.ArgumentCaptor<Article> captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        org.mockito.Mockito.verify(articleRepository).save(captor.capture());
        Article saved = captor.getValue();
        assertEquals("vpn-setup", saved.getSlug());
        assertEquals("vpn-setup", saved.getTitleEn().toLowerCase());
        assertEquals(ArticleStatus.DRAFT, saved.getStatus());
    }

    @Test
    void convertToArticle_shouldAppendUniqueSuffixWhenSlugExists() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));
        when(documentParserService.parse(any(), anyString())).thenReturn("content");
        when(userRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(User.builder().id(AUTHOR_ID).build()));
        when(articleRepository.findBySlug("vpn-setup")).thenReturn(Optional.of(Article.builder().id(UUID.randomUUID()).build()));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL);

        org.mockito.ArgumentCaptor<Article> captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        org.mockito.Mockito.verify(articleRepository).save(captor.capture());
        String slug = captor.getValue().getSlug();
        assertTrue(slug.startsWith("vpn-setup-"), "expected slug to start with vpn-setup-, got " + slug);
    }

    @Test
    void convertToArticle_shouldThrowWhenDocumentNotFound() {
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
            () -> converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL));
    }

    @Test
    void convertToArticle_shouldThrowWhenDocumentNotInCurrentWorkspace() {
        Document doc = createDocument(DocumentStatus.READY);
        doc.setWorkspaceId(UUID.randomUUID()); // different workspace
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));

        assertThrows(DocumentNotFoundException.class,
            () -> converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL));
    }

    @Test
    void convertToArticle_shouldThrowWhenDocumentNotReady() {
        Document doc = createDocument(DocumentStatus.UPLOADED);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));

        assertThrows(DocumentProcessingException.class,
            () -> converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL));
    }

    @Test
    void convertToArticle_shouldThrowWhenAuthorNotFound() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(doc));
        when(documentParserService.parse(any(), anyString())).thenReturn("content");
        when(userRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.empty());

        assertThrows(DocumentProcessingException.class,
            () -> converter.convertToArticle(DOCUMENT_ID, AUTHOR_EMAIL));
    }
}
