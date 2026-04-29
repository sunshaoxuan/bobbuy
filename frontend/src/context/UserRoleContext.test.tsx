import { render, screen } from '@testing-library/react';
import { storeAuthSession } from '../authStorage';
import { UserRoleProvider, useUserRole } from './UserRoleContext';

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  me: vi.fn()
}));

vi.mock('../api', () => ({
  api: {
    auth: {
      login: mocks.login,
      me: mocks.me
    }
  }
}));

function Probe() {
  const { role, isAuthenticated } = useUserRole();
  return <div>{`${role}:${isAuthenticated}`}</div>;
}

describe('UserRoleContext', () => {
  beforeEach(() => {
    localStorage.clear();
    mocks.me.mockReset();
  });

  it('reads injected test role from localStorage', () => {
    localStorage.setItem('bobbuy_test_role', 'AGENT');
    render(
      <UserRoleProvider>
        <Probe />
      </UserRoleProvider>
    );
    expect(screen.getByText('AGENT:true')).toBeInTheDocument();
  });

  it('reads stored authenticated session from localStorage', () => {
    mocks.me.mockResolvedValue({
      id: 1001,
      username: 'customer',
      name: 'Chen Li',
      role: 'CUSTOMER'
    });
    storeAuthSession('token-123', {
      id: 1001,
      username: 'customer',
      name: 'Chen Li',
      role: 'CUSTOMER'
    });
    render(
      <UserRoleProvider>
        <Probe />
      </UserRoleProvider>
    );
    expect(screen.getByText('CUSTOMER:true')).toBeInTheDocument();
  });
});
