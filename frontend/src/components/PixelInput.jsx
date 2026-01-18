function PixelInput({
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  name,
  required = false,
  autoComplete,
  status = 'idle',
  rightElement = null,
}) {
  const statusClass =
    status === 'error'
      ? 'pixel-status-dot pixel-status-dot-error'
      : status === 'ok'
        ? 'pixel-status-dot pixel-status-dot-ok'
        : 'pixel-status-dot pixel-status-dot-idle';

  return (
    <label className="pixel-input-field">
      <span className="pixel-label">{label}</span>
      <div className="pixel-input-shell">
        <span className="pixel-input-prefix" aria-hidden="true">
          &gt;
        </span>
        <input
          className="pixel-input"
          type={type}
          name={name}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
          autoComplete={autoComplete}
        />
        {rightElement}
        <span className={statusClass} aria-hidden="true" />
      </div>
    </label>
  );
}

export default PixelInput;
