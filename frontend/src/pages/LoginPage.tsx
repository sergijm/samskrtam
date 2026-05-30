import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { Card } from 'primereact/card';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Button } from 'primereact/button';
import { Divider } from 'primereact/divider';
import { Message } from 'primereact/message';

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const authData = await authApi.login({ email, password });
      login(authData);
      navigate('/');
    } catch (err) {
      setError('Неверный email или пароль');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex align-items-center justify-content-center min-h-screen bg-gray-100 p-4">
      <div className="w-full max-w-25rem">
        <Card>
          <div className="text-center mb-5">
            <span className="text-4xl">🕉️</span>
            <h1 className="text-3xl font-bold mt-2">Samskrtam</h1>
            <span className="text-gray-500">Войдите, чтобы продолжить</span>
          </div>

          <div className="flex flex-column gap-3 mb-4">
            <Button
              label="Войти через Google"
              icon="pi pi-google"
              className="p-button-outlined"
              onClick={authApi.loginWithGoogle}
            />
            <Button
              label="Войти через Mail.ru"
              icon="pi pi-envelope"
              className="p-button-outlined p-button-secondary"
              onClick={authApi.loginWithMailRu}
            />
          </div>

          <Divider align="center" className="my-4"><b>или</b></Divider>

          <form onSubmit={handleLogin} className="flex flex-column gap-4">
            <div className="p-fluid">
              <InputText
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                type="email"
                required
              />
            </div>
            <div className="p-fluid">
              <Password
                inputId="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Пароль"
                feedback={false}
                toggleMask
                required
              />
            </div>
            {error && <Message severity="error" text={error} />}
            <Button
              type="submit"
              label="Войти"
              loading={loading}
            />
          </form>
        </Card>
      </div>
    </div>
  );
}
