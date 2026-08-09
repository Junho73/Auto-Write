export interface BlogPost {
  title: string;
  content: string;
  tags: string;
  createdAt: string;
}

export type PostTarget = 'MOCK' | 'VELOG';
export type ScheduleType = 'ONCE' | 'RECURRING';
export type RunStatus = 'NEVER_RUN' | 'SUCCESS' | 'FAILURE';
export type FailureReason = 'NONE' | 'SESSION_MISSING' | 'SESSION_EXPIRED' | 'AUTOMATION_ERROR';

export interface StylePreset {
  id: string;
  label: string;
  description: string;
}

export interface AutomationResult {
  success: boolean;
  screenshotUrl: string | null;
  publishedUrl: string | null;
  failureReason: FailureReason;
  logs: string[];
}

export interface ScheduledJob {
  id: number;
  topic: string;
  stylePresetId: string;
  target: PostTarget;
  scheduleType: ScheduleType;
  runAt: string | null;
  cronExpression: string | null;
  enabled: boolean;
  lastRunAt: string | null;
  lastRunStatus: RunStatus;
  lastRunError: string | null;
  createdAt: string;
}

export interface PostHistoryEntry {
  id: number;
  scheduledJobId: number | null;
  topic: string;
  stylePresetId: string;
  title: string;
  tags: string;
  content: string;
  target: PostTarget;
  status: RunStatus;
  failureReason: FailureReason;
  screenshotUrl: string | null;
  publishedUrl: string | null;
  logs: string;
  startedAt: string;
  finishedAt: string | null;
}

export interface VelogSessionStatus {
  connected: boolean;
  connectedAt: string | null;
  connectInProgress: boolean;
  busy: boolean;
}
