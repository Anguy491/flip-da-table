import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginApi } from '../api/auth';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';

function Login() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  // Handle login form submission
  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const token = await loginApi(email, password);
      setToken(token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  // Render login form UI
  return (
    <PageContainer>
      <form onSubmit={handleLogin}>
        <CardContainer>
          <h2 className="text-2xl font-bold text-center">Admin Login</h2>

          <FormInput
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <FormInput
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <ErrorPopup message={error} />

          <SubmitButton>Login</SubmitButton>

          <p className="text-center text-sm">
            Don&apos;t have an account?{' '}
            <span className="text-blue-500 hover:underline cursor-pointer" onClick={() => navigate('/register')}>
              Register
            </span>
          </p>
        </CardContainer>
      </form>
    </PageContainer>
  );
}

export default Login;