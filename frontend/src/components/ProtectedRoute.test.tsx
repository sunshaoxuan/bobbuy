import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { UserRoleProvider } from '../context/UserRoleContext';

function renderWithRole(initialPath: string, role?: 'CUSTOMER' | 'AGENT') {
  localStorage.clear();
  if (role) {
    localStorage.setItem('bobbuy_test_role', role);
  }
  return render(
    <UserRoleProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/" element={<div>customer-home</div>} />
          <Route path="/dashboard" element={<div>agent-home</div>} />
          <Route path="/login" element={<div>login-page</div>} />
          <Route
            path="/guarded"
            element={
              <ProtectedRoute allowedRoles={['AGENT']}>
                <div>guarded-content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </UserRoleProvider>
  );
}

describe('ProtectedRoute', () => {
  it('renders content when role is allowed', () => {
    renderWithRole('/guarded', 'AGENT');
    expect(screen.getByText('guarded-content')).toBeInTheDocument();
  });

  it('redirects disallowed role to customer home by default', () => {
    renderWithRole('/guarded', 'CUSTOMER');
    expect(screen.getByText('customer-home')).toBeInTheDocument();
  });

  it('redirects unauthenticated access to login', () => {
    renderWithRole('/guarded');
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });
});
