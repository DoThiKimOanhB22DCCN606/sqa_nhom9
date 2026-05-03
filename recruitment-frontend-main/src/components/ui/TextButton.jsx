const TextButton = ({ children, onClick, className = "", ...props }) => {
  return (
    <span
      onClick={onClick}
      className={`text-sm font-bold text-red-600 hover:text-red-700 cursor-pointer transition-colors ${className}`}
      {...props}
    >
      {children}
    </span>
  );
};

export default TextButton;
