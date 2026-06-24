import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LlmSettingsService, AiConfigResponse } from './llm-settings.service';
import { WorkspaceService } from '../workspaces/workspace.service';
import {
  WorkspaceLlmConfigService,
  WorkspaceLlmConfigResponse,
} from './workspace-llm-config.service';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-llm-settings',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './llm-settings.component.html',
})
export class LlmSettingsComponent implements OnInit {
  private settingsService = inject(LlmSettingsService);
  private workspaceService = inject(WorkspaceService);
  private workspaceLlmConfigService = inject(WorkspaceLlmConfigService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  config = signal<AiConfigResponse | null>(null);
  openaiApiKey = signal('');
  testResult = signal<{ success: boolean; message: string } | null>(null);
  isTesting = signal(false);
  isReindexing = signal(false);
  isSaving = signal(false);
  saveMessage = signal('');

  protected workspaces = signal<{ id: string; name: string }[]>([]);
  protected selectedWorkspaceId = signal<string>('');
  protected workspaceConfig = signal<WorkspaceLlmConfigResponse | null>(null);
  protected isWorkspaceMode = signal(false);
  protected systemPrompt = signal('');

  providers = computed(() => [
    {
      value: 'OLLAMA',
      label: this.translationService.translate('admin.settings.llm.provider.ollama'),
    },
    {
      value: 'OPENAI_COMPATIBLE',
      label: this.translationService.translate('admin.settings.llm.provider.openai'),
    },
  ]);
  modelExamples = ['llama3.2:3b', 'llama3.1:8b', 'mistral', 'mixtral'];
  embeddingExamples = ['nomic-embed-text', 'all-minilm'];

  updateConfigField<K extends keyof AiConfigResponse>(
    field: K,
    value: AiConfigResponse[K],
  ): void {
    const cfg = this.config();
    if (cfg == null) return;
    this.config.set({ ...cfg, [field]: value });
  }

  ngOnInit(): void {
    this.settingsService
      .getConfig()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (config) => {
          config.llmProvider = (config.llmProvider || 'OLLAMA').trim().toUpperCase();
          if (!this.providers().some((p) => p.value === config.llmProvider)) {
            config.llmProvider = 'OLLAMA';
          }
          this.config.set(config);
        },
        error: () => {
          this.config.set({
            llmProvider: 'OLLAMA',
            ollamaEndpointUrl: 'http://host.docker.internal:11434',
            hasOpenaiKey: false,
            chatModelName: 'llama3.2:3b',
            embeddingModelName: 'nomic-embed-text',
            similarityThreshold: 0.65,
            embeddingDimension: 768,
          });
        },
      });

    this.workspaceService
      .getWorkspaces()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ws) => this.workspaces.set(ws.map((w) => ({ id: w.id, name: w.name }))),
      });
  }

  onProviderChange(): void {
    const cfg = this.config();
    if (cfg == null) return;
    const normalized = (cfg.llmProvider || 'OLLAMA').trim().toUpperCase();
    const updated: AiConfigResponse = {
      ...cfg,
      llmProvider: normalized,
      ollamaEndpointUrl:
        normalized === 'OLLAMA' ? 'http://host.docker.internal:11434' : cfg.ollamaEndpointUrl,
    };
    this.config.set(updated);
    if (normalized === 'OLLAMA') {
      this.openaiApiKey.set('');
    }
  }

  onWorkspaceChange(): void {
    const wsId = this.selectedWorkspaceId();
    if (!wsId) {
      this.isWorkspaceMode.set(false);
      this.workspaceConfig.set(null);
      return;
    }
    this.isWorkspaceMode.set(true);
    this.workspaceLlmConfigService
      .getConfig(wsId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (config) => {
          this.workspaceConfig.set(config);
          // Populate form fields from workspace config
          const cfg = this.config();
          if (cfg) {
            this.config.set({
              ...cfg,
              llmProvider: config.llmProvider,
              ollamaEndpointUrl: config.endpointUrl,
              chatModelName: config.modelName,
              embeddingModelName: config.embeddingModelName,
              similarityThreshold: config.similarityThreshold,
            });
            this.systemPrompt.set(config.systemPrompt ?? '');
          }
        },
        error: () => {
          // No config yet for this workspace
          this.workspaceConfig.set(null);
          const cfg = this.config();
          if (cfg) {
            this.config.set({
              ...cfg,
              llmProvider: 'OLLAMA',
              ollamaEndpointUrl: 'http://host.docker.internal:11434', // NOSONAR - dev default for local Ollama
              chatModelName: 'llama3.2',
              embeddingModelName: 'nomic-embed-text',
              similarityThreshold: 0.65,
            });
          }
        },
      });
  }

  save(): void {
    const cfg = this.config();
    if (cfg == null) return;
    this.isSaving.set(true);
    this.saveMessage.set('');

    const wsId = this.selectedWorkspaceId();
    if (wsId) {
      firstValueFrom(
        this.workspaceLlmConfigService.saveConfig(wsId, {
          llmProvider: cfg.llmProvider,
          endpointUrl: cfg.ollamaEndpointUrl,
          apiKey: this.openaiApiKey() || undefined,
          modelName: cfg.chatModelName,
          embeddingModelName: cfg.embeddingModelName,
          similarityThreshold: cfg.similarityThreshold,
          systemPrompt: this.systemPrompt() || null,
        }),
      )
        .then((wsConfig) => {
          this.config.set({
            llmProvider: wsConfig.llmProvider,
            ollamaEndpointUrl: wsConfig.endpointUrl,
            hasOpenaiKey: !!this.openaiApiKey(),
            chatModelName: wsConfig.modelName,
            embeddingModelName: wsConfig.embeddingModelName,
            similarityThreshold: wsConfig.similarityThreshold,
            embeddingDimension: wsConfig.embeddingDimension,
          });
          this.openaiApiKey.set('');
          this.saveMessage.set(this.translationService.translate('admin.settings.llm.saved'));
        })
        .catch(() => {
          this.saveMessage.set(this.translationService.translate('admin.settings.llm.save-error'));
        })
        .finally(() => {
          this.isSaving.set(false);
        });
    } else {
      firstValueFrom(
        this.settingsService.updateConfig({
          llmProvider: cfg.llmProvider,
          ollamaEndpointUrl: cfg.ollamaEndpointUrl,
          openaiApiKey: this.openaiApiKey() || undefined,
          chatModelName: cfg.chatModelName,
          embeddingModelName: cfg.embeddingModelName,
          similarityThreshold: cfg.similarityThreshold,
        }),
      )
        .then((response) => {
          this.config.set(response);
          this.openaiApiKey.set('');
          this.saveMessage.set(this.translationService.translate('admin.settings.llm.saved'));
        })
        .catch(() => {
          this.saveMessage.set(this.translationService.translate('admin.settings.llm.save-error'));
        })
        .finally(() => {
          this.isSaving.set(false);
        });
    }
  }

  testConnection(): void {
    const cfg = this.config();
    if (cfg == null) return;
    this.isTesting.set(true);
    this.testResult.set(null);

    const wsId = this.selectedWorkspaceId();
    const obs = wsId
      ? this.workspaceLlmConfigService.testConnection(wsId, {
          llmProvider: cfg.llmProvider,
          endpointUrl: cfg.ollamaEndpointUrl,
          apiKey: this.openaiApiKey() || undefined,
          modelName: cfg.chatModelName,
          embeddingModelName: cfg.embeddingModelName,
          similarityThreshold: cfg.similarityThreshold,
        })
      : this.settingsService.testConnection({
          llmProvider: cfg.llmProvider,
          ollamaEndpointUrl: cfg.ollamaEndpointUrl,
          openaiApiKey: this.openaiApiKey() || undefined,
          chatModelName: cfg.chatModelName,
          embeddingModelName: cfg.embeddingModelName,
        });

    firstValueFrom(obs)
      .then((result) => {
        this.testResult.set(result);
      })
      .catch(() => {
        this.testResult.set({
          success: false,
          message: this.translationService.translate('admin.settings.llm.test-failure'),
        });
      })
      .finally(() => {
        this.isTesting.set(false);
      });
  }

  insertTemplateVariable(variable: string): void {
    const current = this.systemPrompt();
    this.systemPrompt.set(current + variable);
  }

  reindex(): void {
    this.isReindexing.set(true);
    firstValueFrom(this.settingsService.reindexEmbeddings())
      .then((result) => {
        this.saveMessage.set(result.message);
      })
      .catch(() => {
        this.saveMessage.set(this.translationService.translate('admin.settings.llm.reindex-error'));
      })
      .finally(() => {
        this.isReindexing.set(false);
      });
  }
}
