import { useState } from "react";
import Modal from "../../../components/ui/Modal";
import Button from "../../../components/ui/Button";
import TextInput from "../../../components/ui/TextInput";
import TextArea from "../../../components/ui/TextArea";
import LoadingOverlay from "../../../components/ui/LoadingOverlay";
import { useSendEmail } from "../../../hooks/useEmail";
import { Mail } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "react-toastify";

export default function SendEmailModal({ isOpen, onClose, recipientEmail, recipientName }) {
  const { t } = useTranslation();
  const defaultSubject = t("modals.emailSubjectTemplate", { name: recipientName });
  const [subject, setSubject] = useState(defaultSubject);
  const [message, setMessage] = useState("");
  const sendEmail = useSendEmail();

  const handleSend = async () => {
    if (!subject.trim() || !message.trim()) {
      return;
    }

    try {
      await sendEmail.mutateAsync({
        toEmail: recipientEmail,
        subject: subject,
        content: message,
        links: null,
        threadId: null,
        replyToId: null,
        sendViaGmail: true,
      });
      toast.success(t("toasts.sendEmailSuccess"));
      setSubject(defaultSubject);
      setMessage("");
      onClose();
    } catch (error) {
      console.error("Failed to send email:", error);
    }
  };

  const handleClose = () => {
    setSubject(defaultSubject);
    setMessage("");
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="lg">
      <div className="bg-white rounded-2xl shadow-2xl overflow-hidden relative">
        {sendEmail.isPending && <LoadingOverlay show={true} />}
        
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Mail className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">{t("modals.emailModalTitle")}</h3>
              <p className="text-sm text-gray-600">{t("modals.sendTo")}: {recipientEmail}</p>
            </div>
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-4">
          <TextInput
            label={t("modals.emailSubject")}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder={t("modals.emailSubjectPlaceholder")}
            required
          />

          <TextArea
            label={t("modals.emailContent")}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder={t("modals.emailContentPlaceholder")}
            rows={10}
            required
          />
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50 flex gap-3 justify-end">
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={sendEmail.isPending}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSend}
            disabled={sendEmail.isPending || !subject.trim() || !message.trim()}
          >
            {sendEmail.isPending ? t("modals.sending") : t("candidates.sendEmail")}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
