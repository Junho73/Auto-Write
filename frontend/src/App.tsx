import { useState, useEffect } from 'react';
import {
  Sparkles,
  Send,
  PenTool,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  Copy,
  ExternalLink,
  CalendarClock,
  ChevronDown,
  History,
  Settings as SettingsIcon,
} from 'lucide-react';
import {
  BACKEND_URL,
  autoPost,
  fetchHistory,
  fetchSchedules,
  generatePost,
  getTistoryWriteUrl,
  setTistoryWriteUrl,
  getDefaultStylePresetId,
  getDefaultAiModel,
} from './api';
import type { AiModel, AutomationResult, PostHistoryEntry, PostTarget, ScheduledJob } from './types';
import StylePresetPicker from './components/StylePresetPicker';
import ModelPicker from './components/ModelPicker';
import PostTargetToggle from './components/PostTargetToggle';
import TopicDigest from './components/TopicDigest';
import ScheduleForm from './components/ScheduleForm';
import ScheduleList from './components/ScheduleList';
import PostHistoryList from './components/PostHistoryList';
import SettingsPanel from './components/SettingsPanel';

function initialView(): 'dashboard' | 'settings' {
  return new URLSearchParams(window.location.search).get('view') === 'settings' ? 'settings' : 'dashboard';
}

export default function App() {
  const [view, setView] = useState<'dashboard' | 'settings'>(initialView);
  const [topic, setTopic] = useState('');
  const [stylePresetId, setStylePresetId] = useState(getDefaultStylePresetId);
  const [aiModel, setAiModel] = useState<AiModel>(getDefaultAiModel() as AiModel);
  const [target, setTarget] = useState<PostTarget>('MOCK');
  const [title, setTitle] = useState('');
  const [tags, setTags] = useState('');
  const [content, setContent] = useState('');

  const [isLoadingAi, setIsLoadingAi] = useState(false);
  const [isLoadingPost, setIsLoadingPost] = useState(false);
  const [copied, setCopied] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [screenshotUrl, setScreenshotUrl] = useState<string | null>(null);
  const [genError, setGenError] = useState<string | null>(null);
  const [genSuccess, setGenSuccess] = useState<string | null>(null);
  const [postError, setPostError] = useState<string | null>(null);
  const [postSuccess, setPostSuccess] = useState<string | null>(null);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [tistoryUrl, setTistoryUrl] = useState(getTistoryWriteUrl);

  const [schedules, setSchedules] = useState<ScheduledJob[]>([]);
  const [history, setHistory] = useState<PostHistoryEntry[]>([]);

  const refreshSchedules = () => {
    fetchSchedules().then(setSchedules).catch(() => {});
  };

  const refreshHistory = () => {
    fetchHistory().then(setHistory).catch(() => {});
  };

  useEffect(() => {
    refreshSchedules();
    refreshHistory();
  }, []);

  // While a Velog/Tistory post is waiting on the extension (queued or filled but
  // not yet published), poll so the badge updates on its own once the extension
  // reports back — otherwise the only way to see it change is a manual reload.
  const isWaitingOnExtension = history.some(
    (entry) => entry.target !== 'MOCK' && (entry.status === 'PENDING_FILL' || entry.status === 'FILLED')
  );
  useEffect(() => {
    if (!isWaitingOnExtension) return;
    const interval = setInterval(refreshHistory, 5000);
    return () => clearInterval(interval);
  }, [isWaitingOnExtension]);

  const handleGeneratePost = async () => {
    if (!topic.trim()) {
      setGenError('블로그 주제를 입력해주세요.');
      return;
    }

    setIsLoadingAi(true);
    setGenError(null);
    setGenSuccess(null);

    try {
      const data = await generatePost(topic, stylePresetId, aiModel);
      setTitle(data.title);
      setTags(data.tags);
      setContent(data.content);
      setGenSuccess('AI 블로그 글이 성공적으로 생성되었습니다!');
    } catch (err) {
      setGenError(err instanceof Error ? err.message : '백엔드 서버와 통신할 수 없습니다. 서버가 켜져 있는지 확인하세요.');
    } finally {
      setIsLoadingAi(false);
    }
  };

  const handleCopy = async () => {
    await navigator.clipboard.writeText(`${title}\n\n${content}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleTistoryUrlChange = (value: string) => {
    setTistoryUrl(value);
    setTistoryWriteUrl(value);
  };

  const openSettings = () => {
    const url = new URL(window.location.href);
    url.searchParams.set('view', 'settings');
    window.history.pushState({}, '', url);
    setView('settings');
  };

  const closeSettings = () => {
    const url = new URL(window.location.href);
    url.searchParams.delete('view');
    window.history.pushState({}, '', url);
    setView('dashboard');
  };

  const handleAutoPost = async () => {
    if (!title.trim() || !content.trim()) {
      setPostError('제목과 본문 내용이 채워져 있어야 합니다.');
      return;
    }

    setIsLoadingPost(true);
    setPostError(null);
    setPostSuccess(null);
    setLogs([]);
    setScreenshotUrl(null);

    try {
      const data: AutomationResult = await autoPost({ topic, stylePresetId, aiModel, title, tags, content, target });
      setLogs(data.logs);

      if (data.success) {
        if (data.screenshotUrl) {
          setScreenshotUrl(`${BACKEND_URL}${data.screenshotUrl}`);
        }
        setPostSuccess(
          target === 'MOCK'
            ? '모의 블로그에 자동 포스팅이 완료되었습니다!'
            : `확장 프로그램 대기열에 추가되었습니다. ${target === 'VELOG' ? 'Velog' : 'Tistory'} 글쓰기 페이지를 열면 자동으로 채워집니다 (확장 프로그램 설치 필요).`
        );
        refreshHistory();
      } else {
        setPostError('작업 중 에러가 발생했습니다. 아래 로그를 확인하세요.');
        refreshHistory();
      }
    } catch (err) {
      setPostError(err instanceof Error ? err.message : '요청 중 네트워크 오류가 발생했습니다.');
      setLogs((prev) => [...prev, '오류: 백엔드 서버 연결 끊김.']);
    } finally {
      setIsLoadingPost(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', padding: '2.5rem 1.5rem 4rem' }}>
      <header style={{ maxWidth: '760px', margin: '0 auto 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '1.6rem', fontWeight: 800, margin: 0 }}>
            Blog Autowriter
          </h1>
          <p style={{ color: 'var(--text-secondary)', margin: '0.3rem 0 0', fontSize: '0.85rem' }}>
            이번 주 토픽을 찾고, 글을 생성하고, 확장 프로그램으로 Velog·Tistory에 반자동 발행하세요.
          </p>
        </div>
        <div className="pill-row">
          <button className="btn btn-sm" onClick={openSettings}>
            <SettingsIcon size={14} /> 설정
          </button>
          <a href={`${BACKEND_URL}/mock-blog/posts`} target="_blank" rel="noopener noreferrer" className="btn btn-sm">
            모의 블로그 보기 <ExternalLink size={14} />
          </a>
        </div>
      </header>

      {view === 'settings' && <SettingsPanel onBack={closeSettings} />}

      {view === 'dashboard' && (
      <main style={{ maxWidth: '760px', margin: '0 auto' }}>
        <TopicDigest onSelect={setTopic} />

        <section className="glass-card panel">
          <h2 className="panel-title">
            <span className="step-badge">1</span> <Sparkles size={17} /> 글감 및 본문 생성
          </h2>
          <div className="field">
            <label className="field-label">주제</label>
            <input
              type="text"
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder="위 토픽 중 하나를 고르거나 직접 입력하세요"
              className="text-input"
            />
          </div>
          <StylePresetPicker value={stylePresetId} onChange={setStylePresetId} />
          <ModelPicker value={aiModel} onChange={setAiModel} />
          <button onClick={handleGeneratePost} disabled={isLoadingAi || isLoadingPost} className="btn btn-primary">
            {isLoadingAi ? (
              <>
                <RefreshCw className="animate-spin" size={16} /> 생성 중...
              </>
            ) : (
              <>
                <Sparkles size={16} /> 생성하기
              </>
            )}
          </button>

          {(genError || genSuccess) && (
            <div className="inline-status" style={{ color: genError ? '#fca5a5' : 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem' }}>
              {genError ? <AlertCircle size={17} /> : <CheckCircle size={17} />}
              <span>{genError || genSuccess}</span>
            </div>
          )}
        </section>

        <section className="glass-card panel">
          <h2 className="panel-title">
            <span className="step-badge">2</span> <PenTool size={17} /> 검토 및 발행
          </h2>
          <div className="field">
            <label className="field-label">제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="블로그 제목이 여기에 생성됩니다..."
              className="text-input"
            />
          </div>
          <div className="field">
            <label className="field-label">태그</label>
            <input
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="태그1, 태그2"
              className="text-input"
            />
          </div>
          <div className="field">
            <label className="field-label">본문 내용</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="본문 내용이 여기에 생성됩니다. 마크다운 또는 텍스트 형식을 지원합니다..."
              className="text-input"
              style={{ minHeight: '220px', fontFamily: 'monospace', resize: 'vertical' }}
            />
          </div>

          <PostTargetToggle value={target} onChange={setTarget} />

          {target === 'TISTORY' && (
            <div className="field">
              <label className="field-label">티스토리 새 글 작성 페이지 URL (한 번만 설정)</label>
              <input
                type="text"
                value={tistoryUrl}
                onChange={(e) => handleTistoryUrlChange(e.target.value)}
                placeholder="예: https://내블로그.tistory.com/manage/newpost/"
                className="text-input"
              />
            </div>
          )}

          {target !== 'MOCK' && (
            <div className="pill-row" style={{ marginBottom: '1rem' }}>
              <button className="btn" onClick={handleCopy} disabled={!title || !content}>
                <Copy size={15} /> {copied ? '복사됨!' : '복사하기 (수동)'}
              </button>
              {target === 'VELOG' ? (
                <a href="https://velog.io/write" target="_blank" rel="noopener noreferrer" className="btn">
                  Velog 새 글 작성 열기 <ExternalLink size={15} />
                </a>
              ) : (
                <a
                  href={tistoryUrl || undefined}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn"
                  aria-disabled={!tistoryUrl}
                  onClick={(e) => { if (!tistoryUrl) e.preventDefault(); }}
                  title={tistoryUrl ? undefined : '위에 티스토리 글쓰기 URL을 먼저 입력해주세요'}
                >
                  Tistory 새 글 작성 열기 <ExternalLink size={15} />
                </a>
              )}
            </div>
          )}

          <button
            onClick={handleAutoPost}
            disabled={isLoadingPost || isLoadingAi || !title || !content}
            className="btn btn-primary"
          >
            {isLoadingPost ? (
              <>
                <RefreshCw className="animate-spin" size={18} /> 처리 중...
              </>
            ) : (
              <>
                <Send size={18} /> {target === 'MOCK' ? '모의 블로그에 지금 포스팅' : '확장 프로그램 대기열에 추가'}
              </>
            )}
          </button>

          {(postError || postSuccess) && (
            <div className="inline-status" style={{ color: postError ? '#fca5a5' : 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem' }}>
              {postError ? <AlertCircle size={17} /> : <CheckCircle size={17} />}
              <span>{postError || postSuccess}</span>
            </div>
          )}

          {logs.length > 0 && (
            <div className="inline-status">
              {logs.map((log, index) => (
                <div key={index} className="log-line">▶ {log}</div>
              ))}
            </div>
          )}

          {screenshotUrl && (
            <div className="inline-status">
              <a href={screenshotUrl} target="_blank" rel="noopener noreferrer">
                <img src={screenshotUrl} alt="포스팅 캡처" style={{ width: '100%', borderRadius: '10px' }} />
              </a>
            </div>
          )}
        </section>

        <section className="glass-card panel">
          <button className="accordion-toggle" onClick={() => setScheduleOpen((v) => !v)}>
            <h2 className="panel-title">
              <CalendarClock size={17} /> 자동 포스팅 예약
              {schedules.length > 0 && <span className="badge badge-muted">{schedules.length}</span>}
            </h2>
            <ChevronDown className={`accordion-chevron ${scheduleOpen ? 'open' : ''}`} size={18} />
          </button>
          {scheduleOpen && (
            <div className="accordion-body">
              <ScheduleForm onCreated={refreshSchedules} />
              <ScheduleList jobs={schedules} onChanged={refreshSchedules} />
            </div>
          )}
        </section>

        <section className="glass-card panel">
          <h2 className="panel-title">
            <History size={17} /> 발행 이력
            {isWaitingOnExtension && (
              <span className="badge badge-muted" style={{ fontWeight: 400 }}>
                <RefreshCw size={11} className="animate-spin" /> 실시간 확인 중
              </span>
            )}
          </h2>
          <PostHistoryList history={history} onChanged={refreshHistory} />
        </section>
      </main>
      )}
    </div>
  );
}
