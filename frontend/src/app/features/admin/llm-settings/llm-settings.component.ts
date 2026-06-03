import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { LlmSettingsService, AiConfigResponse, AiConfigRequest } from './llm-settings.service';

@Component({
  selector: 'app-llm-settings',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor],
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

  async ngOnInit() {
    try {
      this.config = await firstValueFrom(this.settingsService.getConfig());
      this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
      if (!this.providers.some(p => p.value === this.config!.llmProvider)) {
        this.config.llmProvider = 'OLLAMA';
      }
    } catch {
      this.config = {
        llmProvider: 'OLLAMA',
        ollamaEndpointUrl: 'http://localhost:11434',
        hasOpenaiKey: false,
        chatModelName: 'llama3.2:3b',
        embeddingModelName: 'nomic-embed-text',
        similarityThreshold: 0.65,
        embeddingDimension: 768,
      };
    }
  }

  onProviderChange() {
    if (!this.config) return;
    this.config.llmProvider = (this.config.llmProvider || 'OLLAMA').trim().toUpperCase();
    if (this.config?.llmProvider === 'OLLAMA') {
      this.config.ollamaEndpointUrl = 'http://localhost:11434';
      this.openaiApiKey = '';
    }
  }

  async save() {
    if (!this.config) return;
    this.isSaving = true;
    this.saveMessage = '';
    try {
      const req: AiConfigRequest = {
        llmProvider: this.config.llmProvider,
        ollamaEndpointUrl: this.config.ollamaEndpointUrl,
        openaiApiKey: this.openaiApiKey || undefined,
        chatModelName: this.config.chatModelName,
        embeddingModelName: this.config.embeddingModelName,
        similarityThreshold: this.config.similarityThreshold,
      };
      this.config = await firstValueFrom(this.settingsService.updateConfig(req));
      this.openaiApiKey = '';
      this.saveMessage = 'Settings saved';
    } catch {
      this.saveMessage = 'Failed to save settings';
    } finally {
      this.isSaving = false;
    }
  }

  async testConnection() {
    if (!this.config) return;
    this.isTesting = true;
    this.testResult = null;
    try {
      const req: AiConfigRequest = {
        llmProvider: this.config.llmProvider,
        ollamaEndpointUrl: this.config.ollamaEndpointUrl,
        openaiApiKey: this.openaiApiKey || undefined,
        chatModelName: this.config.chatModelName,
        embeddingModelName: this.config.embeddingModelName,
      };
      this.testResult = await firstValueFrom(this.settingsService.testConnection(req));
    } catch {
      this.testResult = { success: false, message: 'Connection test failed' };
    } finally {
      this.isTesting = false;
    }
  }

  async reindex() {
    this.isReindexing = true;
    try {
      const result = await firstValueFrom(this.settingsService.reindexEmbeddings());
      this.saveMessage = result.message;
    } catch {
      this.saveMessage = 'Failed to start re-embedding';
    } finally {
      this.isReindexing = false;
    }
  }
}
