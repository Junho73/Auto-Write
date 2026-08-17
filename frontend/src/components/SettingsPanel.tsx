import { useEffect, useState } from 'react';
import { ArrowLeft, KeyRound, Link2, Sparkles } from 'lucide-react';
import {
  fetchApiKeyStatus,
  saveApiKey,
  getTistoryWriteUrl,
  setTistoryWriteUrl,
  getDefaultStylePresetId,
  setDefaultStylePresetId,
} from '../api';
import type { ApiKeyStatus } from '../types';
import StylePresetPicker from './StylePresetPicker';
import './components.css';

interface Props {
  onBack: () => void;
}

export default function SettingsPanel({ onBack }: Props) {
  const [status, setStatus] = useState<ApiKeyStatus | null>(null);
  const [keyInput, setKeyInput] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  const [tistoryUrl, setTistoryUrlState] = useState(getTistoryWriteUrl);
  const [defaultStyle, setDefaultStyleState] = useState(getDefaultStylePresetId);

  const refreshStatus = () => {
    fetchApiKeyStatus().then(setStatus).catch(() => setStatus(null));
  };

  useEffect(() => {
    refreshStatus();
  }, []);

  const handleSaveKey = async () => {
    if (!keyInput.trim()) return;
    setSaving(true);
    setSaveMessage(null);
    try {
      const updated = await saveApiKey(keyInput.trim());
      setStatus(updated);
      setKeyInput('');
      setSaveMessage('저장되었습니다.');
    } catch (err) {
      setSaveMessage(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: '760px', margin: '0 auto' }}>
      <button className="btn btn-sm" onClick={onBack} style={{ marginBottom: '1.25rem' }}>
        <ArrowLeft size={14} /> 대시보드로
      </button>

      <section className="glass-card panel">
        <h2 className="panel-title">
          <KeyRound size={17} /> Claude API 키
        </h2>
        <p className="panel-subtitle">
          {status
            ? status.configured
              ? `설정됨 (${status.source === 'env' ? '환경 변수' : '저장된 키'}) — ${status.masked}`
              : '아직 설정되지 않았습니다.'
            : '상태 확인 중...'}
        </p>
        <div className="field">
          <label className="field-label">새 API 키 입력 (sk-ant-...)</label>
          <input
            type="password"
            value={keyInput}
            onChange={(e) => setKeyInput(e.target.value)}
            placeholder="sk-ant-api03-..."
            className="text-input"
          />
        </div>
        <button className="btn btn-primary" onClick={handleSaveKey} disabled={saving || !keyInput.trim()}>
          {saving ? '저장 중...' : '저장'}
        </button>
        {saveMessage && (
          <div className="inline-status" style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            {saveMessage}
          </div>
        )}
        <p className="panel-subtitle" style={{ marginTop: '1rem' }}>
          환경 변수(ANTHROPIC_API_KEY)가 설정되어 있으면 항상 그쪽이 우선 사용됩니다. 여기서 저장한 키는
          로컬 파일(backend/data/anthropic-api-key.txt, git에 올라가지 않음)에만 저장됩니다.
        </p>
      </section>

      <section className="glass-card panel">
        <h2 className="panel-title">
          <Link2 size={17} /> Tistory 새 글 작성 URL
        </h2>
        <div className="field">
          <label className="field-label">한 번만 설정해두면 확장 프로그램/발행 버튼에서 재사용됩니다</label>
          <input
            type="text"
            value={tistoryUrl}
            onChange={(e) => {
              setTistoryUrlState(e.target.value);
              setTistoryWriteUrl(e.target.value);
            }}
            placeholder="예: https://내블로그.tistory.com/manage/newpost/"
            className="text-input"
          />
        </div>
      </section>

      <section className="glass-card panel">
        <h2 className="panel-title">
          <Sparkles size={17} /> 기본 작성 스타일
        </h2>
        <p className="panel-subtitle">
          글감 생성 화면을 열 때 기본으로 선택되어 있을 값입니다. (AI 모델은 항상 Haiku 4.5 —
          Sonnet은 토큰을 너무 많이 써서 뺐습니다.)
        </p>
        <StylePresetPicker
          value={defaultStyle}
          onChange={(id) => {
            setDefaultStyleState(id);
            setDefaultStylePresetId(id);
          }}
        />
      </section>
    </div>
  );
}
