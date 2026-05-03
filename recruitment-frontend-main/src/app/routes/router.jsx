import { createBrowserRouter } from "react-router-dom";
import Login from "../../features/auth/Login";
import Layout from "../../components/Layout";
import RecruitmentRequests from "../../features/recruitment-requests/RecruitmentRequests";
import JobPositions from "../../features/job-positions/JobPositions";
import Calendar from "../../features/calendar/Calendar";
import Candidate from "../../features/candidate/Candidate";
import CandidateDetail from "../../features/candidate/CandidateDetail";
import Email from "../../features/email/Email";
import Users from "../../features/users/Users";
import UserForm from "../../features/users/UserForm";
import Employees from "../../features/employees/Employees";
import EmployeeForm from "../../features/employees/EmployeeForm";
import RecruitmentRequestAdd from "../../features/recruitment-requests/RecruitmentRequestAdd";
import RedirectIfAuth from "../../components/RedirectIfAuth";
import ProtectedRoute from "../../components/ProtectedRoute";
import RoleBasedGuard from "../../components/RoleBasedGuard";
import JobPositionsAdd from "../../features/job-positions/JobPositionAdd";
import JobPositionCandidates from "../../features/job-positions/JobPositionCandidates";
import Home from "../../features/dashboard/Home";
import Roles from "../../features/roles/Roles";
import RoleForm from "../../features/roles/RoleForm";
import Workflows from "../../features/workflows/Workflows";
import WorkflowForm from "../../features/workflows/WorkflowForm";
import Offers from "../../features/offers/Offers";
import OfferForm from "../../features/offers/OfferForm";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: (
      <RedirectIfAuth>
        <Login />
      </RedirectIfAuth>
    ),
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <Layout />
      </ProtectedRoute>
    ),
    children: [
      {
        path: "/",
        element: <Home />,
      },
      {
        path: "/users",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <Users />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/users/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <UserForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/users/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <UserForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/roles",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <Roles />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/roles/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <RoleForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/roles/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <RoleForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/workflows",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <Workflows />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/workflows/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <WorkflowForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/workflows/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN"]}>
            <WorkflowForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/employees",
        element: (
          <RoleBasedGuard 
            requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}
            requiredDepartmentIds={[2]}
            exemptRoles={["ADMIN", "CEO"]}
          >
            <Employees />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/employees/new",
        element: (
          <RoleBasedGuard 
            requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}
            requiredDepartmentIds={[2]}
            exemptRoles={["ADMIN", "CEO"]}
          >
            <EmployeeForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/employees/:id",
        element: (
          <RoleBasedGuard 
            requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}
            requiredDepartmentIds={[2]}
            exemptRoles={["ADMIN", "CEO"]}
          >
            <EmployeeForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/recruitment-requests",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <RecruitmentRequests />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/recruitment-requests/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <RecruitmentRequestAdd />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/recruitment-requests/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <RecruitmentRequestAdd />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/job-positions",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <JobPositions />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/job-positions/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <JobPositionsAdd />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/job-positions/:id/edit",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <JobPositionsAdd />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/job-positions/:id/candidates",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <JobPositionCandidates />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/calendar",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <Calendar />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/candidates",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <Candidate />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/candidates/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <CandidateDetail />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/email",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <Email />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/offers",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <Offers />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/offers/new",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <OfferForm />
          </RoleBasedGuard>
        ),
      },
      {
        path: "/offers/:id",
        element: (
          <RoleBasedGuard requiredRoles={["ADMIN", "CEO", "MANAGER", "STAFF"]}>
            <OfferForm />
          </RoleBasedGuard>
        ),
      },
    ],
  },
]);
