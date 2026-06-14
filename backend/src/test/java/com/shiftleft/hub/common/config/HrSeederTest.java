package com.shiftleft.hub.common.config;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrSeederTest {

    @Mock private TagRepository tagRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceService workspaceService;

    @InjectMocks
    private HrSeeder seeder;

    private final UUID hrWsId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        adminUser = User.builder().id(adminId).email("admin@company.com").displayName("System Admin").build();
        Field field = HrSeeder.class.getDeclaredField("seedEnabled");
        field.setAccessible(true);
        field.set(seeder, true);
    }

    @Test
    void seed_shouldSkipWhenNoAdminFound() {
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of());

        seeder.seed();

        verifyNoInteractions(workspaceService, tagRepository, articleRepository);
    }

    @Test
    void seed_shouldSkipWhenWorkspaceNotFound() {
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(workspaceService.findBySlug("human-resources")).thenReturn(Optional.empty());

        seeder.seed();

        verify(tagRepository, never()).save(any(Tag.class));
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void seed_shouldCreateTagsWhenNoneExist() {
        Workspace hrWs = Workspace.builder().id(hrWsId).name("Human Resources").slug("human-resources").build();
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(workspaceService.findBySlug("human-resources")).thenReturn(Optional.of(hrWs));
        when(tagRepository.findAll()).thenReturn(List.of());
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        seeder.seed();

        verify(tagRepository, times(8)).save(any(Tag.class));
    }

    @Test
    void seed_shouldNotDuplicateExistingTags() {
        Workspace hrWs = Workspace.builder().id(hrWsId).name("Human Resources").slug("human-resources").build();
        Tag recruitmentTag = Tag.builder().nameEn("Recruitment").nameFr("Recrutement").color("#2563eb").build();
        recruitmentTag.setWorkspaceId(hrWsId);
        Tag benefitsTag = Tag.builder().nameEn("Benefits").nameFr("Avantages sociaux").color("#16a34a").build();
        benefitsTag.setWorkspaceId(hrWsId);
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(workspaceService.findBySlug("human-resources")).thenReturn(Optional.of(hrWs));
        when(tagRepository.findAll()).thenReturn(List.of(recruitmentTag, benefitsTag));
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        seeder.seed();

        verify(tagRepository, times(6)).save(any(Tag.class));
    }

    @Test
    void seed_shouldUpdateExistingArticle() {
        Workspace hrWs = Workspace.builder().id(hrWsId).name("Human Resources").slug("human-resources").build();
        Tag tag = Tag.builder().nameEn("Training").nameFr("Formation").color("#ea580c").build();
        tag.setWorkspaceId(hrWsId);
        Article existing = Article.builder().id(UUID.randomUUID()).slug("hr-recruitment-process").build();
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(workspaceService.findBySlug("human-resources")).thenReturn(Optional.of(hrWs));
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.of(existing));
        when(articleRepository.save(any(Article.class))).thenReturn(existing);

        seeder.seed();

        verify(articleRepository, atLeastOnce()).save(any(Article.class));
    }

    // ── Idempotency ─────────────────────────────────────────

    @Test
    void run_shouldBeIdempotent() {
        Workspace hrWs = Workspace.builder().id(hrWsId).name("Human Resources").slug("human-resources").build();
        Tag tag1 = Tag.builder().nameEn("Recruitment").nameFr("Recrutement").color("#2563eb").build();
        tag1.setWorkspaceId(hrWsId);
        Tag tag2 = Tag.builder().nameEn("Benefits").nameFr("Avantages sociaux").color("#16a34a").build();
        tag2.setWorkspaceId(hrWsId);
        Tag tag3 = Tag.builder().nameEn("Policies").nameFr("Politiques").color("#9333ea").build();
        tag3.setWorkspaceId(hrWsId);
        Tag tag4 = Tag.builder().nameEn("Onboarding").nameFr("Intégration").color("#0891b2").build();
        tag4.setWorkspaceId(hrWsId);
        Tag tag5 = Tag.builder().nameEn("Payroll").nameFr("Paie").color("#ca8a04").build();
        tag5.setWorkspaceId(hrWsId);
        Tag tag6 = Tag.builder().nameEn("Performance").nameFr("Performance").color("#dc2626").build();
        tag6.setWorkspaceId(hrWsId);
        Tag tag7 = Tag.builder().nameEn("Compliance").nameFr("Conformité").color("#7c3aed").build();
        tag7.setWorkspaceId(hrWsId);
        Tag tag8 = Tag.builder().nameEn("Training").nameFr("Formation").color("#ea580c").build();
        tag8.setWorkspaceId(hrWsId);
        when(userRepository.findByRole(UserRole.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(workspaceService.findBySlug("human-resources")).thenReturn(Optional.of(hrWs));
        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2, tag3, tag4, tag5, tag6, tag7, tag8));
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        // First run — all 8 tags already exist, no saves needed
        seeder.seed();
        verify(tagRepository, never()).save(any(Tag.class));

        // Second run — still idempotent
        clearInvocations(tagRepository);
        seeder.seed();
        verify(tagRepository, never()).save(any(Tag.class));
    }
}
