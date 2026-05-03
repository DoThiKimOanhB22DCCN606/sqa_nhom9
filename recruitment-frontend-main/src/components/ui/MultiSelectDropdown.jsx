import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { ChevronDown, X } from "lucide-react";
import { cn } from "../../utils/utils";
import { useTranslation } from "react-i18next";

export default function MultiSelectDropdown({
  label,
  options = [],
  selectedValues = [],
  onChange,
  placeholder,
  required = false,
  disabled = false,
  isRow = false,
  hideLabel = false,
  compact = false,
  className,
}) {
  const { t } = useTranslation();
  const displayPlaceholder = placeholder || t("common.select");
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const [menuPos, setMenuPos] = useState({ top: 0, left: 0, width: 0 });

  useEffect(() => {
    const handleClickOutside = (event) => {
      const clickedInsideButton = dropdownRef.current && dropdownRef.current.contains(event.target);
      const clickedInsidePortalMenu = !!event.target.closest('[data-portal-menu]');
      if (!clickedInsideButton && !clickedInsidePortalMenu) {
        setIsOpen(false);
      }
    };
    document.addEventListener("click", handleClickOutside);
    return () => {
      document.removeEventListener("click", handleClickOutside);
    };
  }, []);

  const updatePosition = () => {
    if (!dropdownRef.current) return;
    const btn = dropdownRef.current.querySelector("[data-dropdown-button]");
    const rect = btn ? btn.getBoundingClientRect() : dropdownRef.current.getBoundingClientRect();
    setMenuPos({ top: rect.bottom + 4, left: rect.left, width: rect.width });
  };

  useEffect(() => {
    if (!isOpen) return;
    updatePosition();
    window.addEventListener("scroll", updatePosition, true);
    window.addEventListener("resize", updatePosition);
    let resizeObserver;
    const btnEl = dropdownRef.current.querySelector('[data-dropdown-button]');
    if (btnEl && 'ResizeObserver' in window) {
      resizeObserver = new ResizeObserver(() => updatePosition());
      resizeObserver.observe(btnEl);
    }
    return () => {
      window.removeEventListener("scroll", updatePosition, true);
      window.removeEventListener("resize", updatePosition);
      if (resizeObserver) {
        try { resizeObserver.disconnect(); } catch {}
      }
    };
  }, [isOpen]);

  const handleToggle = (optionId) => {
    if (disabled) return;
    const isSelected = selectedValues.map((v) => String(v)).includes(String(optionId));
    const newValues = isSelected
      ? selectedValues.filter((id) => String(id) !== String(optionId))
      : [...selectedValues, optionId];
    onChange(newValues);
  };

  const handleRemove = (optionId) => {
    if (disabled) return;
    onChange(selectedValues.filter((id) => String(id) !== String(optionId)));
  };

  const selectedOptions = options.filter((opt) =>
    selectedValues.map((v) => String(v)).includes(String(opt.id))
  );

  return (
    <div className={`flex ${isRow ? "items-center gap-4" : hideLabel ? "" : "flex-col gap-2"}`}>
      {!hideLabel && label && (
        <label className="block font-medium text-gray-700">
          {label}
          {required && <span className="text-red-500"> *</span>}
        </label>
      )}
      <div className={cn("relative w-full", className)} ref={dropdownRef}>
        <div
          onClick={() => !disabled && setIsOpen(!isOpen)}
          className={cn(
              `border border-gray-300 rounded-lg text-gray-700 bg-white
              focus:outline-none focus-within:ring-2 focus-within:ring-red-500 focus-within:border-transparent
              flex items-center justify-between cursor-pointer`,
              compact ? "px-4 py-2 h-9 text-sm" : "p-2 rounded-md",
            disabled && "opacity-50 cursor-not-allowed bg-gray-100",
            className
          )}
          data-dropdown-button
        >
          <div className="flex items-center gap-2 w-full">
            <div className="flex flex-wrap gap-1.5 flex-1 min-w-0 items-center">
              {selectedOptions.length > 0 ? (
                (() => {
                  const visible = selectedOptions.slice(0, 3);
                  const remaining = selectedOptions.length - visible.length;
                  return (
                    <>
                      {visible.map((option) => (
                        <div
                          key={option.id}
                          className="inline-flex items-center gap-1 pl-2 pr-1 py-0.5 bg-gray-100 text-gray-800 rounded-full text-xs max-w-full border border-transparent hover:border-gray-300 transition"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <span className="truncate max-w-[200px] leading-4">{option.name}</span>
                          {!disabled && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleRemove(option.id);
                              }}
                              className="ml-0.5 inline-flex items-center justify-center h-4 w-4 rounded-full text-gray-600 hover:text-white hover:bg-red-500 transition font-bold"
                              aria-label="Remove"
                              title="Remove"
                            >
                              ×
                            </button>
                          )}
                        </div>
                      ))}
                      {remaining > 0 && (
                        <div className="px-2 py-0.5 bg-gray-50 text-gray-600 rounded-full text-xs border border-transparent">+{remaining}</div>
                      )}
                    </>
                  );
                })()
              ) : (
                <span className="text-gray-400">{displayPlaceholder}</span>
              )}
            </div>
            <ChevronDown
              className={cn(
                "h-4 w-4 text-gray-400 transition-transform duration-200",
                isOpen && "rotate-180"
              )}
            />
          </div>
        </div>

        {isOpen && !disabled &&
          createPortal(
            <ul
              className="fixed z-[1000] max-h-60 overflow-y-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5"
              style={{ top: menuPos.top, left: menuPos.left, width: menuPos.width }}
              data-portal-menu
            >
              {options.map((option) => (
                <li
                  key={option.id}
                  onClick={() => handleToggle(option.id)}
                  className="relative cursor-pointer select-none px-3 py-2 text-sm hover:bg-gray-50 flex items-center gap-3"
                >
                  <input
                    type="checkbox"
                    checked={selectedValues.map(String).includes(String(option.id))}
                    onChange={() => handleToggle(option.id)}
                    className="h-4 w-4 text-red-600 rounded border-gray-300 focus:ring-red-500"
                  />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">{option.name}</div>
                  </div>
                </li>
              ))}
            </ul>,
            document.body
          )}
      </div>
    </div>
  );
}
