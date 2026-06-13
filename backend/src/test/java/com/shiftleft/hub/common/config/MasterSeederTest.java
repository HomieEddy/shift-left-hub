package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AiConfigRepository aiConfigRepository;
    @Mock private WorkspaceService workspaceService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private TagRepository tagRepository;

    private MasterSeeder seeder;
    private final UUID adminId = UUID.randomUUID();
    private final UUID hrWsId = UUID.randomUUID();
    private final UUID legalWsId = UUID.randomUUID();
    private final UUID itWsId = UUID.randomUUID();
    private final UUID publicWsId = UUID.randomUUID();

    @Captor private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() throws Exception {
        seeder = new MasterSeeder(userRepository, passwordEncoder, aiConfigRepository,
            workspaceService, workspaceRepository, articleRepository, tagRepository);
        setField("adminEmail", "admin@company.com");
        setField("adminPassword", "test-password");
    }

    private void setField(String name, String value) throws Exception {
        Field field = MasterSeeder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(seeder, value);
    }

    private Workspace makeWorkspace(UUID id, String name, String slug) {
        return Workspace.builder().id(id).name(name).slug(slug)
            .createdBy(adminId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Workspace workspaceForSlug(String slug) {
        return switch (slug) {
            case "human-resources" -> makeWorkspace(hrWsId, "Human Resources", slug);
            case "legal" -> makeWorkspace(legalWsId, "Legal", slug);
            case "it" -> makeWorkspace(itWsId, "IT", slug);
            case "public" -> makeWorkspace(publicWsId, "Public", slug);
            default -> throw new IllegalArgumentException("Unknown slug: " + slug);
        };
    }

    @Test
    void seed_shouldCreateAllUsersAndWorkspaces() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        User admin = User.builder().id(adminId).email("admin@company.com")
            .displayName("System Admin").role(UserRole.ROLE_ADMIN).build();
        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(workspaceService.findBySlug(anyString())).thenReturn(Optional.empty());
        when(workspaceService.createWorkspace(anyString(), anyString(), any(), eq(adminId)))
            .thenAnswer(inv -> {
                String name = inv.getArgument(0);
                UUID id = switch (name) {
                    case "Human Resources" -> hrWsId;
                    case "Legal" -> legalWsId;
                    case "IT" -> itWsId;
                    default -> publicWsId;
                };
                return makeWorkspace(id, name, name.toLowerCase().replace(' ', '-'));
            });
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiConfigRepository.count()).thenReturn(0L);

        seeder.seed();

        verify(userRepository, atLeast(7)).save(userCaptor.capture());
        var distinctEmails = userCaptor.getAllValues().stream()
            .map(User::getEmail)
            .distinct()
            .toList();
        assertEquals(7, distinctEmails.size());
        assertTrue(distinctEmails.contains("admin@company.com"));
        assertTrue(distinctEmails.contains("hr.user@company.com"));
        assertTrue(distinctEmails.contains("hr.tech@company.com"));
        assertTrue(distinctEmails.contains("it.tech@company.com"));

        verify(workspaceService, times(4)).createWorkspace(anyString(), anyString(), any(), eq(adminId));
        verify(workspaceService, atLeast(0)).assignUserToWorkspace(any(), any(), anyString());
        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    @Test
    void seed_shouldBeIdempotent() {
        User admin = User.builder().id(adminId).email("admin@company.com")
            .displayName("System Admin").role(UserRole.ROLE_ADMIN)
            .defaultWorkspaceId(publicWsId).build();
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(workspaceService.findBySlug(anyString())).thenAnswer(inv -> {
            String slug = inv.getArgument(0);
            return Optional.of(workspaceForSlug(slug));
        });
        when(aiConfigRepository.count()).thenReturn(1L);

        seeder.seed();

        verify(userRepository, never()).save(any(User.class));
        verify(workspaceService, never()).createWorkspace(anyString(), anyString(), any(), any());
        verify(aiConfigRepository, never()).save(any(AiConfig.class));
    }

    @Test
    void seed_shouldCleanupOldArticles() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        User admin = User.builder().id(adminId).email("admin@company.com")
            .displayName("System Admin").role(UserRole.ROLE_ADMIN)
            .defaultWorkspaceId(publicWsId).build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(workspaceService.findBySlug(anyString())).thenAnswer(inv -> {
            String slug = inv.getArgument(0);
            return Optional.of(workspaceForSlug(slug));
        });
        when(aiConfigRepository.count()).thenReturn(1L);
        Article oldArticle = Article.builder().id(UUID.randomUUID()).slug("connect-corporate-wifi").build();
        when(articleRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(articleRepository.findBySlug("connect-corporate-wifi")).thenReturn(Optional.of(oldArticle));

        seeder.seed();

        verify(articleRepository).delete(any(Article.class));
        verify(articleRepository, times(9)).findBySlug(anyString());
    }

    @Test
    void seed_shouldSkipWhenAdminEmailIsNull() throws Exception {
        setField("adminEmail", null);

        seeder.seed();

        verifyNoInteractions(userRepository, aiConfigRepository, workspaceService, workspaceRepository,
            articleRepository, tagRepository);
    }
}
