import { deleteSchedule, setScheduleEnabled } from '../api';
import type { ScheduledJob } from '../types';
import './components.css';

interface Props {
  jobs: ScheduledJob[];
  onChanged: () => void;
}

function describeSchedule(job: ScheduledJob): string {
  if (job.scheduleType === 'ONCE') {
    return job.runAt ? `1회 · ${new Date(job.runAt).toLocaleString('ko-KR')}` : '1회';
  }
  return `반복 · ${job.cronExpression}`;
}

function modelLabel(aiModel: string): string {
  return aiModel === 'claude-sonnet-5' ? 'Sonnet 5' : 'Haiku 4.5';
}

function statusBadge(job: ScheduledJob) {
  if (job.lastRunStatus === 'SUCCESS') return <span className="badge badge-success">성공</span>;
  if (job.lastRunStatus === 'DRAFT_SAVED') return <span className="badge badge-warning">생성 완료 — 이력에서 복사하세요</span>;
  if (job.lastRunStatus === 'FAILURE') return <span className="badge badge-error">{job.lastRunError || '실패'}</span>;
  return <span className="badge badge-muted">대기중</span>;
}

export default function ScheduleList({ jobs, onChanged }: Props) {
  const handleToggle = async (job: ScheduledJob) => {
    await setScheduleEnabled(job.id, !job.enabled);
    onChanged();
  };

  const handleDelete = async (job: ScheduledJob) => {
    await deleteSchedule(job.id);
    onChanged();
  };

  return (
    <section className="glass-card panel">
      <h2 className="panel-title">📋 예약 목록</h2>
      {jobs.length === 0 ? (
        <p className="empty-state">등록된 예약이 없습니다.</p>
      ) : (
        jobs.map((job) => (
          <div className="list-item" key={job.id}>
            <div className="list-item-header">
              <span className="list-item-title">{job.topic}</span>
              <div className="list-item-actions">
                {statusBadge(job)}
                <span className={`badge ${job.target === 'VELOG' ? 'badge-warning' : 'badge-muted'}`}>
                  {job.target}
                </span>
              </div>
            </div>
            <span className="list-item-meta">
              {describeSchedule(job)} · <span className="badge badge-muted">{modelLabel(job.aiModel)}</span>
            </span>
            <div className="list-item-actions">
              <button className="btn btn-sm" onClick={() => handleToggle(job)}>
                {job.enabled ? '일시정지' : '재개'}
              </button>
              <button className="btn btn-sm btn-danger" onClick={() => handleDelete(job)}>
                삭제
              </button>
            </div>
          </div>
        ))
      )}
    </section>
  );
}
