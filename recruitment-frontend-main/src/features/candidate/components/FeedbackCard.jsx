import { useState } from "react";
import { MessageSquare, Send } from "lucide-react";
import Button from "../../../components/ui/Button";
import { useTranslation } from "react-i18next";

export default function FeedbackCard({
  feedback,
  rejectionReason,
  comments = [],
  onAddComment,
}) {
  const { t } = useTranslation();
  const [newComment, setNewComment] = useState("");
  const hasContent =
    feedback ||
    rejectionReason ||
    (Array.isArray(comments) && comments.length > 0);

  const handleSubmit = () => {
    const content = newComment.trim();
    if (!content) return;
    if (typeof onAddComment === "function") {
      onAddComment(content);
    }
    setNewComment("");
  };

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <MessageSquare size={18} />
        {t("candidates.feedback")}
      </h3>
      <div className="space-y-3">
        {feedback && (
          <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
            <p className="text-sm text-gray-700 whitespace-pre-wrap">
              {feedback}
            </p>
          </div>
        )}
        {Array.isArray(comments) && comments.length > 0 && (
          <div className="space-y-2">
            {comments.map((c) => (
              <div
                key={c.id}
                className="bg-white rounded-lg p-3 border border-gray-200"
              >
                <div className="text-xs text-gray-500 mb-1 font-medium">
                  {c.userName || t("candidates.user")}
                </div>
                <p className="text-sm text-gray-700 whitespace-pre-wrap">
                  {c.content}
                </p>
              </div>
            ))}
          </div>
        )}
        {!hasContent && (
          <div className="text-center py-4 text-gray-400">
            <MessageSquare size={32} className="mx-auto mb-2 opacity-50" />
            <p className="text-sm">{t("candidates.noFeedback")}</p>
          </div>
        )}
        <div className="mt-3">
          <div className="flex items-center justify-center gap-2">
            <input
              type="text"
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder={t("candidates.commentPlaceholder")}
              className="w-80 text-sm px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-200 focus:border-red-300"
            />
            <Button
              onClick={handleSubmit}
              disabled={!newComment.trim()}
              aria-label={t("candidates.addComment")}
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </div>
        {rejectionReason && (
          <div className="bg-red-50 rounded-lg p-3 border border-red-200">
            <div className="text-xs text-red-700 font-medium mb-1">
              {t("candidates.rejectionReason")}
            </div>
            <p className="text-sm text-red-800">{rejectionReason}</p>
          </div>
        )}
      </div>
    </div>
  );
}
