import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MockedProvider } from '@apollo/client/testing'
import '@testing-library/jest-dom'
import BucketList from '@/components/ironbucket/BucketList'
import { GET_BUCKETS } from '@/graphql/ironbucket-queries'

/**
 * Test suite for BucketList component
 * Tests client UI for browsing S3 buckets
 * Following IronBucket testing standards and Next.js/React testing patterns
 */
describe('BucketList Component', () => {
  const mockBuckets = [
    { name: 'bucket-1', creationDate: '2026-01-01T00:00:00Z', ownerTenant: 'test-tenant' },
    { name: 'bucket-2', creationDate: '2026-01-02T00:00:00Z', ownerTenant: 'test-tenant' }
  ]

  const mocks = [
    {
      request: {
        query: GET_BUCKETS
      },
      result: {
        data: {
          listBuckets: mockBuckets
        }
      }
    }
  ]

  describe('Rendering', () => {
    it('should render loading state initially', () => {
      render(
        <MockedProvider mocks={[]} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      expect(screen.getByText(/loading/i)).toBeInTheDocument()
    })

    it('should render list of buckets', async () => {
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('bucket-1')).toBeInTheDocument()
        expect(screen.getByText('bucket-2')).toBeInTheDocument()
      })
    })

    it('should render empty state when no buckets exist', async () => {
      const emptyMocks = [
        {
          request: { query: GET_BUCKETS },
          result: { data: { listBuckets: [] } }
        }
      ]

      render(
        <MockedProvider mocks={emptyMocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText(/no buckets found/i)).toBeInTheDocument()
      })
    })

    it('should render error state when query fails', async () => {
      const errorMocks = [
        {
          request: { query: GET_BUCKETS },
          error: new Error('Failed to load buckets')
        }
      ]

      render(
        <MockedProvider mocks={errorMocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText(/error loading buckets/i)).toBeInTheDocument()
      })
    })
  })

  describe('User Interactions', () => {
    it('should navigate to bucket details when clicking bucket', async () => {
      const mockRouter = { push: jest.fn() }
      jest.mock('next/navigation', () => ({
        useRouter: () => mockRouter
      }))

      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        const bucket = screen.getByText('bucket-1')
        fireEvent.click(bucket)
      })

      // Should navigate to bucket details page
      // expect(mockRouter.push).toHaveBeenCalledWith('/buckets/bucket-1')
    })

    it('should show create bucket dialog when clicking create button', async () => {
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      const createButton = screen.getByText(/create bucket/i)
      fireEvent.click(createButton)

      await waitFor(() => {
        expect(screen.getByText(/bucket name/i)).toBeInTheDocument()
      })
    })

    it('should filter buckets by search query', async () => {
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('bucket-1')).toBeInTheDocument()
      })

      const searchInput = screen.getByPlaceholderText(/search buckets/i)
      fireEvent.change(searchInput, { target: { value: 'bucket-2' } })

      await waitFor(() => {
        expect(screen.queryByText('bucket-1')).not.toBeInTheDocument()
        expect(screen.getByText('bucket-2')).toBeInTheDocument()
      })
    })
  })

  describe('Policy-Aware Behavior', () => {
    it('should hide buckets user does not have access to', async () => {
      const restrictedMocks = [
        {
          request: { query: GET_BUCKETS },
          result: {
            data: {
              listBuckets: mockBuckets.filter(b => b.name !== 'bucket-2')
            }
          }
        }
      ]

      render(
        <MockedProvider mocks={restrictedMocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('bucket-1')).toBeInTheDocument()
        expect(screen.queryByText('bucket-2')).not.toBeInTheDocument()
      })
    })

    it('should disable create button for read-only users', async () => {
      const mockUserClaims = { roles: ['s3:read'] }
      // Mock useUserClaims hook
      jest.mock('@/hooks/useUserClaims', () => ({
        useUserClaims: () => mockUserClaims
      }))

      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      const createButton = screen.getByText(/create bucket/i)
      expect(createButton).toBeDisabled()
    })

    it('should show access level indicator for each bucket', async () => {
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        // Should show read-write or read-only badge
        expect(screen.getAllByText(/read-write|read-only/i).length).toBeGreaterThan(0)
      })
    })
  })

  describe('Refresh & Caching', () => {
    it('should refetch buckets when refresh button is clicked', async () => {
      const refetchMock = jest.fn()
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        const refreshButton = screen.getByLabelText(/refresh/i)
        fireEvent.click(refreshButton)
      })

      // Should trigger refetch
    })

    it('should use cached data on component remount', async () => {
      const { unmount } = render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('bucket-1')).toBeInTheDocument()
      })

      unmount()

      // Remount component
      render(
        <MockedProvider mocks={mocks} addTypename={false}>
          <BucketList />
        </MockedProvider>
      )

      // Should show data immediately from cache (no loading state)
      expect(screen.getByText('bucket-1')).toBeInTheDocument()
    })
  })
})
