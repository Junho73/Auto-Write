import { useState, useEffect } from 'react';
import {
  Sparkles,
  Send,
  Terminal,
  Image as ImageIcon,
  BookOpen,
  PenTool,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  Copy,
  ExternalLink
} from 'lucide-react';
import { BACKEND_URL, autoPost, fetchHistory, fetchMockPosts, fetchSchedules, generatePost } from './api';
import type { AiModel, AutomationResult, BlogPost, PostHistoryEntry, PostTarget, ScheduledJob } from './types';
import StylePresetPicker from './components/StylePresetPicker';
import ModelPicker from './components/ModelPicker';
import PostTargetToggle from './components/PostTargetToggle';
import TopicDigest from './components/TopicDigest';
import ScheduleForm from './components/ScheduleForm';
import ScheduleList from './components/ScheduleList';
import PostHistoryList from './components/PostHistoryList';

export default function App() {
  const [topic, setTopic] = useState('');
  const [stylePresetId, setStylePresetId] = useState('');
  const [aiModel, setAiModel] = useState<AiModel>('claude-haiku-4-5');
  const [target, setTarget] = useState<PostTarget>('MOCK');
  const [title, setTitle] = useState('');
  const [tags, setTags] = useState('');
  const [content, setContent] = useState('');

  const [isLoadingAi, setIsLoadingAi] = useState(false);
  const [isLoadingPost, setIsLoadingPost] = useState(false);
  const [copied, setCopied] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [screenshotUrl, setScreenshotUrl] = useState<string | null>(null);
  const [mockPosts, setMockPosts] = useState<BlogPost[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [schedules, setSchedules] = useState<ScheduledJob[]>([]);
  const [history, setHistory] = useState<PostHistoryEntry[]>([]);

  const refreshMockPosts = () => {
    fetchMockPosts().then(setMockPosts).catch(() => {});
  };

  const refreshSchedules = () => {
    fetchSchedules().then(setSchedules).catch(() => {});
  };

  const refreshHistory = () => {
    fetchHistory().then(setHistory).catch(() => {});
  };

  useEffect(() => {
    refreshMockPosts();
    refreshSchedules();
    refreshHistory();
  }, []);

  const handleGeneratePost = async () => {
    if (!topic.trim()) {
      setError('블로그 주제를 입력해주세요.');
      return;
    }

    setIsLoadingAi(true);
    setError(null);
    setSuccess(null);

    try {
      const data = await generatePost(topic, stylePresetId, aiModel);
      setTitle(data.title);
      setTags(data.tags);
      setContent(data.content);
      setSuccess('AI 블로그 글이 성공적으로 생성되었습니다!');
    } catch (err) {
      setError(err instanceof Error ? err.message : '백엔드 서버와 통신할 수 없습니다. 서버가 켜져 있는지 확인하세요.');
    } finally {
      setIsLoadingAi(false);
    }
  };

  const handleCopy = async () => {
    await navigator.clipboard.writeText(`${title}\n\n${content}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleAutoPost = async () => {
    if (!title.trim() || !content.trim()) {
      setError('제목과 본문 내용이 채워져 있어야 합니다.');
      return;
    }

    setIsLoadingPost(true);
    setError(null);
    setSuccess(null);
    setLogs(['처리 중...']);
    setScreenshotUrl(null);

    try {
      const data: AutomationResult = await autoPost({ topic, stylePresetId, aiModel, title, tags, content, target });
      setLogs(data.logs);

      if (data.success) {
        if (data.screenshotUrl) {
          setScreenshotUrl(`${BACKEND_URL}${data.screenshotUrl}`);
        }
        setSuccess(
          target === 'VELOG'
            ? '생성 완료! 아래 "복사하기"로 복사한 뒤 Velog 새 글 작성 화면에 붙여넣어주세요.'
            : '모의 블로그에 자동 포스팅이 완료되었습니다! 아래 스크린샷을 확인하세요.'
        );
        refreshMockPosts();
        refreshHistory();
      } else {
        setError('작업 중 에러가 발생했습니다. 로그를 확인하세요.');
        refreshHistory();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '요청 중 네트워크 오류가 발생했습니다.');
      setLogs((prev) => [...prev, '오류: 백엔드 서버 연결 끊김.']);
    } finally {
      setIsLoadingPost(false);
    }
  };

  return (
    <div className="min-h-screen p-6 md:p-12">
      <header className="max-w-7xl mx-auto mb-10 text-center md:text-left flex flex-col md:flex-row justify-between items-center gap-6 border-b border-white/5 pb-8">
        <div>
          <h1 className="text-4xl font-extrabold tracking-tight" style={{ fontFamily: 'Outfit, sans-serif' }}>
            Playwright Blog Autowriter
          </h1>
          <p className="text-gray-400 mt-2 text-sm md:text-base">
            Claude API로 이번 주 토픽을 찾고 블로그 글을 생성하는 대시보드 — Velog 발행은 복사해서 직접
          </p>
        </div>
        <div className="flex gap-4">
          <a
            href={`${BACKEND_URL}/mock-blog/posts`}
            target="_blank"
            rel="noopener noreferrer"
            className="btn"
          >
            모의 블로그 바로가기 <ExternalLink size={16} />
          </a>
        </div>
      </header>

      <main className="max-w-7xl mx-auto" style={{ display: 'flex', flexWrap: 'wrap', gap: '2rem' }}>
        <div style={{ flex: '1 1 560px' }}>
          <TopicDigest onSelect={setTopic} />

          <section className="glass-card panel">
            <h2 className="panel-title" style={{ color: 'var(--primary)' }}>
              <Sparkles size={20} /> 1. AI 글감 및 본문 자동 생성
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
            <button
              onClick={handleGeneratePost}
              disabled={isLoadingAi || isLoadingPost}
              className="btn btn-primary"
            >
              {isLoadingAi ? (
                <>
                  <RefreshCw className="animate-spin" size={18} /> 생성 중...
                </>
              ) : (
                <>
                  <Sparkles size={18} /> 생성하기
                </>
              )}
            </button>
          </section>

          <section className="glass-card panel">
            <h2 className="panel-title" style={{ color: 'var(--success)' }}>
              <PenTool size={20} /> 2. 포스트 본문 수정 및 검토
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
                style={{ minHeight: '260px', fontFamily: 'monospace', resize: 'vertical' }}
              />
            </div>

            <PostTargetToggle value={target} onChange={setTarget} />

            {target === 'VELOG' && (
              <div className="pill-row" style={{ marginBottom: '1rem' }}>
                <button className="btn" onClick={handleCopy} disabled={!title || !content}>
                  <Copy size={16} /> {copied ? '복사됨!' : '복사하기'}
                </button>
                <a href="https://velog.io/write" target="_blank" rel="noopener noreferrer" className="btn">
                  Velog 새 글 작성 열기 <ExternalLink size={16} />
                </a>
              </div>
            )}

            <button
              onClick={handleAutoPost}
              disabled={isLoadingPost || isLoadingAi || !title || !content}
              className="btn btn-primary pulse-glow"
            >
              {isLoadingPost ? (
                <>
                  <RefreshCw className="animate-spin" size={20} /> 처리 중...
                </>
              ) : (
                <>
                  <Send size={20} /> {target === 'VELOG' ? '이력에 저장' : '모의 블로그에 지금 포스팅'}
                </>
              )}
            </button>
          </section>

          <ScheduleForm onCreated={refreshSchedules} />
          <ScheduleList jobs={schedules} onChanged={refreshSchedules} />
        </div>

        <div style={{ flex: '1 1 420px' }}>
          {(error || success) && (
            <div
              className="panel glass-card"
              style={{
                color: error ? '#fca5a5' : 'var(--success)',
                borderColor: error ? 'rgba(239,68,68,0.3)' : undefined,
              }}
            >
              {error ? <AlertCircle size={20} /> : <CheckCircle size={20} />}
              <span style={{ marginLeft: '0.5rem' }}>{error || success}</span>
            </div>
          )}

          <section className="glass-card panel" style={{ maxHeight: '350px', overflowY: 'auto' }}>
            <h2 className="panel-title" style={{ color: 'var(--primary)' }}>
              <Terminal size={20} /> 실시간 로그
            </h2>
            {logs.length === 0 ? (
              <span className="empty-state">포스팅 버튼을 클릭하면 실시간 동작이 표시됩니다.</span>
            ) : (
              logs.map((log, index) => (
                <div key={index} className="list-item-meta" style={{ marginBottom: '0.3rem' }}>
                  ▶ {log}
                </div>
              ))
            )}
          </section>

          <section className="glass-card panel">
            <h2 className="panel-title">
              <ImageIcon size={20} /> 포스팅 완료 스크린샷
            </h2>
            {screenshotUrl ? (
              <a href={screenshotUrl} target="_blank" rel="noopener noreferrer">
                <img src={screenshotUrl} alt="Playwright Capture" style={{ width: '100%', borderRadius: '12px' }} />
              </a>
            ) : (
              <p className="empty-state">모의 블로그 포스팅이 성공하면 자동 캡쳐본이 이곳에 렌더링됩니다.</p>
            )}
          </section>

          <PostHistoryList history={history} />

          <section className="glass-card panel">
            <div className="list-item-header" style={{ marginBottom: '1rem' }}>
              <h2 className="panel-title" style={{ margin: 0 }}>
                <BookOpen size={20} /> 최근 등록된 모의 블로그 글
              </h2>
              <button className="btn btn-sm" onClick={refreshMockPosts} title="새로고침">
                <RefreshCw size={14} />
              </button>
            </div>
            {mockPosts.length === 0 ? (
              <span className="empty-state">등록된 글이 없습니다.</span>
            ) : (
              mockPosts.map((post, index) => (
                <div key={index} className="list-item">
                  <span className="list-item-title">{post.title}</span>
                  <span className="list-item-meta">{post.content}</span>
                </div>
              ))
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
