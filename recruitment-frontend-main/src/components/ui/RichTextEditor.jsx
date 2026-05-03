import { Bold, Italic, List, ListOrdered } from "lucide-react";

export default function RichTextEditor({
  value,
  onChange,
  placeholder,
  disabled,
}) {
  const insertFormatting = (before, after = "") => {
    const textarea = document.querySelector("[data-rich-editor]");
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end) || "text";
    const newText =
      value.substring(0, start) +
      before +
      selectedText +
      after +
      value.substring(end);

    onChange(newText);

    setTimeout(() => {
      textarea.focus();
      textarea.selectionStart = start + before.length;
      textarea.selectionEnd = start + before.length + selectedText.length;
    }, 0);
  };

  return (
    <div className="flex flex-col gap-2 bg-white">
      <div className="flex gap-1 flex-wrap bg-gray-50 p-2 rounded-md border border-gray-300">
        <button
          type="button"
          onClick={() => insertFormatting("**", "**")}
          disabled={disabled}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-100 hover:border-red-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
          title="Bold (Ctrl+B)"
        >
          <Bold className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() => insertFormatting("_", "_")}
          disabled={disabled}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-100 hover:border-red-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
          title="Italic (Ctrl+I)"
        >
          <Italic className="h-4 w-4" />
        </button>
        <div className="w-px bg-gray-300 mx-1" />
        <button
          type="button"
          onClick={() =>
            insertFormatting("• Item 1\n• Item 2\n• Item 3\n\n", "")
          }
          disabled={disabled}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-100 hover:border-red-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
          title="Bullet List"
        >
          <List className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() =>
            insertFormatting("1. Item 1\n2. Item 2\n3. Item 3\n\n", "")
          }
          disabled={disabled}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-100 hover:border-red-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
          title="Numbered List"
        >
          <ListOrdered className="h-4 w-4" />
        </button>
      </div>
      <div className="text-xs text-gray-500 px-2">
        Hỗ trợ: **bold** _italic_ • bullet 1. numbered
      </div>
      <textarea
        data-rich-editor
        placeholder={placeholder}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="border border-gray-300 p-3 rounded-md text-gray-700 min-h-[150px] resize-y focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed font-mono text-sm"
      />
    </div>
  );
}
