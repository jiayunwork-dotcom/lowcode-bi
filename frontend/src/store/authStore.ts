import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { jwtDecode } from 'jwt-decode'
import type { User } from '@/types'

interface AuthState {
  token: string | null
  user: User | null
  tenantId: string | null
  role: string | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
  refreshToken: (newToken: string) => void
  setUser: (user: User) => void
}

interface JwtClaims {
  sub: string
  tenantId: string
  role: string
  userId: string
  username: string
  email: string
  exp: number
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      tenantId: null,
      role: null,
      isAuthenticated: false,

      login: (token: string) => {
        try {
          const decoded = jwtDecode<JwtClaims>(token)
          set({
            token,
            tenantId: decoded.tenantId,
            role: decoded.role,
            isAuthenticated: true,
            user: {
              id: decoded.userId,
              username: decoded.username,
              email: decoded.email,
              role: decoded.role as any,
              tenantId: decoded.tenantId,
              status: 'ACTIVE',
              createdAt: '',
              updatedAt: ''
            }
          })
        } catch (error) {
          console.error('Failed to decode token:', error)
          set({
            token: null,
            user: null,
            tenantId: null,
            role: null,
            isAuthenticated: false
          })
        }
      },

      logout: () => {
        set({
          token: null,
          user: null,
          tenantId: null,
          role: null,
          isAuthenticated: false
        })
      },

      refreshToken: (newToken: string) => {
        try {
          const decoded = jwtDecode<JwtClaims>(newToken)
          set({
            token: newToken,
            tenantId: decoded.tenantId,
            role: decoded.role,
            isAuthenticated: true,
            user: {
              id: decoded.userId,
              username: decoded.username,
              email: decoded.email,
              role: decoded.role as any,
              tenantId: decoded.tenantId,
              status: 'ACTIVE',
              createdAt: '',
              updatedAt: ''
            }
          })
        } catch (error) {
          console.error('Failed to decode new token:', error)
        }
      },

      setUser: (user: User) => {
        set({ user })
      }
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ token: state.token })
    }
  )
)
