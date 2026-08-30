import React from 'react';

interface EmptyStateProps {
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
}

/**
 * 1. No Pipelines Empty State
 */
export const NoPipelinesEmptyState: React.FC<EmptyStateProps> = ({
  title = 'Пайплайны не найдены',
  description = 'В этом репозитории ещё не запущен ни один рабочий процесс. Создайте конфигурацию .zovik.yml для начала.',
  actionText = 'Запустить первый пайплайн',
  onAction,
}) => (
  <div style={containerStyle}>
    <svg width="200" height="150" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="20" y="20" width="160" height="110" rx="16" fill="#18181B" stroke="#27272A" strokeWidth="1.5" />
      <!-- Grid background -->
      <path d="M20 55 H180 M20 95 H180 M60 20 V130 M100 20 V130 M140 20 V130" stroke="#27272A" strokeWidth="0.75" strokeDasharray="3 3" />
      
      <!-- Ghost DAG Nodes -->
      <circle cx="50" cy="75" r="14" fill="#09090B" stroke="#7C3AED" strokeWidth="2" />
      <circle cx="50" cy="75" r="5" fill="#7C3AED" />
      
      <circle cx="100" cy="50" r="12" fill="#09090B" stroke="#3F3F46" strokeWidth="1.5" strokeDasharray="3 2" />
      <circle cx="100" cy="100" r="12" fill="#09090B" stroke="#3F3F46" strokeWidth="1.5" strokeDasharray="3 2" />
      
      <circle cx="150" cy="75" r="12" fill="#09090B" stroke="#3F3F46" strokeWidth="1.5" strokeDasharray="3 2" />

      <!-- Connector paths -->
      <path d="M64 75 C80 75, 80 50, 88 50" stroke="#7C3AED" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.6" />
      <path d="M64 75 C80 75, 80 100, 88 100" stroke="#7C3AED" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.6" />
      <path d="M112 50 C125 50, 130 75, 138 75" stroke="#3F3F46" strokeWidth="1.5" strokeDasharray="4 3" />
      <path d="M112 100 C125 100, 130 75, 138 75" stroke="#3F3F46" strokeWidth="1.5" strokeDasharray="4 3" />
    </svg>
    <h3 style={titleStyle}>{title}</h3>
    <p style={descStyle}>{description}</p>
    {actionText && onAction && (
      <button style={btnStyle} onClick={onAction}>
        {actionText}
      </button>
    )}
  </div>
);

/**
 * 2. No Runners Empty State
 */
export const NoRunnersEmptyState: React.FC<EmptyStateProps> = ({
  title = 'Нет подключенных раннеров',
  description = 'Сборки выполняются локально. Запустите zovik-agent на внешних нодах для распределения нагрузки.',
  actionText = 'Зарегистрировать агент',
  onAction,
}) => (
  <div style={containerStyle}>
    <svg width="200" height="150" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="#27272A" strokeWidth="1.5" />
      
      <!-- Server Rack Boxes -->
      <rect x="45" y="45" width="110" height="24" rx="6" fill="#09090B" stroke="#3F3F46" strokeWidth="1.5" />
      <circle cx="60" cy="57" r="3" fill="#EF4444" />
      <line x1="75" y1="57" x2="140" y2="57" stroke="#27272A" strokeWidth="2" strokeLinecap="round" />

      <rect x="45" y="80" width="110" height="24" rx="6" fill="#09090B" stroke="#7C3AED" strokeWidth="1.5" strokeDasharray="4 3" />
      <circle cx="60" cy="92" r="3" fill="#EAB308" />
      <line x1="75" y1="92" x2="120" y2="92" stroke="rgba(124, 58, 237, 0.4)" strokeWidth="2" strokeLinecap="round" />
      
      <!-- Signal Wave disconnected -->
      <path d="M100 20 C85 20, 75 30, 75 35" stroke="#7C3AED" strokeWidth="2" strokeLinecap="round" strokeDasharray="3 3" opacity="0.6" />
      <path d="M100 15 C75 15, 60 30, 60 40" stroke="#7C3AED" strokeWidth="2" strokeLinecap="round" strokeDasharray="3 3" opacity="0.3" />
    </svg>
    <h3 style={titleStyle}>{title}</h3>
    <p style={descStyle}>{description}</p>
    {actionText && onAction && (
      <button style={btnStyle} onClick={onAction}>
        {actionText}
      </button>
    )}
  </div>
);

/**
 * 3. Build Failed Empty State
 */
export const BuildFailedEmptyState: React.FC<EmptyStateProps> = ({
  title = 'Сборка завершилась ошибкой',
  description = 'Один из шагов DAG-графа вернул ненулевой код выхода. Ознакомьтесь с логами в терминале ниже.',
  actionText = 'Перезапустить билд',
  onAction,
}) => (
  <div style={containerStyle}>
    <svg width="200" height="150" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="rgba(239, 68, 68, 0.3)" strokeWidth="1.5" />
      
      <!-- Central Failed Shield Badge -->
      <circle cx="100" cy="70" r="28" fill="rgba(239, 68, 68, 0.08)" stroke="#EF4444" strokeWidth="2" />
      <line x1="88" y1="58" x2="112" y2="82" stroke="#EF4444" strokeWidth="3" strokeLinecap="round" />
      <line x1="112" y1="58" x2="88" y2="82" stroke="#EF4444" strokeWidth="3" strokeLinecap="round" />

      <!-- Error Terminal Lines -->
      <rect x="50" y="108" width="100" height="4" rx="2" fill="#27272A" />
      <rect x="65" y="116" width="70" height="4" rx="2" fill="#EF4444" opacity="0.6" />
    </svg>
    <h3 style={titleStyle}>{title}</h3>
    <p style={descStyle}>{description}</p>
    {actionText && onAction && (
      <button style={{ ...btnStyle, backgroundColor: '#EF4444' }} onClick={onAction}>
        {actionText}
      </button>
    )}
  </div>
);

/**
 * 4. Disconnected Empty State
 */
export const DisconnectedEmptyState: React.FC<EmptyStateProps> = ({
  title = 'Потеряна связь с сервером Zovik',
  description = 'Не удается установить WebSocket-соединение с бэкендом. Проверьте статус демона и сети.',
  actionText = 'Повторить подключение',
  onAction,
}) => (
  <div style={containerStyle}>
    <svg width="200" height="150" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="#27272A" strokeWidth="1.5" />
      
      <!-- Broken Link Chain / Wi-Fi -->
      <circle cx="100" cy="75" r="24" fill="#09090B" stroke="#52525B" strokeWidth="2" strokeDasharray="4 3" />
      <line x1="60" y1="35" x2="140" y2="115" stroke="#EF4444" strokeWidth="2.5" strokeLinecap="round" />
      
      <circle cx="100" cy="75" r="5" fill="#71717A" />
    </svg>
    <h3 style={titleStyle}>{title}</h3>
    <p style={descStyle}>{description}</p>
    {actionText && onAction && (
      <button style={btnStyle} onClick={onAction}>
        {actionText}
      </button>
    )}
  </div>
);

/* Styles */
const containerStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '48px 24px',
  textAlign: 'center',
  background: '#18181B',
  borderRadius: '16px',
  border: '1px solid #27272A',
  maxWidth: '560px',
  margin: '0 auto',
};

const titleStyle: React.CSSProperties = {
  fontSize: '17px',
  fontWeight: 700,
  color: '#FAFAFA',
  margin: '20px 0 8px 0',
  letterSpacing: '-0.02em',
};

const descStyle: React.CSSProperties = {
  fontSize: '13.5px',
  color: '#A1A1AA',
  lineHeight: 1.55,
  maxWidth: '440px',
  margin: '0 0 24px 0',
};

const btnStyle: React.CSSProperties = {
  padding: '8px 20px',
  borderRadius: '8px',
  backgroundColor: '#7C3AED',
  color: '#FFFFFF',
  fontWeight: 600,
  fontSize: '13px',
  border: 'none',
  cursor: 'pointer',
  transition: 'background 0.2s',
};
