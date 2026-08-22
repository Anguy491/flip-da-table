import { ArcadeInput } from './arcade/ArcadeUI';

function FormInput({ type = 'text', placeholder, label, value, onChange, required = false, ...props }) {
  return <ArcadeInput type={type} placeholder={placeholder} label={label || placeholder} value={value} onChange={onChange} required={required} {...props} />;
}

export default FormInput;
