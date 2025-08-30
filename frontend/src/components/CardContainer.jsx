// A wrapper component that provides consistent styling for card-like layouts.
// It accepts children elements and optional extra class names.
function CardContainer({ children, className = '', noMax = false }) {
  const maxClass = noMax ? 'max-w-none' : 'max-w-md';
  return (
    <div className={`card bg-base-100 shadow-xl p-6 w-full ${maxClass} space-y-4 ${className}`}>
      {children}
    </div>
  );
}

export default CardContainer;