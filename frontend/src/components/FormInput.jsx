// A reusable form input component with standard styling.
// Supports common HTML input props like type, placeholder, value, onChange, and required.
function FormInput({ type = 'text', placeholder, value, onChange, required = false }) {
  return (
    <input
      type={type}
      placeholder={placeholder}
      className="input input-bordered w-full"
      value={value}
      onChange={onChange}
      required={required}
    />
  );
}

export default FormInput;