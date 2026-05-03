import { MoreVertical, ChevronRight } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { cn } from "../../utils/utils";

export default function DropdownMenu({ options = [], position = "right" }) {
  const [showMenu, setShowMenu] = useState(false);
  const [hoveredSubmenu, setHoveredSubmenu] = useState(null);
  const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
  const menuRef = useRef(null);
  const buttonRef = useRef(null);
  const submenuTimeoutRef = useRef(null);

  // Update menu position when showing
  useEffect(() => {
    if (showMenu && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setMenuPosition({
        top: rect.bottom + window.scrollY + 4,
        left: position === "left" ? rect.left + window.scrollX : rect.right + window.scrollX - 160,
      });
    }
  }, [showMenu, position]);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowMenu(false);
        setHoveredSubmenu(null);
      }
    };

    if (showMenu) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      if (submenuTimeoutRef.current) {
        clearTimeout(submenuTimeoutRef.current);
      }
    };
  }, [showMenu]);

  const handleSubmenuEnter = (index) => {
    if (submenuTimeoutRef.current) {
      clearTimeout(submenuTimeoutRef.current);
    }
    setHoveredSubmenu(index);
  };

  const handleSubmenuLeave = () => {
    submenuTimeoutRef.current = setTimeout(() => {
      setHoveredSubmenu(null);
    }, 150); // Delay 150ms trước khi ẩn
  };

  const handleOptionClick = (e, option) => {
    e.stopPropagation();
    if (submenuTimeoutRef.current) {
      clearTimeout(submenuTimeoutRef.current);
    }
    setShowMenu(false);
    setHoveredSubmenu(null);
    if (option.onClick) {
      option.onClick();
    }
  };

  const getPositionClass = () => {
    switch (position) {
      case "left":
        return "left-0";
      case "right":
      default:
        return "right-0";
    }
  };

  return (
    <>
      <div className="relative" ref={buttonRef}>
        <div
          className="p-1 hover:bg-gray-100 rounded transition-colors cursor-pointer"
          onClick={(e) => {
            e.stopPropagation();
            setShowMenu(!showMenu);
          }}
        >
          <MoreVertical className="h-4 w-4 text-gray-600 hover:text-gray-800" />
        </div>
      </div>

      {showMenu && createPortal(
        <div
          ref={menuRef}
          className="fixed min-w-[160px] bg-white rounded-lg shadow-lg border border-gray-200 z-[100] py-1"
          style={{
            top: `${menuPosition.top}px`,
            left: `${menuPosition.left}px`,
          }}
        >
          {options.map((option, index) => {
            // Skip if option is hidden
            if (option.hidden) return null;

            // Render divider
            if (option.divider) {
              return (
                <div
                  key={`divider-${index}`}
                  className="h-px bg-gray-200 my-1"
                />
              );
            }

            // Render menu item with submenu
            if (option.submenu && option.submenu.length > 0) {
              const Icon = option.icon;
              return (
                <div
                  key={option.label || index}
                  className="relative"
                  onMouseEnter={() => handleSubmenuEnter(index)}
                  onMouseLeave={handleSubmenuLeave}
                >
                  <div
                    className={cn(
                      "w-full px-4 py-2 text-sm text-left hover:bg-gray-50 flex items-center justify-between gap-2 transition-colors cursor-pointer text-gray-700 hover:text-gray-900 whitespace-nowrap",
                      option.disabled &&
                        "opacity-50 cursor-not-allowed pointer-events-none"
                    )}
                  >
                    <div className="flex items-center gap-2">
                      {Icon && <Icon className="h-4 w-4 flex-shrink-0" />}
                      <span>{option.label}</span>
                    </div>
                    <ChevronRight className="h-4 w-4 flex-shrink-0" />
                  </div>

                  {/* Bridge area - invisible area to make hovering easier */}
                  {hoveredSubmenu === index && (
                    <div className="absolute left-full top-0 w-2 h-full" />
                  )}

                  {/* Submenu */}
                  {hoveredSubmenu === index && (
                    <div
                      className="absolute left-full top-0 ml-0.5 min-w-[160px] bg-white rounded-lg shadow-lg border border-gray-200 z-[100] py-1"
                      onMouseEnter={() => handleSubmenuEnter(index)}
                      onMouseLeave={handleSubmenuLeave}
                    >
                      {option.submenu.map((subOption, subIndex) => {
                        const SubIcon = subOption.icon;
                        const subTextColor =
                          subOption.variant === "danger"
                            ? "text-red-600 hover:text-red-700"
                            : "text-gray-700 hover:text-gray-900";

                        return (
                          <div
                            key={subOption.label || subIndex}
                            className={cn(
                              "w-full px-4 py-2 text-sm text-left hover:bg-gray-50 flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap",
                              subTextColor,
                              subOption.disabled &&
                                "opacity-50 cursor-not-allowed pointer-events-none"
                            )}
                            onClick={(e) => {
                              e.stopPropagation();
                              if (submenuTimeoutRef.current) {
                                clearTimeout(submenuTimeoutRef.current);
                              }
                              setShowMenu(false);
                              setHoveredSubmenu(null);
                              if (subOption.onClick) {
                                subOption.onClick();
                              }
                            }}
                          >
                            {SubIcon && <SubIcon className="h-4 w-4 flex-shrink-0" />}
                            <span>{subOption.label}</span>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            }

            // Render regular menu item
            const Icon = option.icon;
            const textColor =
              option.variant === "danger"
                ? "text-red-600 hover:text-red-700"
                : "text-gray-700 hover:text-gray-900";

            return (
              <div
                key={option.label || index}
                className={cn(
                  "w-full px-4 py-2 text-sm text-left hover:bg-gray-50 flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap",
                  textColor,
                  option.disabled &&
                    "opacity-50 cursor-not-allowed pointer-events-none"
                )}
                onClick={(e) =>
                  !option.disabled && handleOptionClick(e, option)
                }
              >
                {Icon && <Icon className="h-4 w-4 flex-shrink-0" />}
                <span>{option.label}</span>
              </div>
            );
          })}
        </div>,
        document.body
      )}
    </>
  );
}
