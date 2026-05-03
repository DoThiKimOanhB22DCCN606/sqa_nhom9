export default function LoadingOverlay({ show }) {
  if (!show) return null;
  return (
    <div className="absolute inset-0 bg-white/60 backdrop-blur-[1px] flex items-center justify-center z-40">
      <div className="h-10 w-10 animate-spin rounded-full border-2 border-red-600 border-t-transparent" />
    </div>
  );
}
