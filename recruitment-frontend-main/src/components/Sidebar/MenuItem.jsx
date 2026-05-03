import { NavLink } from "react-router-dom";

const MenuItem = ({ link, text, icon, isCollapsed }) => {
  return (
    <li>
      <NavLink
        to={link}
        replace
        state={{ fom: "menu" }}
        className={({ isActive }) => `
      relative flex items-center rounded-md px-2 py-2 my-2
      justify-center

      ${isActive ? "bg-red-100/50 text-[#af1b1b]" : "text-gray-900 hover:text-black"}
      `}
      >
        {icon}
        <span
          className={`overflow-hidden transition-all duration-300
        ${isCollapsed ? "w-0" : "w-40 ml-2"}`}
        >
          {text}
        </span>
      </NavLink>
    </li>
  );
};

export default MenuItem;
