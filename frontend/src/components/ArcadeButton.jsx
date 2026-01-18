function ArcadeButton({ children, variant = 'primary', className = '', ...props }) {
  const variantClass = variant === 'secondary' ? 'pixel-button pixel-button-secondary' : 'pixel-button pixel-button-primary';

  return (
    <button type="button" className={`${variantClass} ${className}`} {...props}>
      {children}
    </button>
  );
}

export default ArcadeButton;
