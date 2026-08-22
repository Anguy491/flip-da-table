import { ArcadeButton } from './arcade/ArcadeUI';

function SubmitButton({ children, className = '', disabled = false, fullWidth = false, variant = 'primary', ...props }) {
  return <ArcadeButton className={className} disabled={disabled} block={fullWidth} variant={variant} type="submit" {...props}>{children}</ArcadeButton>;
}

export default SubmitButton;
