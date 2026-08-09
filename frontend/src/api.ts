import type {
  AutomationResult,
  BlogPost,
  PostHistoryEntry,
  ScheduledJob,
  StylePreset,
  VelogSessionStatus,
} from './types';

export const BACKEND_URL = 'http://localhost:8080';

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

export function generatePost(topic: string, stylePresetId: string): Promise<Record<string, string>> {
  return request('/api/blog/generate', {
    method: 'POST',
    body: JSON.stringify({ topic, stylePresetId }),
  });
}

export function autoPost(payload: {
  topic: string;
  stylePresetId: string;
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

export function fetchVelogSessionStatus(): Promise<VelogSessionStatus> {
  return request('/api/velog/session/status');
}

export function connectVelogSession(): Promise<{ message?: string; error?: string }> {
  return request('/api/velog/session/connect', { method: 'POST' });
}

export function confirmVelogSession(): Promise<{ message?: string; error?: string }> {
  return request('/api/velog/session/confirm', { method: 'POST' });
}

export function cancelVelogSession(): Promise<{ message?: string; error?: string }> {
  return request('/api/velog/session/cancel', { method: 'POST' });
}
