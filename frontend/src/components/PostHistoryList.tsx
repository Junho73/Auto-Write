import { useState } from 'react';
import { BACKEND_URL, getTistoryWriteUrl, markPublished } from '../api';
import type { PostHistoryEntry } from '../types';
import './components.css';

interface Props {
  history: PostHistoryEntry[];
  onChanged?: () => void;
}

function modelLabel(aiModel: string): string {
  return aiModel === 'claude-sonnet-5' ? 'Sonnet 5' : 'Haiku 4.5';
}

function badgeFor(entry: PostHistoryEntry) {
  if (entry.status === 'SUCCESS') return <span className="badge badge-success">발행 완료</span>;
  if (entry.status === 'PENDING_FILL') return <span className="badge badge-warning">확장 프로그램 대기 중</span>;
  if (entry.status === 'FILLED') return <span className="badge badge-warning">채워짐 — 출간하기만 누르면 됩니다</span>;
  return <span className="badge badge-error">실패</span>;
}

function writePageUrl(target: string): string | null {
  if (target === 'VELOG') return 'https://velog.io/write';
  if (target === 'TISTORY') return getTistoryWriteUrl() || null;
  return null;
}

export default function PostHistoryList({ history, onChanged }: Props) {
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const [markingId, setMarkingId] = useState<number | null>(null);

  const handleCopy = async (entry: PostHistoryEntry) => {
    const text = `${entry.title}\n\n${entry.content}`;
    await navigator.clipboard.writeText(text);
    setCopiedId(entry.id);
    setTimeout(() => setCopiedId((current) => (current === entry.id ? null : current)), 2000);
  };

  const handleMarkPublished = async (entry: PostHistoryEntry) => {
    setMarkingId(entry.id);
    try {
      await markPublished(entry.id);
      onChanged?.();
    } finally {
      setMarkingId(null);
    }
  };

  return (
    <>
      {history.length === 0 ? (
        <p className="empty-state">아직 발행 이력이 없습니다.</p>
      ) : (
        history.map((entry) => {
          const isExtensionTarget = entry.target === 'VELOG' || entry.target === 'TISTORY';
          const writeUrl = isExtensionTarget ? writePageUrl(entry.target) : null;
          const awaitingPublish = isExtensionTarget && (entry.status === 'PENDING_FILL' || entry.status === 'FILLED');

          return (
            <div className="list-item" key={entry.id}>
              <div className="list-item-header">
                <span className="list-item-title">{entry.title}</span>
                <div className="list-item-actions">
                  {badgeFor(entry)}
                  <span className={`badge ${isExtensionTarget ? 'badge-warning' : 'badge-muted'}`}>
                    {entry.target}
                  </span>
                </div>
              </div>
              <span className="list-item-meta">
                {entry.startedAt ? new Date(entry.startedAt).toLocaleString('ko-KR') : ''}
                {entry.aiModel && (
                  <>
                    {' '}
                    · <span className="badge badge-muted">{modelLabel(entry.aiModel)}</span>
                  </>
                )}
              </span>
              {entry.status === 'SUCCESS' && entry.publishedUrl && (
                <a href={entry.publishedUrl} target="_blank" rel="noopener noreferrer" className="list-item-meta" style={{ color: 'var(--primary)' }}>
                  발행된 글 보기
                </a>
              )}
              <div className="list-item-actions">
                {isExtensionTarget && (
                  <>
                    <button className="btn btn-sm" onClick={() => handleCopy(entry)}>
                      {copiedId === entry.id ? '복사됨!' : '본문 복사하기'}
                    </button>
                    {writeUrl && (
                      <a href={writeUrl} target="_blank" rel="noopener noreferrer" className="btn btn-sm">
                        {entry.target === 'VELOG' ? 'Velog' : 'Tistory'} 글쓰기 열기
                      </a>
                    )}
                    {awaitingPublish && (
                      <button className="btn btn-sm" onClick={() => handleMarkPublished(entry)} disabled={markingId === entry.id}>
                        {markingId === entry.id ? '표시 중...' : '발행 완료로 표시'}
                      </button>
                    )}
                  </>
                )}
                {entry.screenshotUrl && (
                  <a
                    href={`${BACKEND_URL}${entry.screenshotUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-sm"
                  >
                    스크린샷 보기
                  </a>
                )}
              </div>
            </div>
          );
        })
      )}
    </>
  );
}
