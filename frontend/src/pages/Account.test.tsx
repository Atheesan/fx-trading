import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { Account } from './Account'
import { authenticate } from '../api/client'
vi.mock('../api/client', () => ({ authenticate: vi.fn(), logout: vi.fn() }))
vi.mock('../auth/authContext', () => ({ useAuth: () => ({ userId: null, loading: false }) }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
it('submits login and clears the password after success', async () => {
  vi.mocked(authenticate).mockResolvedValue(undefined)
  render(<Account />)
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'person@example.com' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'long-password' } })
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
  await waitFor(() => expect(authenticate).toHaveBeenCalledWith('login', 'person@example.com', 'long-password'))
  await waitFor(() => expect(screen.getByLabelText('Password')).toHaveValue(''))
})
it('offers registration and shows generic failures', async () => {
  vi.mocked(authenticate).mockRejectedValue(new Error('internal details'))
  render(<Account />)
  fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'person@example.com' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'long-password' } })
  fireEvent.click(screen.getByRole('button', { name: 'Register' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Authentication failed')
  expect(screen.queryByText('internal details')).not.toBeInTheDocument()
})
