import { useEffect, useRef, useState } from 'react';
import { cancelVelogSession, confirmVelogSession, connectVelogSession, fetchVelogSessionStatus } from '../api';
import type { VelogSessionStatus } from '../types';
import './components.css';

export default function VelogSessionPanel() {
  const [status, setStatus] = useState<VelogSessionStatus | null>(null);
  const [busyAction, setBusyAction] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const pollRef = useRef<number | null>(null);

  const refresh = () => {
    fetchVelogSessionStatus()
      .then(setStatus)
      .catch(() => setStatus(null));
  };

  useEffect(() => {
    refresh();
    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, []);

  const startPolling = () => {
    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = window.setInterval(refresh, 4000);
  };

  const stopPolling = () => {
    if (pollRef.current) {
      window.clearInterval(pollRef.current);
      pollRef.current = null;
    }
  };

  const handleConnect = async () => {
    setBusyAction(true);
    setMessage(null);
    try {
      const res = await connectVelogSession();
      setMessage(res.message || null);
      startPolling();
      refresh();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : '연결 시작 실패');
    } finally {
      setBusyAction(false);
    }
  };

  const handleConfirm = async () => {
    setBusyAction(true);
    setMessage(null);
    try {
      const res = await confirmVelogSession();
      setMessage(res.message || null);
      stopPolling();
      refresh();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : '세션 저장 실패');
    } finally {
      setBusyAction(false);
    }
  };

  const handleCancel = async () => {
    setBusyAction(true);
    setMessage(null);
    try {
      const res = await cancelVelogSession();
      setMessage(res.message || null);
      stopPolling();
      refresh();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : '취소 실패');
    } finally {
      setBusyAction(false);
    }
  };

  const connecting = !!status?.connectInProgress;

  return (
    <section className="glass-card panel">
      <h2 className="panel-title">🔗 Velog 세션 연결</h2>

      <div className="status-line" style={{ marginBottom: '1rem' }}>
        <span className={`dot ${status?.connected ? 'dot-connected' : 'dot-disconnected'}`} />
        {status?.connected
          ? `연결됨${status.connectedAt ? ` · ${new Date(status.connectedAt).toLocaleString('ko-KR')}` : ''}`
          : '연결된 Velog 세션이 없습니다.'}
      </div>

      {!connecting && (
        <button className="btn btn-primary" onClick={handleConnect} disabled={busyAction || status?.busy}>
          {status?.connected ? 'Velog 재연결하기' : 'Velog 연결하기'}
        </button>
      )}

      {connecting && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
          <p className="list-item-meta">
            새로 열린 브라우저 창에서 Velog 로그인을 완료한 뒤, 아래 "로그인 완료" 버튼을 눌러주세요.
          </p>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="btn btn-primary" onClick={handleConfirm} disabled={busyAction}>
              로그인 완료, 세션 저장
            </button>
            <button className="btn" onClick={handleCancel} disabled={busyAction}>
              취소
            </button>
          </div>
        </div>
      )}

      {message && <p className="list-item-meta" style={{ marginTop: '0.6rem' }}>{message}</p>}
    </section>
  );
}
