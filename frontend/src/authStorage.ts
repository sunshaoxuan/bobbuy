export type UserRole = 'CUSTOMER' | 'AGENT' | 'MERCHANT';

export type AuthenticatedUser = {
  id: number;
  username: string;
  name: string;
  role: UserRole;
};

const ACCESS_TOKEN_KEY = 'bobbuy_access_token';
const AUTH_USER_KEY = 'bobbuy_auth_user';
const TEST_ROLE_KEY = 'bobbuy_test_role';
const TEST_USER_KEY = 'bobbuy_test_user';
const AUTH_CHANGED_EVENT = 'bobbuy-auth-changed';

export const isLocalAuthOverrideEnabled = () =>
  typeof window !== 'undefined' &&
  (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

function dispatchAuthChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
  }
}

function parseStoredUser(value: string | null): AuthenticatedUser | null {
  if (!value) {
    return null;
  }
  try {
    const parsed = JSON.parse(value) as Partial<AuthenticatedUser>;
    if (
      typeof parsed.id === 'number' &&
      typeof parsed.username === 'string' &&
      typeof parsed.name === 'string' &&
      (parsed.role === 'CUSTOMER' || parsed.role === 'AGENT' || parsed.role === 'MERCHANT')
    ) {
      return parsed as AuthenticatedUser;
    }
  } catch {
    // Ignore invalid persisted payloads.
  }
  return null;
}

export function getStoredAccessToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const token = window.localStorage.getItem(ACCESS_TOKEN_KEY);
  return token && token.trim() ? token.trim() : null;
}

export function getStoredUser(): AuthenticatedUser | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return parseStoredUser(window.localStorage.getItem(AUTH_USER_KEY));
}

export function storeAuthSession(accessToken: string, user: AuthenticatedUser) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  dispatchAuthChanged();
}

export function clearAuthSession() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_USER_KEY);
  dispatchAuthChanged();
}

export function getTestInjectedRole(): UserRole | null {
  if (typeof window === 'undefined' || !isLocalAuthOverrideEnabled()) {
    return null;
  }
  const injectedRole = window.localStorage.getItem(TEST_ROLE_KEY);
  return injectedRole === 'CUSTOMER' || injectedRole === 'AGENT' || injectedRole === 'MERCHANT' ? injectedRole : null;
}

export function getTestInjectedUser(): string | null {
  if (typeof window === 'undefined' || !isLocalAuthOverrideEnabled()) {
    return null;
  }
  const injectedUser = window.localStorage.getItem(TEST_USER_KEY);
  return injectedUser && injectedUser.trim() ? injectedUser.trim() : null;
}

export function subscribeToAuthChanges(listener: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }
  window.addEventListener(AUTH_CHANGED_EVENT, listener);
  window.addEventListener('storage', listener);
  return () => {
    window.removeEventListener(AUTH_CHANGED_EVENT, listener);
    window.removeEventListener('storage', listener);
  };
}
