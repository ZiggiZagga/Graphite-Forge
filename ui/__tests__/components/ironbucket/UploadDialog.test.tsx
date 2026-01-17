import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MockedProvider } from '@apollo/client/testing'
import '@testing-library/jest-dom'
import UploadDialog from '@/components/ironbucket/UploadDialog'
import { UPLOAD_OBJECT } from '@/graphql/ironbucket-mutations'

/**
 * Test suite for UploadDialog component
 * Tests file upload functionality with drag-and-drop support
 */
describe('UploadDialog Component', () => {
  const mockBucket = 'test-bucket'
  const mockOnClose = jest.fn()
  const mockOnSuccess = jest.fn()

  const uploadMocks = [
    {
      request: {
        query: UPLOAD_OBJECT,
        variables: {
          bucket: mockBucket,
          key: 'test-file.txt',
          content: expect.any(Object),
          contentType: 'text/plain'
        }
      },
      result: {
        data: {
          uploadObject: {
            key: 'test-file.txt',
            bucket: mockBucket,
            size: 1024
          }
        }
      }
    }
  ]

  describe('Rendering', () => {
    it('should render upload dialog', () => {
      render(
        <MockedProvider mocks={[]} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      expect(screen.getByText(/upload file/i)).toBeInTheDocument()
      expect(screen.getByText(/drag.*drop/i)).toBeInTheDocument()
    })

    it('should show bucket name in dialog', () => {
      render(
        <MockedProvider mocks={[]} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      expect(screen.getByText(new RegExp(mockBucket, 'i'))).toBeInTheDocument()
    })
  })

  describe('File Selection', () => {
    it('should allow file selection via file picker', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)

      fireEvent.change(input, { target: { files: [file] } })

      await waitFor(() => {
        expect(screen.getByText('test-file.txt')).toBeInTheDocument()
      })
    })

    it('should support drag and drop', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const dropzone = screen.getByText(/drag.*drop/i).closest('div')
      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })

      fireEvent.drop(dropzone!, {
        dataTransfer: { files: [file] }
      })

      await waitFor(() => {
        expect(screen.getByText('test-file.txt')).toBeInTheDocument()
      })
    })

    it('should support multiple file uploads', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file1 = new File(['content1'], 'file1.txt', { type: 'text/plain' })
      const file2 = new File(['content2'], 'file2.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)

      fireEvent.change(input, { target: { files: [file1, file2] } })

      await waitFor(() => {
        expect(screen.getByText('file1.txt')).toBeInTheDocument()
        expect(screen.getByText('file2.txt')).toBeInTheDocument()
      })
    })
  })

  describe('File Validation', () => {
    it('should validate file size limit', async () => {
      render(
        <MockedProvider mocks={[]} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} maxFileSize={1024} />
        </MockedProvider>
      )

      const largeFile = new File(['x'.repeat(2000)], 'large-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)

      fireEvent.change(input, { target: { files: [largeFile] } })

      await waitFor(() => {
        expect(screen.getByText(/file size exceeds limit/i)).toBeInTheDocument()
      })
    })

    it('should validate file type restrictions', async () => {
      render(
        <MockedProvider mocks={[]} addTypename={false}>
          <UploadDialog 
            bucket={mockBucket} 
            onClose={mockOnClose} 
            onSuccess={mockOnSuccess}
            allowedTypes={['text/plain', 'application/pdf']}
          />
        </MockedProvider>
      )

      const invalidFile = new File(['content'], 'script.js', { type: 'application/javascript' })
      const input = screen.getByLabelText(/choose file/i)

      fireEvent.change(input, { target: { files: [invalidFile] } })

      await waitFor(() => {
        expect(screen.getByText(/file type not allowed/i)).toBeInTheDocument()
      })
    })
  })

  describe('Upload Progress', () => {
    it('should show upload progress bar', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)
      fireEvent.change(input, { target: { files: [file] } })

      const uploadButton = screen.getByText(/upload/i)
      fireEvent.click(uploadButton)

      await waitFor(() => {
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
      })
    })

    it('should show upload percentage', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      // Trigger upload and verify percentage is displayed
    })

    it('should allow canceling upload', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)
      fireEvent.change(input, { target: { files: [file] } })

      const uploadButton = screen.getByText(/upload/i)
      fireEvent.click(uploadButton)

      const cancelButton = await screen.findByText(/cancel/i)
      fireEvent.click(cancelButton)

      // Should stop upload and show cancellation message
    })
  })

  describe('Error Handling', () => {
    it('should show error message on upload failure', async () => {
      const errorMocks = [
        {
          request: {
            query: UPLOAD_OBJECT,
            variables: expect.any(Object)
          },
          error: new Error('Upload failed')
        }
      ]

      render(
        <MockedProvider mocks={errorMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)
      fireEvent.change(input, { target: { files: [file] } })

      const uploadButton = screen.getByText(/upload/i)
      fireEvent.click(uploadButton)

      await waitFor(() => {
        expect(screen.getByText(/upload failed/i)).toBeInTheDocument()
      })
    })

    it('should allow retry after failure', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      // Trigger failed upload
      // Click retry button
      // Verify retry is attempted
    })
  })

  describe('Success Handling', () => {
    it('should call onSuccess callback after successful upload', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)
      fireEvent.change(input, { target: { files: [file] } })

      const uploadButton = screen.getByText(/upload/i)
      fireEvent.click(uploadButton)

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalledWith({
          key: 'test-file.txt',
          bucket: mockBucket,
          size: 1024
        })
      })
    })

    it('should close dialog after successful upload', async () => {
      render(
        <MockedProvider mocks={uploadMocks} addTypename={false}>
          <UploadDialog bucket={mockBucket} onClose={mockOnClose} onSuccess={mockOnSuccess} autoClose />
        </MockedProvider>
      )

      const file = new File(['test content'], 'test-file.txt', { type: 'text/plain' })
      const input = screen.getByLabelText(/choose file/i)
      fireEvent.change(input, { target: { files: [file] } })

      const uploadButton = screen.getByText(/upload/i)
      fireEvent.click(uploadButton)

      await waitFor(() => {
        expect(mockOnClose).toHaveBeenCalled()
      })
    })
  })
})
