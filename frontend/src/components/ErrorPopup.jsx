import { StatusBanner } from './arcade/ArcadeUI';

function ErrorPopup({ message }) {
  if (!message) return null;
  return <StatusBanner tone="error" live>{message}</StatusBanner>;
}

export default ErrorPopup;
