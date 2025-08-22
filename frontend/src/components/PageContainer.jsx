// Provides a full-page centered layout with padding.
// Useful as a base layout for forms or main content pages.
function PageContainer({ children }) {
  return (
    <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center px-4">
      {children}
    </div>
  );
}

export default PageContainer;