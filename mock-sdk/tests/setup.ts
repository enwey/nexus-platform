import { vi } from 'vitest'

global.window = vi.fn()

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    value: {
      href: 'http://localhost:5173'
    },
    writable: true
  })
})

afterAll(() => {
  vi.restoreAllMocks()
})
