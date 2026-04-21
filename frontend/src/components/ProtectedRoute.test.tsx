import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { UserRoleProvider } from '../context/UserRoleContext';

function renderWithRole(initialPath: string, role: 'CUSTOMER' | 'AGENT') {
  localStorage.setItem('bobbuy_test_role', role);
  return render(
    <UserRoleProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/" element={<div>customer-home</div>} />
          <Route path="/dashboard" element={<div>agent-home</div>} />
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
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders content when role is allowed', () => {
    renderWithRole('/guarded', 'AGENT');
    expect(screen.getByText('guarded-content')).toBeInTheDocument();
  });

  it('redirects disallowed role to customer home by default', () => {
    renderWithRole('/guarded', 'CUSTOMER');
    expect(screen.getByText('customer-home')).toBeInTheDocument();
  });
});
