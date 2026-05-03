export default function CheckBoxOptions({
  label,
  options = [],
  selectedValues = [],
  onChange,
  isRow = false,
  required = false,
  disabled = false,
}) {
  return (
    <div className={`flex ${isRow ? "items-center gap-4" : "flex-col gap-2"}`}>
      <label className="block text-gray-700" htmlFor={label}>
        {label}
        {required && <span className="text-red-500"> *</span>}
      </label>
      <div className={`flex flex-wrap gap-4 ${isRow ? "" : "flex-col"}`}>
        {options.map((option) => (
          <CheckBoxInput
            key={option.id}
            label={option.name}
            checked={selectedValues.includes(option.id)}
            onChange={onChange}
            value={option.id}
            disabled={disabled}
          />
        ))}
      </div>
    </div>
  );
}
const CheckBoxInput = ({
  label,
  checked,
  onChange,
  value,
  disabled = false,
}) => {
  return (
    <div className="flex items-center gap-2">
      <input
        type="checkbox"
        id={label}
        checked={checked}
        onChange={() => onChange(value)}
        disabled={disabled}
        className="h-4 w-4 text-red-600 border-gray-300 rounded focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
      />
      <label
        htmlFor={label}
        className={`text-gray-700 ${
          disabled ? "opacity-50 cursor-not-allowed" : ""
        }`}
      >
        {label}
      </label>
    </div>
  );
};
