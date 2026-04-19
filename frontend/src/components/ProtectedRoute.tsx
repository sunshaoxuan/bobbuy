import React from 'react';
import { Navigate } from 'react-router-dom';
import { useUserRole, type UserRole } from '../context/UserRoleContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
  redirectTo?: string;
}

export default function ProtectedRoute({ children, allowedRoles, redirectTo }: ProtectedRouteProps) {
  const { role } = useUserRole();

  if (!allowedRoles.includes(role)) {
    const defaultRedirect = role === 'AGENT' ? '/dashboard' : '/';
    return <Navigate to={redirectTo || defaultRedirect} replace />;
  }

  return <>{children}</>;
}
