import type { AiModel } from '../types';
import './components.css';

const MODELS: { id: AiModel; label: string }[] = [
  { id: 'claude-haiku-4-5', label: 'Haiku 4.5 (빠름·저렴)' },
  { id: 'claude-sonnet-5', label: 'Sonnet 5 (고품질)' },
];

interface Props {
  value: AiModel;
  onChange: (model: AiModel) => void;
}

export default function ModelPicker({ value, onChange }: Props) {
  return (
    <div className="field">
      <label className="field-label">AI 모델</label>
      <div className="pill-row">
        {MODELS.map((m) => (
          <button
            key={m.id}
            type="button"
            className={`pill ${value === m.id ? 'active' : ''}`}
            onClick={() => onChange(m.id)}
          >
            {m.label}
          </button>
        ))}
      </div>
    </div>
  );
}
