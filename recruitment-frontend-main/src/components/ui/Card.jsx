const Card = ({ children }) => {
  return (
    <div
      className="flex flex-col justify-center items-center
    bg-white rounded-xl shadow-md p-8 border border-[#f3f3f3]"
    >
      {children}
    </div>
  );
};

export default Card;
