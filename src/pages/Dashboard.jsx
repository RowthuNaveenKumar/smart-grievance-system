export default function Dashboard() {
  return (
    <div className="page-container">

      <h1 className="page-title">Dashboard</h1>

      <div style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit,minmax(250px,1fr))",
        gap: "20px"
      }}>

        <div className="ui-card">
          <h3>Submit Complaint</h3>
          <p>Create a new grievance request</p>
        </div>

        <div className="ui-card">
          <h3>Track Complaints</h3>
          <p>Check status of your complaints</p>
        </div>

        <div className="ui-card">
          <h3>Resolved Issues</h3>
          <p>View completed complaints</p>
        </div>

      </div>

    </div>
  );
}