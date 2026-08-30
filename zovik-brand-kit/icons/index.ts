/**
 * Zovik Icon Registry & Lucide-compatible Icon Wrapper
 */

export type ZovikStatus =
  | 'queued'
  | 'pending'
  | 'running'
  | 'success'
  | 'failed'
  | 'skipped'
  | 'retry'
  | 'offline';

export type ZovikCoreIcon =
  | 'dag'
  | 'runner'
  | 'docker'
  | 'terminal'
  | 'pipeline'
  | 'logs'
  | 'secrets'
  | 'queue'
  | 'artifacts';

export interface IconProps {
  name: ZovikStatus | ZovikCoreIcon;
  size?: number;
  color?: string;
  className?: string;
}

export const ZOVIK_STATUS_CONFIG: Record<
  ZovikStatus,
  { label: string; color: string; bg: string; border: string; symbol: string }
> = {
  success: {
    label: 'Успешно',
    color: '#10B981',
    bg: 'rgba(16, 185, 129, 0.1)',
    border: 'rgba(16, 185, 129, 0.3)',
    symbol: '[✓]',
  },
  running: {
    label: 'Выполняется',
    color: '#3B82F6',
    bg: 'rgba(59, 130, 246, 0.12)',
    border: 'rgba(59, 130, 246, 0.35)',
    symbol: '[◐]',
  },
  failed: {
    label: 'Ошибка',
    color: '#EF4444',
    bg: 'rgba(239, 68, 68, 0.1)',
    border: 'rgba(239, 68, 68, 0.3)',
    symbol: '[✕]',
  },
  pending: {
    label: 'Ожидание',
    color: '#EAB308',
    bg: 'rgba(234, 179, 8, 0.1)',
    border: 'rgba(234, 179, 8, 0.3)',
    symbol: '[⋯]',
  },
  queued: {
    label: 'В очереди',
    color: '#A1A1AA',
    bg: 'rgba(161, 161, 170, 0.08)',
    border: 'rgba(161, 161, 170, 0.2)',
    symbol: '[○]',
  },
  skipped: {
    label: 'Пропущен',
    color: '#6B7280',
    bg: 'rgba(107, 114, 128, 0.08)',
    border: 'rgba(107, 114, 128, 0.2)',
    symbol: '[⊘]',
  },
  retry: {
    label: 'Повтор',
    color: '#F97316',
    bg: 'rgba(249, 115, 22, 0.1)',
    border: 'rgba(249, 115, 22, 0.3)',
    symbol: '[↻]',
  },
  offline: {
    label: 'Оффлайн',
    color: '#52525B',
    bg: 'rgba(82, 82, 91, 0.1)',
    border: 'rgba(82, 82, 91, 0.25)',
    symbol: '[—]',
  },
};
