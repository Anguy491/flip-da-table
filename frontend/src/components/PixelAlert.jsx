function PixelAlert({ message, onDismiss }) {
  if (!message) {
    return null;
  }

  return (
    <div className="pixel-alert">
      <span className="pixel-alert-icon" aria-hidden="true">
        !
      </span>
      <span className="pixel-alert-text">{message}</span>
      {onDismiss ? (
        <button type="button" className="pixel-alert-action" onClick={onDismiss}>
          DISMISS
        </button>
      ) : null}
    </div>
  );
}

export default PixelAlert;
