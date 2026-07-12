import { useEffect, useState } from 'react';

interface CountdownTimerProps {
  endsAt: Date;
  className?: string;
}

export function CountdownTimer({ endsAt, className = '' }: CountdownTimerProps) {
  const [secondsLeft, setSecondsLeft] = useState(getSecondsLeft(endsAt));

  useEffect(() => {
    const interval = setInterval(() => {
      setSecondsLeft(getSecondsLeft(endsAt));
    }, 1000);
    return () => clearInterval(interval);
  }, [endsAt]);

  if (secondsLeft <= 0) {
    return (
      <span className={`pill-red tabular-nums font-mono ${className}`}>
        Ended
      </span>
    );
  }

  const hours = Math.floor(secondsLeft / 3600);
  const minutes = Math.floor((secondsLeft % 3600) / 60);
  const seconds = secondsLeft % 60;

  const displayTime =
    hours > 0
      ? `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
      : `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  let stateClass = 'countdown-neutral';
  if (secondsLeft <= 120) stateClass = 'countdown-danger pulse-ring';
  else if (secondsLeft <= 300) stateClass = 'countdown-warning';

  return (
    <span
      className={`pill tabular-nums font-mono text-xs font-bold px-2.5 py-1 rounded-full ${stateClass} ${className}`}
    >
      {displayTime}
    </span>
  );
}

/** Large countdown clock variant for the detail page */
export function CountdownClock({ endsAt }: { endsAt: Date }) {
  const [secondsLeft, setSecondsLeft] = useState(getSecondsLeft(endsAt));

  useEffect(() => {
    const interval = setInterval(() => {
      setSecondsLeft(getSecondsLeft(endsAt));
    }, 1000);
    return () => clearInterval(interval);
  }, [endsAt]);

  const hours = Math.floor(secondsLeft / 3600);
  const minutes = Math.floor((secondsLeft % 3600) / 60);
  const seconds = secondsLeft % 60;

  const isDanger = secondsLeft <= 120;
  const isWarning = secondsLeft <= 300 && secondsLeft > 120;

  const colorClass = isDanger
    ? 'text-danger'
    : isWarning
    ? 'text-warning'
    : 'text-text-primary';

  return (
    <div className={`text-center ${isDanger ? 'pulse-ring rounded-xl' : ''}`}>
      <div className={`font-mono tabular-nums text-5xl font-bold tracking-tight ${colorClass}`}>
        {String(hours).padStart(2, '0')}:{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
      </div>
      <div className="text-xs text-text-muted mt-1 font-medium uppercase tracking-wider">
        {isDanger
          ? '⚠ Soft-close window active'
          : isWarning
          ? 'Ending soon'
          : 'Time remaining'}
      </div>
    </div>
  );
}

function getSecondsLeft(endsAt: Date): number {
  return Math.max(0, Math.floor((endsAt.getTime() - Date.now()) / 1000));
}
