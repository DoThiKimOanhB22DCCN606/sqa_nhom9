import React from "react";
import Button from "./Button";
import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ currentPage, totalPages, onPageChange }) {
  const isFirstPage = currentPage === 1;
  const isLastPage = currentPage === totalPages;
  const hasMultiplePages = totalPages > 1;

  return (
    <div className="flex items-center gap-1">
      {/* Previous Button */}
      {hasMultiplePages && !isFirstPage && (
        <Button
          variant="outline"
          className="h-8 w-8 p-0 ql"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirstPage}
        >
          {"<"}
        </Button>
      )}

      {/* Page Numbers */}
      {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => {
        const showPage =
          page === 1 ||
          page === totalPages ||
          (page >= currentPage - 1 && page <= currentPage + 1);

        const showEllipsisBefore = page === currentPage - 2 && currentPage > 3;
        const showEllipsisAfter =
          page === currentPage + 2 && currentPage < totalPages - 2;

        if (!showPage && !showEllipsisBefore && !showEllipsisAfter) {
          return null;
        }

        if (showEllipsisBefore || showEllipsisAfter) {
          return (
            <span
              key={`ellipsis-${page}`}
              className="px-2 text-gray-400 text-sm"
            >
              ...
            </span>
          );
        }

        return (
          <Button
            key={page}
            variant={page === currentPage ? "solid" : "outline"}
            className="h-8 w-8 p-0 text-sm"
            onClick={() => onPageChange(page)}
          >
            {page}
          </Button>
        );
      })}

      {/* Next Button */}
      {hasMultiplePages && !isLastPage && (
        <Button
          variant="outline"
          className="h-8 w-8 p-0"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLastPage}
        >
          {">"}
        </Button>
      )}
    </div>
  );
}
