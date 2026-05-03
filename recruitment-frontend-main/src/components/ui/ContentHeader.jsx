export default function ContentHeader({ title, actions }) {
  return (
    <div className="flex items-center justify-between w-full px-4">
      <div className="text-lg font-semibold text-gray-800">{title}</div>
      <div className="flex items-center gap-2">{actions}</div>
    </div>
  );
}
