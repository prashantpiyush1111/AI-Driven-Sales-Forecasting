import useAuth from "../auth/useAuth";

function Navbar() {
  const { user, logout } = useAuth();

  return (
    <header className="navbar">
      <div>
        <h2>Business Intelligence</h2>
      </div>

      <div className="user-section">
        <span>Welcome, {user?.username ?? "User"}</span>
        {user?.role && <span className="role-badge">{user.role}</span>}
        <button type="button" onClick={logout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default Navbar;
