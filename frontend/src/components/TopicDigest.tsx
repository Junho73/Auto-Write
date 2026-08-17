import { useState } from 'react';
import { Newspaper, RefreshCw } from 'lucide-react';
import { fetchWeeklyTopics } from '../api';
import type { TopicSuggestion } from '../types';
import './components.css';

interface Props {
  onSelect: (topic: string) => void;
}

export default function TopicDigest({ onSelect }: Props) {
  const [topics, setTopics] = useState<TopicSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedTitle, setSelectedTitle] = useState<string | null>(null);

  const handleFetch = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchWeeklyTopics();
      setTopics(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '토픽을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (topic: TopicSuggestion) => {
    setSelectedTitle(topic.title);
    onSelect(topic.title);
  };

  return (
    <section className="glass-card panel">
      <h2 className="panel-title">
        <Newspaper size={17} /> 이번 주 토픽
      </h2>
      <p className="panel-subtitle">
        Claude 웹 검색으로 이번 주 개발/기술 주요 토픽 5개를 찾아옵니다. 하나 고르면 아래 주제란에 채워집니다.
      </p>
      <button className="btn btn-primary" onClick={handleFetch} disabled={loading} style={{ marginBottom: topics.length > 0 ? '1rem' : 0 }}>
        {loading ? (
          <>
            <RefreshCw className="animate-spin" size={15} /> 검색 중... (몇 분 정도 걸릴 수 있어요)
          </>
        ) : (
          '이번 주 토픽 불러오기'
        )}
      </button>

      {error && <p className="list-item-meta" style={{ color: 'var(--error)' }}>{error}</p>}

      {topics.length === 0 && !loading && !error && (
        <p className="empty-state">아직 불러온 토픽이 없습니다.</p>
      )}

      {topics.map((topic, index) => (
        <div
          key={index}
          className="list-item"
          style={{ cursor: 'pointer', borderColor: selectedTitle === topic.title ? 'var(--primary)' : undefined }}
          onClick={() => handleSelect(topic)}
        >
          <div className="list-item-header">
            <span className="list-item-title">{topic.title}</span>
            {selectedTitle === topic.title && <span className="badge badge-success">선택됨</span>}
          </div>
          <span className="list-item-meta">{topic.summary}</span>
          {topic.sourceUrl && (
            <a
              href={topic.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="list-item-meta"
              onClick={(e) => e.stopPropagation()}
              style={{ color: 'var(--primary)' }}
            >
              출처 링크
            </a>
          )}
        </div>
      ))}
    </section>
  );
}
