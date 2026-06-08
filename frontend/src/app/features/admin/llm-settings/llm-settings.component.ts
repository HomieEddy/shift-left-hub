import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LlmSettingsService, AiConfigResponse } from './llm-settings.service';

@Component({
  selector: 'app-llm-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './llm-settings.component.html',
})
export class LlmSettingsComponent implements OnInit {
  private settingsService = inject(LlmSettingsService);

  config: AiConfigResponse | null = null;
  openaiApiKey = '';
  testResult: { success: boolean; message: string } | null = null;
  isTesting = false;
  isReindexing = false;
  isSaving = false;
  saveMessage = '';

  providers = [
    { value: 'OLLAMA', label: 'Ollama' },
    { value: 'OPENAI', label: 'OpenAI' },
  ];
  modelExamples = ['llama3.2:3b', 'llama3.1:8b', 'mistral', 'mixtral'];
  embeddingExamples = ['nomic-embed-text', 'all-minilm'];

  ngOnInit(): void {
    this.settingsService.getConfig().subscribe({
      next: (config) => {
        this.config = config;
        this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
        if (!this.providers.some(p => p.value === this.config!.llmProvider)) {
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
      this.saveMessage = 'Settings saved';
    }).catch(() => {
      this.saveMessage = 'Failed to save settings';
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
      this.testResult = { success: false, message: 'Connection test failed' };
    }).finally(() => {
      this.isTesting = false;
    });
  }

  reindex(): void {
    this.isReindexing = true;
    firstValueFrom(this.settingsService.reindexEmbeddings()).then(result => {
      this.saveMessage = result.message;
    }).catch(() => {
      this.saveMessage = 'Failed to start re-embedding';
    }).finally(() => {
      this.isReindexing = false;
    });
  }
}
