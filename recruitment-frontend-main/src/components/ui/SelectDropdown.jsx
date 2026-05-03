import { useState, useEffect, useRef } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "../../utils/utils";

export default function SelectDropdown({
  label,
  options = [],
  value,
  isRow,
  onChange = () => {},
  placeholder,
  className,
  required,
  disabled,
  hideLabel = false,
  compact = false,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const selectedOption = options.find((option) => option.id === value);

  const displayLabel = selectedOption ? selectedOption.name : placeholder;

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [dropdownRef]);

  const handleOptionClick = (option) => {
    if (!disabled) {
      onChange(option.id);
      setIsOpen(false);
    }
  };

  return (
    <div className={`flex ${isRow ? "items-center gap-4" : hideLabel ? "" : "flex-col gap-2"}`}>
      {!hideLabel && label && (
        <label className="block font-medium text-gray-700" htmlFor={label}>
          {label}
          {required && <span className="text-red-500"> *</span>}
        </label>
      )}
      <div className={cn("relative w-full", className)} ref={dropdownRef}>
        <div
          tabIndex={disabled ? -1 : 0}
          className={cn(
            `border border-gray-300 rounded-lg text-gray-700 bg-white
        focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
        flex items-center justify-between cursor-pointer`,
            compact ? "px-4 py-2 h-9 text-sm" : "p-2 rounded-md",
            disabled && "opacity-50 cursor-not-allowed bg-gray-100",
            className
          )}
          onClick={() => !disabled && setIsOpen(!isOpen)}
        >
          <span className={cn(!selectedOption && "text-gray-400")}>
            {displayLabel}
          </span>
          <ChevronDown
            className={cn(
              "h-4 w-4 text-gray-400 transition-transform duration-200",
              isOpen && "rotate-180"
            )}
          />
        </div>

        {isOpen && !disabled && (
          <ul
            className="absolute z-[100] mt-1 w-full
                     max-h-60 overflow-y-auto
                     rounded-md bg-white py-1
                     shadow-lg ring-1 ring-black ring-opacity-5"
            role="listbox"
          >
            {options.map((option) => (
              <li
                key={option.id}
                className={cn(
                  "relative cursor-pointer select-none px-3 py-2 text-sm",
                  "text-gray-900 hover:bg-gray-100",
                  option.id === value && "font-semibold bg-gray-100"
                )}
                onClick={() => handleOptionClick(option)}
                role="option"
                aria-selected={option.id === value}
              >
                {option.name}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
