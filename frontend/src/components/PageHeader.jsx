function PageHeader({
  title,
  rightContent = null,
  leftContent = null,
  className = ''
}) {
  return (
    <div className={`arcade-dashboard-header mb-6 ${className}`}>
      <div className="flex items-center gap-2">
        {leftContent}
        <h2 className="arcade-title">{title}</h2>
      </div>
      {rightContent && (
        <div className="flex items-center gap-2">
          {rightContent}
        </div>
      )}
    </div>
  );
}

export default PageHeader;
