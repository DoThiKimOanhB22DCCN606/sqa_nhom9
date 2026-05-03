import { cn } from "../../utils/utils";

const TextArea = ({
  label,
  name,
  placeholder,
  readOnly = false,
  disabled = false,
  value,
  onChange,
  required = false,
  rows = 4,
  className,
}) => {
  return (
    <div className="flex flex-col gap-2">
      <label className="block font-medium text-gray-700" htmlFor={label}>
        {label}
        {required && <span className="text-red-500"> *</span>}
      </label>
      <textarea
        className={cn(
          `border border-gray-300 p-2 rounded-md text-gray-700
        focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
        disabled:opacity-50 disabled:cursor-not-allowed resize-none
        flex-1`,
          className
        )}
        id={label}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        disabled={readOnly || disabled}
        rows={rows}
      />
    </div>
  );
};

export default TextArea;
