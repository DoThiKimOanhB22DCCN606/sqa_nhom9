import { cn } from "../../utils/utils";

const TextInput = ({
  label,
  name,
  placeholder,
  type,
  readOnly = false,
  disabled = false,
  value,
  onChange,
  required = false,
  isRow = false,
  className,
  icon: Icon,
  hideLabel = false,
}) => {
  return (
    <div className={`flex ${isRow ? "items-center gap-4" : "flex-col gap-2"}`}>
      {!hideLabel && (
        <label className="block font-medium text-gray-700" htmlFor={label}>
          {label}
          {required && <span className="text-red-500"> *</span>}
        </label>
      )}
      <div className="relative flex-1">
        {Icon && (
          <Icon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
        )}
        <input
          className={cn(
            `border border-gray-300 p-2 rounded-md text-gray-700
          focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
          disabled:opacity-50 disabled:cursor-not-allowed
          w-full`,
            Icon && "pl-10",
            className
          )}
          type={type}
          id={label}
          name={name}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          required={required}
          disabled={readOnly || disabled}
          min={type === "number" ? 0 : undefined}
        />
      </div>
    </div>
  );
};

export default TextInput;
