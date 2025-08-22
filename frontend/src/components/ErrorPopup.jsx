// Displays an error message in a styled red popup.
// If no message is provided, renders nothing.
function ErrorPopup({ message }) {
  if (!message) return null;

  return (
    <div className="text-red-500 text-sm text-center p-2 bg-red-100 rounded">
      {message}
    </div>
  );
}

export default ErrorPopup;