import React, { createContext, useContext, useState } from 'react';

export type UserRole = 'CUSTOMER' | 'AGENT' | 'MERCHANT';

interface UserRoleContextValue {
  role: UserRole;
  setRole: (role: UserRole) => void;
  isPurchaser: boolean;
  isCustomer: boolean;
  isMerchant: boolean;
}

const STORAGE_KEY = 'bobbuy_user_role';
const TEST_INJECT_KEY = 'bobbuy_test_role';
const DEFAULT_ROLE: UserRole = 'CUSTOMER';

const UserRoleContext = createContext<UserRoleContextValue | undefined>(undefined);

export function UserRoleProvider({ children }: { children: React.ReactNode }) {
  const resolveInitialRole = (): UserRole => {
    if (typeof window === 'undefined') {
      return DEFAULT_ROLE;
    }
    const params = new URLSearchParams(window.location.search);
    const queryRole = params.get('role');
    if (queryRole === 'CUSTOMER' || queryRole === 'AGENT' || queryRole === 'MERCHANT') {
      return queryRole;
    }
    const injected = localStorage.getItem(TEST_INJECT_KEY);
    if (injected === 'CUSTOMER' || injected === 'AGENT' || injected === 'MERCHANT') {
      return injected;
    }
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'CUSTOMER' || stored === 'AGENT' || stored === 'MERCHANT') {
      return stored;
    }
    return DEFAULT_ROLE;
  };

  const [role, setRoleState] = useState<UserRole>(() => {
    return resolveInitialRole();
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
