// Turns an axios error into a message that is safe to show to the user.
export function getErrorMessage(error, fallback = "Something went wrong. Please try again.") {
  if (!error?.response) {
    return "Cannot reach the server. Check that the backend is running.";
  }

  const { status, data } = error.response;

  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors).join(". ");
  }
  if (status === 429) {
    return data?.message || "Too many attempts. Please try again later.";
  }
  if (status === 403) {
    return "You do not have permission to do that.";
  }
  return data?.message || fallback;
}
