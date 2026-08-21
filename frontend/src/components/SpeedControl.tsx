import { NARRATION_CONSTRAINTS } from '../domain/speech';

interface SpeedControlProps {
  value: number;
  onChange: (speed: number) => void;
}

export function SpeedControl({ value, onChange }: SpeedControlProps) {
  return (
    <div className="field-block">
      <div className="field-heading">
        <label className="field-label" htmlFor="speed">
          말하기 속도
        </label>
        <output className="speed-value" htmlFor="speed" aria-live="polite">
          {value.toFixed(1)}×
        </output>
      </div>
      <div className="range-row">
        <span aria-hidden="true">느리게</span>
        <input
          id="speed"
          type="range"
          min={NARRATION_CONSTRAINTS.minSpeed}
          max={NARRATION_CONSTRAINTS.maxSpeed}
          step={NARRATION_CONSTRAINTS.speedStep}
          value={value}
          onChange={(event) => onChange(Number(event.target.value))}
          aria-valuetext={`${value.toFixed(1)}배 속도`}
        />
        <span aria-hidden="true">빠르게</span>
      </div>
    </div>
  );
}
