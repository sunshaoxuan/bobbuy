import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import {
  clearAuthSession,
  getStoredAccessToken,
  getStoredUser,
  getTestInjectedRole,
  getTestInjectedUser,
  storeAuthSession,
  subscribeToAuthChanges,
  type AuthenticatedUser,
  type UserRole
} from '../authStorage';

interface UserRoleContextValue {
  role: UserRole;
  user: AuthenticatedUser | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (username: string, password: string) => Promise<AuthenticatedUser>;
  logout: () => void;
  isPurchaser: boolean;
  isCustomer: boolean;
  isMerchant: boolean;
}

const DEFAULT_ROLE: UserRole = 'CUSTOMER';

const UserRoleContext = createContext<UserRoleContextValue | undefined>(undefined);

function getInjectedUser(role: UserRole): AuthenticatedUser {
  const injectedUser = getTestInjectedUser();
  const fallbackId = role === 'AGENT' ? 1000 : role === 'CUSTOMER' ? 1001 : 1002;
  return {
    id: Number(injectedUser) || fallbackId,
    username: injectedUser ?? `test-${role.toLowerCase()}`,
    name: injectedUser ?? `Test ${role.toLowerCase()}`,
    role
  };
}

function readSessionUser(): AuthenticatedUser | null {
  const storedUser = getStoredUser();
  if (storedUser && getStoredAccessToken()) {
    return storedUser;
  }
  const injectedRole = getTestInjectedRole();
  return injectedRole ? getInjectedUser(injectedRole) : null;
}

export function UserRoleProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(() => readSessionUser());
  const [loading, setLoading] = useState(() => Boolean(getStoredAccessToken()));

  const syncFromStorage = useCallback(() => {
    setUser(readSessionUser());
  }, []);

  useEffect(() => {
    syncFromStorage();
    return subscribeToAuthChanges(syncFromStorage);
  }, [syncFromStorage]);

  useEffect(() => {
    const accessToken = getStoredAccessToken();
    if (!accessToken) {
      setLoading(false);
      return;
    }
    let active = true;
    setLoading(true);
    api.auth
      .me()
      .then((currentUser) => {
        if (!active) {
          return;
        }
        storeAuthSession(accessToken, currentUser);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        clearAuthSession();
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setLoading(true);
    try {
      const loginResult = await api.auth.login({ username, password });
      storeAuthSession(loginResult.accessToken, loginResult.user);
      const currentUser = await api.auth.me();
      storeAuthSession(loginResult.accessToken, currentUser);
      return currentUser;
    } catch (error) {
      clearAuthSession();
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearAuthSession();
  }, []);

  const role = user?.role ?? DEFAULT_ROLE;
  const isAuthenticated = user !== null;
  const value = useMemo(
    () => ({
      role,
      user,
      isAuthenticated,
      loading,
      login,
      logout,
      isPurchaser: role === 'AGENT',
      isCustomer: role === 'CUSTOMER',
      isMerchant: role === 'MERCHANT'
    }),
    [isAuthenticated, loading, login, logout, role, user]
  );

  return <UserRoleContext.Provider value={value}>{children}</UserRoleContext.Provider>;
}

export function useUserRole() {
  const context = useContext(UserRoleContext);
  if (!context) {
    throw new Error('useUserRole must be used within a UserRoleProvider');
  }
  return context;
}

export type { UserRole };
