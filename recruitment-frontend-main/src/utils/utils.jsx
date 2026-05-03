import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function formatNumber(value) {
  if (!value && value !== 0) return "";
  const num = typeof value === "string" ? parseFloat(value.replace(/,/g, "")) : value;
  if (isNaN(num) || num < 0) return "";
  return num.toLocaleString("en-US");
}

export function parseFormattedNumber(formattedValue) {
  if (!formattedValue) return null;
  const num = parseFloat(String(formattedValue).replace(/,/g, ""));
  return isNaN(num) || num < 0 ? null : num;
}

export function formatSalary(amount, notAvailableText = "N/A") {
  if (!amount && amount !== 0) return notAvailableText;

  if (amount >= 1000000) {
    const millions = amount / 1000000;
    // Nếu là số nguyên triệu thì không hiển thị số lẻ
    if (millions % 1 === 0) {
      return `${millions.toFixed(0)} triệu`;
    }
    return `${millions.toFixed(1)} triệu`;
  } else if (amount >= 1000) {
    return `${(amount / 1000).toFixed(0)} nghìn`;
  } else {
    return `${amount} đ`;
  }
}

// Format ISO date/time to a readable string
// Default: vi-VN locale, e.g., 21/11/2025, 10:50
export function formatDateTime(input, options) {
  if (!input) return "";
  try {
    const date = typeof input === "string" || typeof input === "number" ? new Date(input) : input;
    if (isNaN(date.getTime())) return "";
    const fmt = new Intl.DateTimeFormat("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      ...options,
    });
    return fmt.format(date);
  } catch (e) {
    return "";
  }
}

// Lightweight markdown-like parser used for rich text fields
export const parseMarkdown = (content) => {
  if (!content) return "";

  let html = String(content)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  html = html
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/_(.+?)_/g, "<em>$1</em>");

  const lines = html.split("\n");
  let result = "";
  let inUL = false;
  let inOL = false;

  const closeLists = () => {
    if (inUL) {
      result += "</ul>";
      inUL = false;
    }
    if (inOL) {
      result += "</ol>";
      inOL = false;
    }
  };

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line) {
      closeLists();
      continue;
    }

    if (line.startsWith("•")) {
      if (!inUL) {
        closeLists();
        result += "<ul class='list-disc list-inside ml-4 my-2'>";
        inUL = true;
      }
      result += "<li>" + line.substring(1).trim() + "</li>";
    } else if (/^\d+\./.test(line)) {
      if (!inOL) {
        closeLists();
        result += "<ol class='list-decimal list-inside ml-4 my-2'>";
        inOL = true;
      }
      result += "<li>" + line.replace(/^\d+\.\s*/, "") + "</li>";
    } else {
      closeLists();
      result += "<p class='my-2'>" + line + "</p>";
    }
  }

  closeLists();
  return result;
};
