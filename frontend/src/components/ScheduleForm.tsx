import { useState } from 'react';
import { createSchedule } from '../api';
import type { AiModel, PostTarget, ScheduleType } from '../types';
import StylePresetPicker from './StylePresetPicker';
import ModelPicker from './ModelPicker';
import PostTargetToggle from './PostTargetToggle';
import './components.css';

const WEEKDAYS: { label: string; value: string }[] = [
  { label: '월', value: 'MON' },
  { label: '화', value: 'TUE' },
  { label: '수', value: 'WED' },
  { label: '목', value: 'THU' },
  { label: '금', value: 'FRI' },
  { label: '토', value: 'SAT' },
  { label: '일', value: 'SUN' },
];

interface Props {
  onCreated: () => void;
}

export default function ScheduleForm({ onCreated }: Props) {
  const [topic, setTopic] = useState('');
  const [stylePresetId, setStylePresetId] = useState('');
  const [aiModel, setAiModel] = useState<AiModel>('claude-haiku-4-5');
  const [target, setTarget] = useState<PostTarget>('MOCK');
  const [scheduleType, setScheduleType] = useState<ScheduleType>('ONCE');
  const [runAt, setRunAt] = useState('');
  const [days, setDays] = useState<string[]>([]);
  const [time, setTime] = useState('09:00');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleDay = (day: string) => {
    setDays((prev) => (prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]));
  };

  const buildCron = () => {
    const [hour, minute] = time.split(':');
    const dayField = days.length > 0 ? days.join(',') : '*';
    return `0 ${Number(minute)} ${Number(hour)} * * ${dayField}`;
  };

  const handleSubmit = async () => {
    setError(null);
    if (!topic.trim()) {
      setError('주제를 입력해주세요.');
      return;
    }
    if (scheduleType === 'ONCE' && !runAt) {
      setError('예약 날짜/시간을 선택해주세요.');
      return;
    }

    setSubmitting(true);
    try {
      await createSchedule({
        topic,
        stylePresetId,
        aiModel,
        target,
        scheduleType,
        runAt: scheduleType === 'ONCE' ? new Date(runAt).toISOString() : undefined,
        cronExpression: scheduleType === 'RECURRING' ? buildCron() : undefined,
      });
      setTopic('');
      onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : '예약 생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="glass-card panel">
      <h2 className="panel-title">🗓️ 자동 포스팅 예약</h2>

      <div className="field">
        <label className="field-label">주제</label>
        <input
          className="text-input"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="예: TypeScript 제네릭 활용법"
        />
      </div>

      <StylePresetPicker value={stylePresetId} onChange={setStylePresetId} />
      <ModelPicker value={aiModel} onChange={setAiModel} />
      <PostTargetToggle value={target} onChange={setTarget} />

      <div className="field">
        <label className="field-label">예약 방식</label>
        <div className="pill-row">
          <button
            type="button"
            className={`pill ${scheduleType === 'ONCE' ? 'active' : ''}`}
            onClick={() => setScheduleType('ONCE')}
          >
            1회 예약
          </button>
          <button
            type="button"
            className={`pill ${scheduleType === 'RECURRING' ? 'active' : ''}`}
            onClick={() => setScheduleType('RECURRING')}
          >
            반복 예약
          </button>
        </div>
      </div>

      {scheduleType === 'ONCE' ? (
        <div className="field">
          <label className="field-label">날짜/시간</label>
          <input
            type="datetime-local"
            className="text-input"
            value={runAt}
            onChange={(e) => setRunAt(e.target.value)}
          />
        </div>
      ) : (
        <>
          <div className="field">
            <label className="field-label">요일 (선택 안 하면 매일)</label>
            <div className="weekday-row">
              {WEEKDAYS.map((d) => (
                <button
                  key={d.value}
                  type="button"
                  className={`weekday-btn ${days.includes(d.value) ? 'active' : ''}`}
                  onClick={() => toggleDay(d.value)}
                >
                  {d.label}
                </button>
              ))}
            </div>
          </div>
          <div className="field">
            <label className="field-label">시간</label>
            <input
              type="time"
              className="text-input"
              value={time}
              onChange={(e) => setTime(e.target.value)}
            />
          </div>
        </>
      )}

      {error && <p className="list-item-meta" style={{ color: 'var(--error)' }}>{error}</p>}

      <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
        {submitting ? '예약 생성 중...' : '예약 추가'}
      </button>
    </section>
  );
}
