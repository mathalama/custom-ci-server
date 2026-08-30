import React from 'react';
import { ZOVIK_STATUS_CONFIG, type ZovikStatus } from '../icons';

export interface DagStatusBadgeProps {
  status: ZovikStatus;
  showIcon?: boolean;
  pulse?: boolean;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const DagStatusBadge: React.FC<DagStatusBadgeProps> = ({
  status,
  showIcon = true,
  pulse = false,
  size = 'md',
  className = '',
}) => {
  const config = ZOVIK_STATUS_CONFIG[status] || ZOVIK_STATUS_CONFIG.queued;
  const isRunning = status === 'running';

  const sizeStyles = {
    sm: { padding: '2px 8px', fontSize: '11px', dotSize: 5 },
    md: { padding: '3px 10px', fontSize: '12px', dotSize: 6 },
    lg: { padding: '5px 14px', fontSize: '13.5px', dotSize: 7 },
  }[size];

  return (
    <span
      className={`zovik-dag-badge ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: sizeStyles.padding,
        borderRadius: '9999px',
        backgroundColor: config.bg,
        border: `1px solid ${config.border}`,
        color: config.color,
        fontSize: sizeStyles.fontSize,
        fontWeight: 600,
        fontFamily: "'Inter', sans-serif",
        lineHeight: 1.4,
        letterSpacing: '-0.01em',
        whiteSpace: 'nowrap',
        userSelect: 'none',
      }}
    >
      {/* Animated Dot Indicator */}
      <span
        style={{
          width: sizeStyles.dotSize,
          height: sizeStyles.dotSize,
          borderRadius: '50%',
          backgroundColor: config.color,
          boxShadow: isRunning || pulse ? `0 0 8px ${config.color}` : 'none',
          animation: isRunning || pulse ? 'zovik-pulse 1.8s cubic-bezier(0.4, 0, 0.6, 1) infinite' : 'none',
        }}
      />

      {/* Status Label */}
      <span>{config.label}</span>
    </span>
  );
};
