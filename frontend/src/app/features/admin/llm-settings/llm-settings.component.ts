import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { $localize } from '@angular/localize/init';
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
    { value: 'OLLAMA', label: $localize`:@@admin.settings.llm.provider.ollama:Ollama` },
    { value: 'OPENAI', label: $localize`:@@admin.settings.llm.provider.openai:OpenAI` },
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

  saveLabel = $localize`:@@admin.settings.llm.save:Save Settings`;
  savingLabel = $localize`:@@admin.settings.llm.saving:Saving...`;
  testLabel = $localize`:@@admin.settings.llm.test:Test Connection`;
  testingLabel = $localize`:@@admin.settings.llm.testing:Testing...`;
  reindexLabel = $localize`:@@admin.settings.llm.reindex:Re-Embed All Articles`;
  reindexingLabel = $localize`:@@admin.settings.llm.reindexing:Re-Embedding...`;

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
      this.saveMessage = $localize`:@@admin.settings.llm.saved:Settings saved`;
    }).catch(() => {
      this.saveMessage = $localize`:@@admin.settings.llm.save-error:Failed to save settings`;
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
      this.testResult = { success: false, message: $localize`:@@admin.settings.llm.test-failure:Connection test failed` };
    }).finally(() => {
      this.isTesting = false;
    });
  }

  reindex(): void {
    this.isReindexing = true;
    firstValueFrom(this.settingsService.reindexEmbeddings()).then(result => {
      this.saveMessage = result.message;
    }).catch(() => {
      this.saveMessage = $localize`:@@admin.settings.llm.reindex-error:Failed to start re-embedding`;
    }).finally(() => {
      this.isReindexing = false;
    });
  }
}
