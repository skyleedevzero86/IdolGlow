import { Navigate, useLocation, useParams } from "react-router-dom";
import { glowCommunityPostPath } from "../../../shared/data/glowCommunityApi";

export function GlowCommunityRedirect() {
  const { postId } = useParams<{ postId: string }>();
  const location = useLocation();
  const id = Number(postId);
  if (!Number.isInteger(id) || id <= 0) {
    return <Navigate to="/glow?tab=local" replace />;
  }
  return (
    <Navigate to={`${glowCommunityPostPath(id)}${location.hash}`} replace />
  );
}

export default GlowCommunityRedirect;
