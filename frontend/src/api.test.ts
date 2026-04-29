import { api } from './api';
import { clearAuthSession, storeAuthSession } from './authStorage';

describe('api authentication headers', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    clearAuthSession();
  });

  it('adds Bearer token when an authenticated session exists', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: async () => ({ data: [] })
    });
    vi.stubGlobal('fetch', fetchMock);
    storeAuthSession('token-123', {
      id: 1000,
      username: 'agent',
      name: 'Aiko Tan',
      role: 'AGENT'
    });

    await api.trips();

    const headers = fetchMock.mock.calls[0][1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer token-123');
    expect(headers.get('X-BOBBUY-ROLE')).toBeNull();
  });
});
