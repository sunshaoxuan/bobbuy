import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './styles.css';
import { I18nProvider } from './i18n';
import { UserRoleProvider } from './context/UserRoleContext';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <I18nProvider>
      <UserRoleProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </UserRoleProvider>
    </I18nProvider>
  </React.StrictMode>
);
