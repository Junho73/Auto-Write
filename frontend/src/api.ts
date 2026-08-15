import type {
  ApiKeyStatus,
  AutomationResult,
  BlogPost,
  PostHistoryEntry,
  ScheduledJob,
  StylePreset,
  TopicSuggestion,
} from './types';

export const BACKEND_URL = 'http://localhost:8091';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BACKEND_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.error || `요청 실패 (${response.status})`);
  }
  return data as T;
}

export function fetchStylePresets(): Promise<StylePreset[]> {
  return request('/api/style-presets');
}

export function fetchMockPosts(): Promise<BlogPost[]> {
  return request('/mock-blog/api/posts');
}

export function generatePost(topic: string, stylePresetId: string, aiModel: string): Promise<Record<string, string>> {
  return request('/api/blog/generate', {
    method: 'POST',
    body: JSON.stringify({ topic, stylePresetId, aiModel }),
  });
}

export function autoPost(payload: {
  topic: string;
  stylePresetId: string;
  aiModel: string;
  title: string;
  tags: string;
  content: string;
  target: string;
}): Promise<AutomationResult> {
  return request('/api/blog/post', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchSchedules(): Promise<ScheduledJob[]> {
  return request('/api/schedules');
}

export function createSchedule(payload: {
  topic: string;
  stylePresetId: string;
  aiModel: string;
  target: string;
  scheduleType: string;
  runAt?: string;
  cronExpression?: string;
}): Promise<ScheduledJob> {
  return request('/api/schedules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function setScheduleEnabled(id: number, enabled: boolean): Promise<ScheduledJob> {
  return request(`/api/schedules/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled }),
  });
}

export function deleteSchedule(id: number): Promise<void> {
  return request(`/api/schedules/${id}`, { method: 'DELETE' });
}

export function fetchHistory(): Promise<PostHistoryEntry[]> {
  return request('/api/history');
}

export function fetchWeeklyTopics(): Promise<TopicSuggestion[]> {
  return request('/api/topics/weekly');
}

export function markPublished(id: number, publishedUrl?: string): Promise<PostHistoryEntry> {
  return request(`/api/history/${id}/mark-published`, {
    method: 'POST',
    body: JSON.stringify(publishedUrl ? { publishedUrl } : {}),
  });
}

const TISTORY_URL_KEY = 'tistoryWriteUrl';

export function getTistoryWriteUrl(): string {
  return localStorage.getItem(TISTORY_URL_KEY) || '';
}

export function setTistoryWriteUrl(url: string): void {
  localStorage.setItem(TISTORY_URL_KEY, url);
}

const DEFAULT_STYLE_KEY = 'defaultStylePresetId';
const DEFAULT_MODEL_KEY = 'defaultAiModel';

export function getDefaultStylePresetId(): string {
  return localStorage.getItem(DEFAULT_STYLE_KEY) || '';
}

export function setDefaultStylePresetId(id: string): void {
  localStorage.setItem(DEFAULT_STYLE_KEY, id);
}

export function getDefaultAiModel(): string {
  return localStorage.getItem(DEFAULT_MODEL_KEY) || 'claude-haiku-4-5';
}

export function setDefaultAiModel(model: string): void {
  localStorage.setItem(DEFAULT_MODEL_KEY, model);
}

export function fetchApiKeyStatus(): Promise<ApiKeyStatus> {
  return request('/api/settings/api-key');
}

export function saveApiKey(apiKey: string): Promise<ApiKeyStatus> {
  return request('/api/settings/api-key', {
    method: 'POST',
    body: JSON.stringify({ apiKey }),
  });
}
