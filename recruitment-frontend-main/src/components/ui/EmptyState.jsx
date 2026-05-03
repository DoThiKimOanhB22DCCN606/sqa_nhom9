import { Image as ImageIcon } from "lucide-react";

export default function EmptyState({
  imageSrc,
  imageAlt = "",
  icon: Icon = ImageIcon,
  title,
  className = "",
}) {
  return (
    <div className={`flex flex-col items-center justify-center text-center ${className}`}>
      <div className="mb-3">
        {imageSrc ? (
          <img src={imageSrc} alt={imageAlt} className="w-24 h-24 object-contain opacity-80" />
        ) : (
          <Icon className="w-16 h-16 text-gray-300" />
        )}
      </div>
      <div className="text-sm text-gray-600">{title}</div>
    </div>
  );
}
