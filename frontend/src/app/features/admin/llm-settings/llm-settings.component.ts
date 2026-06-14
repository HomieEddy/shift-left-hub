import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
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
  templateUrl: './llm-settings.component.html',
})
export class LlmSettingsComponent implements OnInit {
  private settingsService = inject(LlmSettingsService);
  private workspaceService = inject(WorkspaceService);
  private workspaceLlmConfigService = inject(WorkspaceLlmConfigService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  config: AiConfigResponse | null = null;
  openaiApiKey = '';
  testResult: { success: boolean; message: string } | null = null;
  isTesting = false;
  isReindexing = false;
  isSaving = false;
  saveMessage = '';

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

  ngOnInit(): void {
    this.settingsService
      .getConfig()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (config) => {
          this.config = config;
          this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
          if (!this.providers().some((p) => p.value === this.config!.llmProvider)) {
            this.config.llmProvider = 'OLLAMA';
          }
        },
        error: () => {
          this.config = {
            llmProvider: 'OLLAMA',
            ollamaEndpointUrl: 'http://host.docker.internal:11434',
            hasOpenaiKey: false,
            chatModelName: 'llama3.2:3b',
            embeddingModelName: 'nomic-embed-text',
            similarityThreshold: 0.65,
            embeddingDimension: 768,
          };
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
    if (this.config == null) return;
    this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
    if (this.config?.llmProvider === 'OLLAMA') {
      this.config.ollamaEndpointUrl = 'http://host.docker.internal:11434';
      this.openaiApiKey = '';
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
          if (this.config) {
            this.config.llmProvider = config.llmProvider;
            this.config.ollamaEndpointUrl = config.endpointUrl;
            this.config.chatModelName = config.modelName;
            this.config.embeddingModelName = config.embeddingModelName;
            this.config.similarityThreshold = config.similarityThreshold;
            this.systemPrompt.set(config.systemPrompt ?? '');
          }
        },
        error: () => {
          // No config yet for this workspace
          this.workspaceConfig.set(null);
          if (this.config) {
            this.config.llmProvider = 'OLLAMA';
            this.config.ollamaEndpointUrl = 'http://host.docker.internal:11434'; // NOSONAR - dev default for local Ollama
            this.config.chatModelName = 'llama3.2';
            this.config.embeddingModelName = 'nomic-embed-text';
            this.config.similarityThreshold = 0.65;
          }
        },
      });
  }

  save(): void {
    if (this.config == null) return;
    this.isSaving = true;
    this.saveMessage = '';

    const wsId = this.selectedWorkspaceId();
    if (wsId) {
      firstValueFrom(
        this.workspaceLlmConfigService.saveConfig(wsId, {
          llmProvider: this.config.llmProvider,
          endpointUrl: this.config.ollamaEndpointUrl,
          apiKey: this.openaiApiKey || undefined,
          modelName: this.config.chatModelName,
          embeddingModelName: this.config.embeddingModelName,
          similarityThreshold: this.config.similarityThreshold,
          systemPrompt: this.systemPrompt() || null,
        }),
      )
        .then((wsConfig) => {
          this.config = {
            llmProvider: wsConfig.llmProvider,
            ollamaEndpointUrl: wsConfig.endpointUrl,
            hasOpenaiKey: !!this.openaiApiKey,
            chatModelName: wsConfig.modelName,
            embeddingModelName: wsConfig.embeddingModelName,
            similarityThreshold: wsConfig.similarityThreshold,
            embeddingDimension: wsConfig.embeddingDimension,
          };
          this.openaiApiKey = '';
          this.saveMessage = this.translationService.translate('admin.settings.llm.saved');
        })
        .catch(() => {
          this.saveMessage = this.translationService.translate('admin.settings.llm.save-error');
        })
        .finally(() => {
          this.isSaving = false;
        });
    } else {
      firstValueFrom(
        this.settingsService.updateConfig({
          llmProvider: this.config.llmProvider,
          ollamaEndpointUrl: this.config.ollamaEndpointUrl,
          openaiApiKey: this.openaiApiKey || undefined,
          chatModelName: this.config.chatModelName,
          embeddingModelName: this.config.embeddingModelName,
          similarityThreshold: this.config.similarityThreshold,
        }),
      )
        .then((response) => {
          this.config = response;
          this.openaiApiKey = '';
          this.saveMessage = this.translationService.translate('admin.settings.llm.saved');
        })
        .catch(() => {
          this.saveMessage = this.translationService.translate('admin.settings.llm.save-error');
        })
        .finally(() => {
          this.isSaving = false;
        });
    }
  }

  testConnection(): void {
    if (this.config == null) return;
    this.isTesting = true;
    this.testResult = null;

    const wsId = this.selectedWorkspaceId();
    const obs = wsId
      ? this.workspaceLlmConfigService.testConnection(wsId, {
          llmProvider: this.config.llmProvider,
          endpointUrl: this.config.ollamaEndpointUrl,
          apiKey: this.openaiApiKey || undefined,
          modelName: this.config.chatModelName,
          embeddingModelName: this.config.embeddingModelName,
          similarityThreshold: this.config.similarityThreshold,
        })
      : this.settingsService.testConnection({
          llmProvider: this.config.llmProvider,
          ollamaEndpointUrl: this.config.ollamaEndpointUrl,
          openaiApiKey: this.openaiApiKey || undefined,
          chatModelName: this.config.chatModelName,
          embeddingModelName: this.config.embeddingModelName,
        });

    firstValueFrom(obs)
      .then((result) => {
        this.testResult = result;
      })
      .catch(() => {
        this.testResult = {
          success: false,
          message: this.translationService.translate('admin.settings.llm.test-failure'),
        };
      })
      .finally(() => {
        this.isTesting = false;
      });
  }

  insertTemplateVariable(variable: string): void {
    const current = this.systemPrompt();
    this.systemPrompt.set(current + variable);
  }

  reindex(): void {
    this.isReindexing = true;
    firstValueFrom(this.settingsService.reindexEmbeddings())
      .then((result) => {
        this.saveMessage = result.message;
      })
      .catch(() => {
        this.saveMessage = this.translationService.translate('admin.settings.llm.reindex-error');
      })
      .finally(() => {
        this.isReindexing = false;
      });
  }
}
