import { Colors } from "../../constants/colors";
import { cn } from "../../utils/utils";

const Button = ({
  children,
  onClick,
  className,
  variant = "solid",
  disabled,
  type = "button",
  ...props
}) => {
  const getVariantStyles = () => {
    if (variant === "outline") {
      return {
        className: cn(
          "border-2 border-[#DC2626] text-[#DC2626] bg-white hover:bg-red-50",
          disabled && "opacity-50 cursor-not-allowed hover:bg-white"
        ),
        style: {},
      };
    }
    // Default solid variant
    return {
      className: cn(
        "hover:opacity-90",
        disabled && "opacity-50 cursor-not-allowed"
      ),
      style: { backgroundColor: Colors.primary, color: "#fff" },
    };
  };

  const variantStyles = getVariantStyles();

  return (
    <button
      type={type}
      className={cn(
        `inline-flex items-center justify-center
        px-4 py-2 h-9 rounded-md font-medium shadow-sm
        transition-all duration-200 cursor-pointer select-none`,
        variantStyles.className,
        className
      )}
      style={variantStyles.style}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;
