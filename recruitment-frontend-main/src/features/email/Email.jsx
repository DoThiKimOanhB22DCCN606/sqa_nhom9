import React, { useState, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Send, Mail, Star, Inbox, FileText } from "lucide-react";
import ContentHeader from "../../components/ui/ContentHeader";
import TextInput from "../../components/ui/TextInput";
import TextArea from "../../components/ui/TextArea";
import RichTextEditor from "../../components/ui/RichTextEditor";
import Button from "../../components/ui/Button";
import LoadingContent from "../../components/ui/LoadingContent";
import EmptyState from "../../components/ui/EmptyState";
import { useSendEmail, useInboxEmails, useSentEmails } from "../../hooks/useEmail";
import { formatDateTime } from "../../utils/utils";
import { toast } from "react-toastify";

export default function Email() {
  const { t } = useTranslation();
  const formRef = useRef(null);
  const [selectedTab, setSelectedTab] = useState("inbox");
  const [selectedEmail, setSelectedEmail] = useState(null);
  const [form, setForm] = useState({
    toEmail: "",
    subject: "",
    content: "",
  });

  const sendEmail = useSendEmail();
  const {
    data: inboxEmails = [],
    isLoading: inboxLoading,
    isError: inboxError,
  } = useInboxEmails();
  const {
    data: sentEmails = [],
    isLoading: sentLoading,
    isError: sentError,
  } = useSentEmails();

  const onChange = (field) => (e) => {
    const value = e?.target ? e.target.value : e;
    setForm((s) => ({ ...s, [field]: value }));
  };

  const handleSubmit = (e) => {
    e?.preventDefault();

    const requiredFieldsMap = {
      toEmail: t("toEmail"),
      subject: t("subject"),
      content: t("content"),
    };

    for (const [field, label] of Object.entries(requiredFieldsMap)) {
      const value = form[field];
      if (value === null || value === undefined || String(value).trim() === "") {
        toast.error(`${label} ${t("isRequired")}`);
        return;
      }
    }

    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(form.toEmail)) {
      toast.error(t("invalidEmail"));
      return;
    }

    const payload = {
      toEmail: form.toEmail,
      subject: form.subject,
      content: form.content,
      links: null,
      threadId: null,
      replyToId: null,
      sendViaGmail: true,
    };

    sendEmail.mutate(payload, {
      onSuccess: () => {
        setForm({
          toEmail: "",
          subject: "",
          content: "",
        });
      },
    });
  };

  const handleReset = () => {
    setForm({
      toEmail: "",
      subject: "",
      content: "",
    });
  };

  const renderContent = (content) => {
    if (!content) return "";
    
    // If content contains HTML tags, strip out styles and unnecessary attributes
    if (content.includes('<') && content.includes('>')) {
      // Remove DOCTYPE, HTML structure, and all unwanted tags
      let cleaned = content
        .replace(/<!DOCTYPE[^>]*>/gi, '')
        .replace(/<html[^>]*>/gi, '')
        .replace(/<\/html>/gi, '')
        .replace(/<head[^>]*>[\s\S]*?<\/head>/gi, '')
        .replace(/<meta[^>]*>/gi, '')
        .replace(/<link[^>]*>/gi, '')
        .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
        .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
        .replace(/<body[^>]*>/gi, '')
        .replace(/<\/body>/gi, '')
        .replace(/style="[^"]*"/gi, '')
        .replace(/bgcolor="[^"]*"/gi, '')
        .replace(/class="[^"]*"/gi, '')
        .replace(/border="[^"]*"/gi, '')
        .replace(/cellpadding="[^"]*"/gi, '')
        .replace(/cellspacing="[^"]*"/gi, '')
        .replace(/width="[^"]*"/gi, '')
        .replace(/height="[^"]*"/gi, '')
        .replace(/align="[^"]*"/gi, '')
        .replace(/valign="[^"]*"/gi, '')
        .replace(/<table[^>]*>/gi, '')
        .replace(/<\/table>/gi, '')
        .replace(/<tbody[^>]*>/gi, '')
        .replace(/<\/tbody>/gi, '')
        .replace(/<tr[^>]*>/gi, '')
        .replace(/<\/tr>/gi, '')
        .replace(/<td[^>]*>/gi, '')
        .replace(/<\/td>/gi, '<br/>')
        .replace(/<div[^>]*>/gi, '')
        .replace(/<\/div>/gi, '<br/>')
        .replace(/<span[^>]*>/gi, '')
        .replace(/<\/span>/gi, '')
        .replace(/<font[^>]*>/gi, '')
        .replace(/<\/font>/gi, '')
        .replace(/(<br\s*\/?>\s*){3,}/gi, '<br/><br/>') // Remove excessive line breaks
        .trim();
      
      return cleaned;
    }
    
    // Convert markdown-style formatting to HTML
    let html = content
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>') // Bold
      .replace(/_(.+?)_/g, '<em>$1</em>') // Italic
      .replace(/^• (.+)$/gm, '<li>$1</li>') // Bullet points
      .replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>') // Numbered lists
      .replace(/\n/g, '<br/>'); // Line breaks
    
    // Wrap consecutive <li> in <ul>
    html = html.replace(/(<li>.*?<\/li>\s*)+/g, (match) => {
      if (match.includes('<li>')) {
        return `<ul class="list-disc list-inside space-y-1 ml-4">${match}</ul>`;
      }
      return match;
    });
    
    return html;
  };

  return (
    <div className="flex flex-col h-full">
      <ContentHeader
        title={t("emailPage")}
        actions={
          <Button
            onClick={() => {
              setSelectedTab("inbox");
              setSelectedEmail(null);
            }}
          >
            <Send className="h-4 w-4 mr-2" />
            {t("composeEmail")}
          </Button>
        }
      />

      <div className="flex-1 flex gap-4 mt-4 min-h-0">
        <div className="w-80 flex-shrink-0 bg-white rounded-xl shadow overflow-hidden flex flex-col">
          <div className="p-3 border-b border-gray-200">
            <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
              <div
                onClick={() => setSelectedTab("inbox")}
                className={`
                  flex-1 px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer flex items-center justify-center
                  ${
                    selectedTab === "inbox"
                      ? "bg-white text-red-600 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }
                `}
              >
                {t("inbox")}
              </div>
              <div
                onClick={() => setSelectedTab("sent")}
                className={`
                  flex-1 px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer flex items-center justify-center
                  ${
                    selectedTab === "sent"
                      ? "bg-white text-red-600 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }
                `}
              >
                {t("sent")}
              </div>
              <div
                onClick={() => setSelectedTab("drafts")}
                className={`
                  flex-1 px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer flex items-center justify-center
                  ${
                    selectedTab === "drafts"
                      ? "bg-white text-red-600 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }
                `}
              >
                {t("drafts")}
              </div>
            </div>
          </div>

          <div className="p-3 border-b border-gray-200">
            <div className="relative">
              <input
                type="text"
                placeholder={t("searchEmail")}
                className="w-full px-3 py-2 pl-9 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            </div>
          </div>

          <div className="flex-1 overflow-y-auto">
            {(() => {
              const loading = selectedTab === "inbox" ? inboxLoading : sentLoading;
              const error = selectedTab === "inbox" ? inboxError : sentError;
              const emails = selectedTab === "inbox" ? inboxEmails : sentEmails;

              if (loading) {
                return (
                  <div className="p-4">
                    <LoadingContent />
                  </div>
                );
              }

              if (error) {
                return (
                  <div className="p-4 text-sm text-red-600">
                    {t("failedToLoadEmails")}
                  </div>
                );
              }

              if (!emails || emails.length === 0) {
                return (
                  <div className="p-4">
                    <EmptyState title={t("noEmails")} />
                  </div>
                );
              }

              return emails.map((email) => {
                const id = email.id || email._id || email.uuid || `${email.subject}-${email.createdAt || email.date || email.sentAt || email.receivedAt}`;
                const from = email.fromEmail || email.from || email.sender || email.userFrom || "";
                const to = email.toEmail || email.to || email.receiver || email.userTo || "";
                const subject = email.subject || email.title || t("common.noTitle");
                const rawPreview = email.preview || email.content || email.body || "";
                // Clean HTML from preview
                const preview = rawPreview.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
                const date = email.createdAt || email.date || email.sentAt || email.receivedAt || "";
                const starred = email.starred || false;

                const item = { id, from, to, subject, preview, date, starred };
                
                // Show from for inbox, to for sent
                const displayName = selectedTab === "sent" ? item.to : item.from;

                return (
                  <div
                    key={item.id}
                    onClick={() => setSelectedEmail(item)}
                    className={`p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors ${
                      selectedEmail?.id === item.id ? "bg-blue-50" : ""
                    }`}
                  >
                    <div className="flex items-start justify-between mb-1">
                      <span className="text-sm font-medium text-gray-900">{displayName}</span>
                      {item.starred && (
                        <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
                      )}
                    </div>
                    <p className="text-sm font-medium text-gray-800 mb-1 truncate">{item.subject}</p>
                    <p className="text-xs text-gray-600 line-clamp-2 mb-1">{item.preview}</p>
                    <span className="text-xs text-gray-500">{formatDateTime(item.date)}</span>
                  </div>
                );
              });
            })()}
          </div>
        </div>

        <div className="flex-1 bg-white rounded-xl shadow overflow-auto">
          {selectedTab === "inbox" && !selectedEmail ? (
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-gray-900">
                  {t("composeNewEmail")}
                </h2>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={handleReset} type="button" disabled={sendEmail.isPending}>
                    {t("reset")}
                  </Button>
                  <Button onClick={() => formRef.current?.requestSubmit()} disabled={sendEmail.isPending}>
                    <Send className="h-4 w-4 mr-2" />
                    {t("send")}
                  </Button>
                </div>
              </div>
              {sendEmail.isPending ? (
                <div className="flex items-center justify-center py-12">
                  <LoadingContent />
                </div>
              ) : (
                <form ref={formRef} onSubmit={handleSubmit}>
                  <div className="space-y-4">
                    <TextInput
                      label={t("to")}
                      type="email"
                      value={form.toEmail}
                      onChange={onChange("toEmail")}
                      required
                      placeholder="recipient@email.com"
                    />

                    <TextInput
                      label={t("subject")}
                      type="text"
                      value={form.subject}
                      onChange={onChange("subject")}
                      required
                      placeholder={t("subjectPlaceholder")}
                    />

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        {t("content")}
                      </label>
                      <RichTextEditor
                        value={form.content}
                        onChange={onChange("content")}
                        placeholder={t("contentPlaceholder")}
                      />
                    </div>
                  </div>
                </form>
              )}
            </div>
          ) : selectedEmail ? (
            <div>
              <div className="p-6 border-b border-gray-200">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h2 className="text-xl font-semibold text-gray-900 mb-2">
                      {selectedEmail.subject}
                    </h2>
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <span className="font-medium">{selectedEmail.from}</span>
                      <span>•</span>
                      <span>{formatDateTime(selectedEmail.date)}</span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setForm({
                          toEmail: selectedEmail.from,
                          subject: `Re: ${selectedEmail.subject}`,
                          content: "",
                        });
                        setSelectedTab("inbox");
                        setSelectedEmail(null);
                      }}
                    >
                      {t("reply")}
                    </Button>
                  </div>
                </div>
              </div>
              <div className="flex-1 p-6 overflow-auto">
                <div 
                  className="text-gray-700 prose prose-sm max-w-none"
                  dangerouslySetInnerHTML={{ __html: renderContent(selectedEmail.preview) }}
                />
              </div>
            </div>
          ) : selectedTab === "compose" ? (
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-gray-900">
                  {t("composeNewEmail")}
                </h2>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={handleReset} type="button" disabled={sendEmail.isPending}>
                    {t("reset")}
                  </Button>
                  <Button onClick={() => formRef.current?.requestSubmit()} disabled={sendEmail.isPending}>
                    <Send className="h-4 w-4 mr-2" />
                    {t("send")}
                  </Button>
                </div>
              </div>
              {sendEmail.isPending ? (
                <div className="flex items-center justify-center py-12">
                  <LoadingContent />
                </div>
              ) : (
                <form ref={formRef} onSubmit={handleSubmit}>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        {t("to")}
                      </label>
                      <input
                        type="email"
                        value={form.toEmail}
                        onChange={onChange("toEmail")}
                        required
                        placeholder="recipient@email.com"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        {t("subject")}
                      </label>
                      <input
                        type="text"
                        value={form.subject}
                        onChange={onChange("subject")}
                        required
                        placeholder={t("subjectPlaceholder")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        {t("content")}
                      </label>
                      <RichTextEditor
                        value={form.content}
                        onChange={onChange("content")}
                        placeholder={t("contentPlaceholder")}
                      />
                    </div>
                  </div>
                </form>
              )}
            </div>
          ) : (
            
            <div className="flex-1 flex items-center justify-center">
              <div className="text-center text-gray-500">
                <FileText className="h-12 w-12 mx-auto mb-2 text-gray-400" />
                <p>{t("noEmails")}</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
