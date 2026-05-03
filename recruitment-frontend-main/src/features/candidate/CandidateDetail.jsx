import { useParams, useNavigate } from "react-router-dom";
import { useCandidate, useAddCandidateComment, useUpdateCandidateStatus } from "./hooks/useCandidates";
import { useAuth } from "../../context/AuthContext";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import CandidateInfoCard from "./components/CandidateInfoCard";
import JobPositionInfoCard from "./components/JobPositionInfoCard";
import UpcomingScheduleCard from "./components/UpcomingScheduleCard";
import FeedbackCard from "./components/FeedbackCard";
import NotesCard from "./components/NotesCard";
import ApplicationProgress from "./components/ApplicationProgress";
import ResumeViewer from "./components/ResumeViewer";
import { Calendar, Mail } from "lucide-react";
import LoadingContent from "../../components/ui/LoadingContent";
import { useState } from "react";
import SendEmailModal from "./components/SendEmailModal";
import CreateEventModal from "../calendar/components/CreateEventModal";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";

export default function CandidateDetail() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isHR = user?.department?.id === 2;
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [showCreateEventModal, setShowCreateEventModal] = useState(false);
  const { data, isLoading, isError, error } = useCandidate(id, {
    refetchOnMount: 'always',
    refetchOnWindowFocus: true,
    staleTime: 0,
    cacheTime: 0,
  });
  const addComment = useAddCandidateComment();
  const updateStatus = useUpdateCandidateStatus();

  const CANDIDATE_STATUSES = [
    { id: "SUBMITTED", label: t("statuses.submitted"), color: "#3B82F6" },
    { id: "REVIEWING", label: t("statuses.reviewing"), color: "#6366F1" },
    { id: "INTERVIEW", label: t("statuses.interview"), color: "#F59E0B" },
    // { id: "OFFER", label: t("statuses.offer"), color: "#8B5CF6" },
    { id: "HIRED", label: t("statuses.hired"), color: "#10B981" },
    { id: "ARCHIVED", label: t("statuses.archived"), color: "#6B7280" },
    { id: "REJECTED", label: t("statuses.rejected"), color: "#EF4444" },
  ];

  const candidate = data?.data;

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={t("candidateDetail")}
          subtitle={t("loading")}
          onBack={() => navigate("/candidates")}
        />
        <div className="flex-1 flex items-center justify-center mt-4">
          <LoadingContent />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-4">
        <div className="text-red-500">{t("error")}: {error?.message}</div>
        <Button onClick={() => navigate("/candidates")}>{t("common.back")}</Button>
      </div>
    );
  }

  if (!candidate) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-4">
        <div className="text-gray-500">{t("candidates.notFound")}</div>
        <Button onClick={() => navigate("/candidates")}>{t("common.back")}</Button>
      </div>
    );
  }

  const displayName = candidate.name || candidate.candidateName || "-";
  const displayEmail = candidate.email || candidate.candidateEmail || "-";
  const displayPhone = candidate.phone || candidate.candidatePhone || "-";
  const jobPositionTitle = candidate.jobPosition?.title || "-";
  const departmentName = candidate.jobPosition?.departmentName || "-";
  const salaryMin = candidate.jobPosition?.salaryMin;
  const salaryMax = candidate.jobPosition?.salaryMax;
  const currency = candidate.jobPosition?.currency;
  const experienceLevel = candidate.jobPosition?.experienceLevel;
  const yearsOfExperience = candidate.jobPosition?.yearsOfExperience;

  const handleStatusChange = (newStatus) => {
    updateStatus.mutate(
      { id: candidate.id, status: newStatus },
      {
        onSuccess: () => {
          toast.success(t("toasts.updateStatusSuccess"));
        },
      }
    );
  };

  return (
    <div className="flex flex-col h-full">
      <ContentHeader
        title={t("candidateDetail")}
        subtitle={displayName}
        onBack={() => navigate("/candidates")}
        actions={
          isHR && candidate.status !== "REJECTED" && candidate.status !== "ARCHIVED" && (
            <>
              <Button
                variant="outline"
                onClick={() => setShowEmailModal(true)}
              >
                <Mail className="h-4 w-4 mr-2" />
                {t("candidates.sendEmail")}
              </Button>
              <Button onClick={() => setShowCreateEventModal(true)}>
                <Calendar className="h-4 w-4 mr-2" />
                {t("createSchedule")}
              </Button>
            </>
          )
        }
      />

      <div className="flex-1 mt-4 overflow-auto">
        <div className="grid grid-cols-12 gap-4">
          <div className="col-span-4 space-y-4">
            <CandidateInfoCard
              displayName={displayName}
              displayEmail={displayEmail}
              displayPhone={displayPhone}
              appliedDate={candidate.appliedDate}
            />

            <JobPositionInfoCard
              jobPositionTitle={jobPositionTitle}
              departmentName={departmentName}
              salaryMin={salaryMin}
              salaryMax={salaryMax}
              currency={currency}
              experienceLevel={experienceLevel}
              yearsOfExperience={yearsOfExperience}
            />

            <UpcomingScheduleCard schedules={candidate.upcomingSchedules} />

            <NotesCard notes={candidate.notes} />

            <FeedbackCard
              feedback={candidate.feedback}
              rejectionReason={candidate.rejectionReason}
              comments={Array.isArray(candidate.comments) ? candidate.comments : []}
              onAddComment={(content) =>
                addComment.mutate({ applicationId: candidate.id, content }, {
                  onSuccess: () => {
                    toast.success(t("toasts.submitFeedbackSuccess"));
                  }
                })
              }
            />
          </div>

          <div className="col-span-8 space-y-4">
            <ApplicationProgress 
              status={candidate.status}
              isHR={isHR}
              onStatusChange={handleStatusChange}
              isUpdating={updateStatus.isPending}
              statuses={CANDIDATE_STATUSES}
            />

            <ResumeViewer resumeUrl={candidate.resumeUrl} />
          </div>
        </div>
      </div>

      <SendEmailModal
        isOpen={showEmailModal}
        onClose={() => setShowEmailModal(false)}
        recipientEmail={displayEmail}
        recipientName={displayName}
      />

      <CreateEventModal
        isOpen={showCreateEventModal}
        onClose={() => setShowCreateEventModal(false)}
        defaultCandidate={{
          id: candidate.id,
          name: displayName,
          departmentId: candidate.jobPosition?.departmentId,
        }}
      />
    </div>
  );
}
