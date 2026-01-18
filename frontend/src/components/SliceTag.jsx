function SliceTag({ children, className = '' }) {
  return <span className={`pixel-slice-tag ${className}`}>{children}</span>;
}

export default SliceTag;
