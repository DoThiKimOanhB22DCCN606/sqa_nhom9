// LangToggle.jsx
import { useTranslation } from "react-i18next";
import vnFlag from "../../assets/images/vn_flag.svg";
import ukFlag from "../../assets/images/uk_flag.svg";

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const lng = (i18n.resolvedLanguage || "en").split("-")[0];
  const isVI = lng === "vi";

  const toggle = () => i18n.changeLanguage(isVI ? "en" : "vi");

  return (
    <div
      onClick={toggle}
      className="relative inline-flex items-center w-[64px]
      h-[32px] rounded-full select-none  p-2 cursor-pointer"
      style={{
        background: "#EEF1F5",
        border: "none",
        transition: "background .25s ease, box-shadow .25s ease",
      }}
    >
      <span
        className={`flex-1 text-sm font-semibold tracking-wide
            transition-colors `}
      >
        vi
      </span>
      <span
        className={`text-sm font-semibold tracking-wide
             transition-colors `}
      >
        en
      </span>
      <span
        className="absolute  w-[28px] h-[28px] rounded-full bg-white"
        style={{
          left: isVI ? 2 : "auto",
          right: isVI ? "auto" : 2,
          boxShadow:
            "0 10px 18px rgba(0,0,0,.20), inset 6px 6px 12px rgba(0,0,0,.05), inset -6px -6px 12px rgba(255,255,255,.9)",
          transition: "left 240ms ease, box-shadow 240ms ease",
        }}
      >
        
          <img
            src={isVI ? vnFlag : ukFlag}
            className="w-full h-full rounded-full object-cover" 
            alt={isVI ? "Vietnam flag" : "UK flag"}
          />
      </span>
    </div>
  );
}
