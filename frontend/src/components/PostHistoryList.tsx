import { BACKEND_URL } from '../api';
import type { PostHistoryEntry } from '../types';
import './components.css';

interface Props {
  history: PostHistoryEntry[];
}

function badgeFor(entry: PostHistoryEntry) {
  if (entry.status === 'SUCCESS') return <span className="badge badge-success">성공</span>;
  if (entry.failureReason === 'SESSION_EXPIRED') {
    return <span className="badge badge-error">세션 만료 — 재연결 필요</span>;
  }
  if (entry.failureReason === 'SESSION_MISSING') {
    return <span className="badge badge-error">Velog 미연결</span>;
  }
  return <span className="badge badge-error">실패</span>;
}

export default function PostHistoryList({ history }: Props) {
  return (
    <section className="glass-card panel">
      <h2 className="panel-title">📚 발행 이력</h2>
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
            </span>
            <div className="list-item-actions">
              {entry.publishedUrl && (
                <a href={entry.publishedUrl} target="_blank" rel="noopener noreferrer" className="btn btn-sm">
                  발행글 보기
                </a>
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
    </section>
  );
}
