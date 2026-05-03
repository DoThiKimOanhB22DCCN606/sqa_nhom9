import StatusBadge from "../../../components/ui/StatusBadge";

export default function RequestCard({ request }) {
    return (
        <div className="p-4 rounded-lg shadow bg-white">
            <h4 className="font-semibold">{request.position}</h4>
            <p>{request.department}</p>
            <p>{request.requestedBy}</p>
            <p>{request.dateRequested}</p>
            <StatusBadge status={request.status} />
        </div>
    );
}