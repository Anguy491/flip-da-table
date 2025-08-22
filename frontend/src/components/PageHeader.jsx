// A flexible header layout with optional left and right content areas.
// Often used for page titles with accompanying controls (e.g. buttons).
function PageHeader({
  title,
  rightContent = null,
  leftContent = null,
  className = ''
}) {
  return (
    <div className={`flex justify-between items-center mb-6 ${className}`}>
      <div className="flex items-center gap-2">
        {leftContent}
        <h2 className="text-3xl font-bold">{title}</h2>
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