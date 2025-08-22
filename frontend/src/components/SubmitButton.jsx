// A standardized submit button component with flexible width styling.
// Accepts children as the button label and supports additional props and class names.
function SubmitButton({ children, className = '', disabled = false, fullWidth = false, ...props }) {
  return (
    <button
      type="submit"
      className={`btn btn-primary ${fullWidth ? 'w-full' : ''} ${className}`}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
}

export default SubmitButton;