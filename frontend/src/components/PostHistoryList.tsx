import { useState } from 'react';
import { BACKEND_URL } from '../api';
import type { PostHistoryEntry } from '../types';
import './components.css';

interface Props {
  history: PostHistoryEntry[];
}

function modelLabel(aiModel: string): string {
  return aiModel === 'claude-sonnet-5' ? 'Sonnet 5' : 'Haiku 4.5';
}

function badgeFor(entry: PostHistoryEntry) {
  if (entry.status === 'SUCCESS') return <span className="badge badge-success">성공</span>;
  if (entry.status === 'DRAFT_SAVED') return <span className="badge badge-warning">생성 완료 — 복사해서 붙여넣기</span>;
  return <span className="badge badge-error">실패</span>;
}

export default function PostHistoryList({ history }: Props) {
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const handleCopy = async (entry: PostHistoryEntry) => {
    const text = `${entry.title}\n\n${entry.content}`;
    await navigator.clipboard.writeText(text);
    setCopiedId(entry.id);
    setTimeout(() => setCopiedId((current) => (current === entry.id ? null : current)), 2000);
  };

  return (
    <>
      {history.length === 0 ? (
        <p className="empty-state">아직 발행 이력이 없습니다.</p>
      ) : (
        history.map((entry) => (
          <div className="list-item" key={entry.id}>
            <div className="list-item-header">
              <span className="list-item-title">{entry.title}</span>
              <div className="list-item-actions">
                {badgeFor(entry)}
                <span className={`badge ${entry.target === 'VELOG' ? 'badge-warning' : 'badge-muted'}`}>
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
            <div className="list-item-actions">
              {entry.target === 'VELOG' && entry.status === 'DRAFT_SAVED' && (
                <>
                  <button className="btn btn-sm" onClick={() => handleCopy(entry)}>
                    {copiedId === entry.id ? '복사됨!' : '본문 복사하기'}
                  </button>
                  <a href="https://velog.io/write" target="_blank" rel="noopener noreferrer" className="btn btn-sm">
                    Velog 새 글 작성 열기
                  </a>
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
        ))
      )}
    </>
  );
}
