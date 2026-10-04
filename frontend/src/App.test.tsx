import '@testing-library/jest-dom/vitest'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import App from './App'

describe('App', () => {
  it('renders the initial market monitoring shell', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: /forex market monitoring/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Initial application scaffold',
    )
  })
})
