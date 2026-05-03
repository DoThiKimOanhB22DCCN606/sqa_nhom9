export default function RadioOptions({
  label,
  options,
  selectedValue,
  onChange,
  isRow = false,
  required = false,
}) {
  const handleRadioChange = (event) => {
    onChange(event.target.value);
  };
  return (
    <div className={`flex ${isRow ? "items-center gap-4" : "flex-col gap-2"}`}>
      <label className="block text-gray-700" htmlFor={label}>
        {label}
        {required && <span className="text-red-500"> *</span>}
      </label>
      <div className={`flex flex-wrap gap-4 ${isRow ? "" : "flex-col"}`}>
        {options.map((option) => (
          <RadioInput
            key={option.value}
            label={option.label}
            checked={selectedValue === option.value}
            onChange={handleRadioChange}
            value={option.value}
          />
        ))}
      </div>
    </div>
  );
}
const RadioInput = ({ label, checked, onChange, value }) => {
  return (
    <div className="flex items-center gap-2">
      <input
        type="radio"
        id={label}
        checked={checked}
        onChange={onChange}
        value={value}
        className="h-4 w-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
      />
      <label htmlFor={label} className="text-gray-700">
        {label}
      </label>
    </div>
  );
};
