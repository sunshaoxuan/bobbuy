import { render, screen } from '@testing-library/react';
import { UserRoleProvider, useUserRole } from './UserRoleContext';

function Probe() {
  const { role } = useUserRole();
  return <div>{role}</div>;
}

describe('UserRoleContext', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/');
  });

  it('reads injected test role from localStorage', () => {
    localStorage.setItem('bobbuy_test_role', 'AGENT');
    render(
      <UserRoleProvider>
        <Probe />
      </UserRoleProvider>
    );
    expect(screen.getByText('AGENT')).toBeInTheDocument();
  });

  it('query role overrides stored role', () => {
    localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
    window.history.replaceState({}, '', '/?role=AGENT');
    render(
      <UserRoleProvider>
        <Probe />
      </UserRoleProvider>
    );
    expect(screen.getByText('AGENT')).toBeInTheDocument();
  });
});
