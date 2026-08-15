import type { PostTarget } from '../types';
import './components.css';

interface Props {
  value: PostTarget;
  onChange: (target: PostTarget) => void;
}

export default function PostTargetToggle({ value, onChange }: Props) {
  return (
    <div className="field">
      <label className="field-label">포스팅 대상</label>
      <div className="pill-row">
        <button
          type="button"
          className={`pill ${value === 'MOCK' ? 'active' : ''}`}
          onClick={() => onChange('MOCK')}
        >
          모의 블로그 (자동 데모)
        </button>
        <button
          type="button"
          className={`pill ${value === 'VELOG' ? 'active' : ''}`}
          onClick={() => onChange('VELOG')}
        >
          Velog (확장 프로그램)
        </button>
        <button
          type="button"
          className={`pill ${value === 'TISTORY' ? 'active' : ''}`}
          onClick={() => onChange('TISTORY')}
        >
          Tistory (확장 프로그램)
        </button>
      </div>
    </div>
  );
}
