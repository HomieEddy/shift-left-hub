import { Component, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LlmSettingsService, AiConfigResponse } from './llm-settings.service';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-llm-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './llm-settings.component.html',
})
export class LlmSettingsComponent implements OnInit {
  private settingsService = inject(LlmSettingsService);
  protected translationService = inject(TranslationService);

  config: AiConfigResponse | null = null;
  openaiApiKey = '';
  testResult: { success: boolean; message: string } | null = null;
  isTesting = false;
  isReindexing = false;
  isSaving = false;
  saveMessage = '';

  providers = computed(() => [
    { value: 'OLLAMA', label: this.translationService.translate('admin.settings.llm.provider.ollama') },
    { value: 'OPENAI', label: this.translationService.translate('admin.settings.llm.provider.openai') },
  ]);
  modelExamples = ['llama3.2:3b', 'llama3.1:8b', 'mistral', 'mixtral'];
  embeddingExamples = ['nomic-embed-text', 'all-minilm'];

  ngOnInit(): void {
    this.settingsService.getConfig().subscribe({
      next: (config) => {
        this.config = config;
        this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
        if (!this.providers().some(p => p.value === this.config!.llmProvider)) {
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
  }

  onProviderChange(): void {
    if (this.config == null) return;
    this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
    if (this.config?.llmProvider === 'OLLAMA') {
      this.config.ollamaEndpointUrl = 'http://host.docker.internal:11434';
      this.openaiApiKey = '';
    }
  }

  save(): void {
    if (this.config == null) return;
    this.isSaving = true;
    this.saveMessage = '';
    firstValueFrom(this.settingsService.updateConfig({
      llmProvider: this.config.llmProvider,
      ollamaEndpointUrl: this.config.ollamaEndpointUrl,
      openaiApiKey: this.openaiApiKey || undefined,
      chatModelName: this.config.chatModelName,
      embeddingModelName: this.config.embeddingModelName,
      similarityThreshold: this.config.similarityThreshold,
    })).then(config => {
      this.config = config;
      this.openaiApiKey = '';
      this.saveMessage = this.translationService.translate('admin.settings.llm.saved');
    }).catch(() => {
      this.saveMessage = this.translationService.translate('admin.settings.llm.save-error');
    }).finally(() => {
      this.isSaving = false;
    });
  }

  testConnection(): void {
    if (this.config == null) return;
    this.isTesting = true;
    this.testResult = null;
    firstValueFrom(this.settingsService.testConnection({
      llmProvider: this.config.llmProvider,
      ollamaEndpointUrl: this.config.ollamaEndpointUrl,
      openaiApiKey: this.openaiApiKey || undefined,
      chatModelName: this.config.chatModelName,
      embeddingModelName: this.config.embeddingModelName,
    })).then(result => {
      this.testResult = result;
    }).catch(() => {
      this.testResult = { success: false, message: this.translationService.translate('admin.settings.llm.test-failure') };
    }).finally(() => {
      this.isTesting = false;
    });
  }

  reindex(): void {
    this.isReindexing = true;
    firstValueFrom(this.settingsService.reindexEmbeddings()).then(result => {
      this.saveMessage = result.message;
    }).catch(() => {
      this.saveMessage = this.translationService.translate('admin.settings.llm.reindex-error');
    }).finally(() => {
      this.isReindexing = false;
    });
  }
}
