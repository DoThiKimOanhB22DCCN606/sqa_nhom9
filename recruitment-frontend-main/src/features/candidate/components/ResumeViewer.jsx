import { FileText, Download } from "lucide-react";
import { useTranslation } from "react-i18next";

export default function ResumeViewer({ resumeUrl }) {
  const { t } = useTranslation();
  const handleDownload = () => {
    if (!resumeUrl) return;
    
    // Extract filename from URL or use default
    const filename = resumeUrl.split('/').pop() || 'resume.pdf';
    
    // Create a temporary link and trigger download
    const link = document.createElement('a');
    link.href = resumeUrl;
    link.download = filename;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <FileText size={18} />
        {t("candidates.resume")}
      </h3>
      {resumeUrl ? (
        <div className="space-y-4">
          <div className="flex gap-2">
            <a
              href={resumeUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors text-sm font-medium text-gray-700"
            >
              <FileText size={16} />
              {t("candidates.openInNewTab")}
            </a>
            <button
              onClick={handleDownload}
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors text-sm font-medium text-blue-700"
            >
              <Download size={16} />
              {t("candidates.download")}
            </button>
          </div>
          <div
            className="border border-gray-200 rounded-lg overflow-hidden bg-gray-50"
            style={{ height: "600px" }}
          >
            <iframe src={resumeUrl} className="w-full h-full" title="Resume" />
          </div>
        </div>
      ) : (
        <div className="text-center py-12 text-gray-400 border-2 border-dashed border-gray-200 rounded-lg">
          <FileText size={48} className="mx-auto mb-3 opacity-50" />
          <p className="text-sm">{t("candidates.noResume")}</p>
        </div>
      )}
    </div>
  );
}
