import { forwardRef, useEffect, useId, useRef } from 'react';

function classes(...values) {
  return values.filter(Boolean).join(' ');
}

export function ArcadeShell({ children, theme = 'neutral', game = false, className = '' }) {
  return (
    <div className={classes('arcade-shell', className)} data-game={theme}>
      <a className="skip-link" href="#main-content">Skip to content</a>
      <main id="main-content" className={classes('arcade-main', game && 'arcade-main--game')}>
        {children}
      </main>
    </div>
  );
}

export function ArcadePanel({ children, className = '', padded = true, quiet = false, as = 'section', ...props }) {
  const Component = as;
  return (
    <Component
      className={classes('arcade-panel', padded && 'arcade-panel--padded', quiet && 'arcade-panel--quiet', className)}
      {...props}
    >
      {children}
    </Component>
  );
}

export const ArcadeButton = forwardRef(function ArcadeButton({
  children,
  variant = 'primary',
  size = 'medium',
  block = false,
  loading = false,
  disabled,
  className = '',
  type = 'button',
  ...props
}, ref) {
  return (
    <button
      ref={ref}
      type={type}
      className={classes(
        'arcade-button',
        variant !== 'primary' && `arcade-button--${variant}`,
        size === 'small' && 'arcade-button--small',
        block && 'arcade-button--block',
        className,
      )}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading && <span aria-hidden="true">[...]</span>}
      {children}
    </button>
  );
});

export function ArcadeInput({ label, hint, error, id, className = '', ...props }) {
  const generatedId = useId();
  const controlId = id || generatedId;
  const descriptionId = hint || error ? `${controlId}-description` : undefined;
  return (
    <label className={classes('arcade-field', className)} htmlFor={controlId}>
      {label && <span className="arcade-field__label">{label}</span>}
      <input
        id={controlId}
        className="arcade-field__control"
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={descriptionId}
        {...props}
      />
      {(error || hint) && (
        <span id={descriptionId} className={error ? 'arcade-field__error' : 'arcade-field__hint'}>
          {error || hint}
        </span>
      )}
    </label>
  );
}

export function ArcadeSelect({ label, hint, id, className = '', children, ...props }) {
  const generatedId = useId();
  const controlId = id || generatedId;
  return (
    <label className={classes('arcade-field', className)} htmlFor={controlId}>
      {label && <span className="arcade-field__label">{label}</span>}
      <select id={controlId} className="arcade-field__control" {...props}>{children}</select>
      {hint && <span className="arcade-field__hint">{hint}</span>}
    </label>
  );
}

export function ArcadeDialog({
  open,
  title,
  eyebrow,
  children,
  actions,
  onClose,
  dismissible = true,
  closeLabel = 'Close',
  wide = false,
  initialFocusRef,
  className = '',
}) {
  const titleId = useId();
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const previousOverflow = document.body.style.overflow;
    const previousActive = document.activeElement;
    document.body.style.overflow = 'hidden';
    const dialog = dialogRef.current;
    const focusable = dialog?.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex]:not([tabindex="-1"])');
    const firstFocusable = focusable?.[0];
    const preferredControl = dialog?.querySelector(
      '[autofocus], input:not([disabled]), select:not([disabled]), textarea:not([disabled])',
    );
    const focusTarget = initialFocusRef?.current || preferredControl || firstFocusable || dialog;
    focusTarget?.focus();

    const onKeyDown = (event) => {
      if (event.key === 'Escape' && dismissible) {
        event.preventDefault();
        onClose?.();
        return;
      }
      if (event.key !== 'Tab' || !focusable?.length) return;
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === firstFocusable) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        firstFocusable.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', onKeyDown);
      previousActive?.focus?.();
    };
  }, [dismissible, initialFocusRef, onClose, open]);

  if (!open) return null;
  return (
    <div className="arcade-dialog-backdrop">
      <section
        ref={dialogRef}
        className={classes('arcade-dialog', wide && 'arcade-dialog--wide', className)}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <header className="arcade-dialog__header">
          <div>
            {eyebrow && <p className="arcade-eyebrow">{eyebrow}</p>}
            <h2 id={titleId} className="arcade-title">{title}</h2>
          </div>
          {dismissible && (
            <ArcadeButton variant="ghost" size="small" onClick={onClose} aria-label={`${closeLabel} dialog`}>
              {closeLabel}
            </ArcadeButton>
          )}
        </header>
        {children}
        {actions && <footer className="arcade-dialog__actions">{actions}</footer>}
      </section>
    </div>
  );
}

export function StatusBanner({ children, tone = 'info', live = false, className = '' }) {
  return (
    <div
      className={classes('arcade-status', `arcade-status--${tone}`, className)}
      role={tone === 'error' ? 'alert' : 'status'}
      aria-live={live ? 'polite' : undefined}
    >
      <span aria-hidden="true">{tone === 'error' ? '!!' : tone === 'success' ? '++' : tone === 'warning' ? '!' : 'i'}</span>
      <span>{children}</span>
    </div>
  );
}

export function ArcadeBadge({ children, tone = 'info', className = '' }) {
  return <span className={classes('arcade-badge', tone !== 'info' && `arcade-badge--${tone}`, className)}>{children}</span>;
}

export function ArcadeToolbar({ children, className = '' }) {
  return <header className={classes('arcade-toolbar', className)}>{children}</header>;
}

export function ToolbarGroup({ children, className = '' }) {
  return <div className={classes('arcade-toolbar__group', className)}>{children}</div>;
}

export function ConnectionBadge({ state = 'connected' }) {
  const labels = {
    connecting: 'Connecting',
    connected: 'Online',
    reconnecting: 'Reconnecting',
    offline: 'Offline',
  };
  const tones = {
    connecting: 'warning',
    connected: 'success',
    reconnecting: 'warning',
    offline: 'error',
  };
  return <ArcadeBadge tone={tones[state] || 'muted'}>{labels[state] || state}</ArcadeBadge>;
}

export function PlayerSeat({ name, index = 0, active = false, meta, badge, children, className = '', ...props }) {
  return (
    <article className={classes('arcade-seat', active && 'arcade-seat--active', className)} aria-current={active ? 'true' : undefined} {...props}>
      <span className="arcade-seat__avatar" aria-hidden="true">P{index + 1}</span>
      <span className="min-w-0">
        <span className="arcade-seat__name" title={name}>{name}</span>
        {meta && <span className="arcade-seat__meta block">{meta}</span>}
      </span>
      {badge}
      {children}
    </article>
  );
}

export function Scoreboard({ columns, rows, getRowKey = (_, index) => index }) {
  return (
    <div className="arcade-table-wrap">
      <table className="arcade-table">
        <thead><tr>{columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}</tr></thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={getRowKey(row, index)}>
              {columns.map((column) => <td key={column.key}>{column.render ? column.render(row, index) : row[column.key]}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
