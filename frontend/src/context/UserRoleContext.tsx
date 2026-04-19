import React, { createContext, useContext, useState, useEffect } from 'react';

export type UserRole = 'CUSTOMER' | 'AGENT' | 'MERCHANT';

interface UserRoleContextValue {
  role: UserRole;
  setRole: (role: UserRole) => void;
  isPurchaser: boolean;
  isCustomer: boolean;
  isMerchant: boolean;
}

const STORAGE_KEY = 'bobbuy_user_role';
const DEFAULT_ROLE: UserRole = 'CUSTOMER';

const UserRoleContext = createContext<UserRoleContextValue | undefined>(undefined);

export function UserRoleProvider({ children }: { children: React.ReactNode }) {
  const [role, setRoleState] = useState<UserRole>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    return (stored as UserRole) || DEFAULT_ROLE;
  });

  const setRole = (newRole: UserRole) => {
    setRoleState(newRole);
    localStorage.setItem(STORAGE_KEY, newRole);
  };

  const value = {
    role,
    setRole,
    isPurchaser: role === 'AGENT',
    isCustomer: role === 'CUSTOMER',
    isMerchant: role === 'MERCHANT',
  };

  return <UserRoleContext.Provider value={value}>{children}</UserRoleContext.Provider>;
}

export function useUserRole() {
  const context = useContext(UserRoleContext);
  if (!context) {
    throw new Error('useUserRole must be used within a UserRoleProvider');
  }
  return context;
}
