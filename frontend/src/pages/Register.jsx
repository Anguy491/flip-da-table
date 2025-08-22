import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { RegisterApi } from '../api/auth';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';

function Register() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');

  // Handle register form submission
  const handleRegister = async (e) => {
    e.preventDefault();

    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }

    try {
      const { token } = await RegisterApi({ email, password, nickname });
      setToken(token); // persist via context
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  // Render register form UI
  return (
    <PageContainer>
      <form onSubmit={handleRegister}>
        <CardContainer>
          <h2 className="text-2xl font-bold text-center">Register</h2>

          <FormInput
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <FormInput
            type="text"
            placeholder="Nickname"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            required
          />

          <FormInput
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <FormInput
            type="password"
            placeholder="Confirm Password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />

          <ErrorPopup message={error} />

          <SubmitButton>Register</SubmitButton>

          <p className="text-center text-sm">
            Already have an account?{' '}
            <span className="text-blue-500 hover:underline cursor-pointer" onClick={() => navigate('/login')}>
              Login
            </span>
          </p>
        </CardContainer>
      </form>
    </PageContainer>
  );
}

export default Register;