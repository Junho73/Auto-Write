import { useEffect, useState } from 'react';
import { fetchStylePresets } from '../api';
import type { StylePreset } from '../types';
import './components.css';

interface Props {
  value: string;
  onChange: (id: string) => void;
}

export default function StylePresetPicker({ value, onChange }: Props) {
  const [presets, setPresets] = useState<StylePreset[]>([]);

  useEffect(() => {
    fetchStylePresets()
      .then((list) => {
        setPresets(list);
        if (!value && list.length > 0) {
          onChange(list[0].id);
        }
      })
      .catch(() => setPresets([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="field">
      <label className="field-label">스타일 프리셋</label>
      <div className="pill-row">
        {presets.map((preset) => (
          <button
            key={preset.id}
            type="button"
            className={`pill ${value === preset.id ? 'active' : ''}`}
            title={preset.description}
            onClick={() => onChange(preset.id)}
          >
            {preset.label}
          </button>
        ))}
      </div>
    </div>
  );
}
