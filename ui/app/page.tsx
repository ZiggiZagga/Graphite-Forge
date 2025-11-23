'use client'

import { useMutation, useQuery } from '@apollo/client'
import { useState } from 'react'
import { gql } from '@apollo/client'

const GET_ITEMS = gql`
  query GetItems {
    items {
      id
      name
      description
    }
  }
`

const CREATE_ITEM = gql`
  mutation CreateItem($name: String!, $description: String) {
    createItem(name: $name, description: $description) {
      id
      name
      description
    }
  }
`

const UPDATE_ITEM = gql`
  mutation UpdateItem($id: String!, $name: String, $description: String) {
    updateItem(id: $id, name: $name, description: $description) {
      id
      name
      description
    }
  }
`

const DELETE_ITEM = gql`
  mutation DeleteItem($id: String!) {
    deleteItem(id: $id)
  }
`

interface Item {
  id: string
  name: string
  description?: string
}

export default function Page() {
  const { data, loading, error, refetch } = useQuery(GET_ITEMS)
  const [createItem, { loading: createLoading }] = useMutation(CREATE_ITEM, {
    onCompleted: () => {
      refetch()
      setName('')
      setDescription('')
    }
  })
  const [updateItem, { loading: updateLoading }] = useMutation(UPDATE_ITEM, {
    onCompleted: () => {
      refetch()
      setEditingId(null)
      setName('')
      setDescription('')
    }
  })
  const [deleteItem, { loading: deleteLoading }] = useMutation(DELETE_ITEM, {
    onCompleted: () => refetch()
  })

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)

  const handleCreate = async () => {
    if (!name.trim()) return
    await createItem({ variables: { name, description: description || null } })
  }

  const handleUpdate = async () => {
    if (!editingId || !name.trim()) return
    await updateItem({ variables: { id: editingId, name, description: description || null } })
  }

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this item?')) {
      await deleteItem({ variables: { id } })
    }
  }

  const startEdit = (item: Item) => {
    setEditingId(item.id)
    setName(item.name)
    setDescription(item.description || '')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setName('')
    setDescription('')
  }

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Graphite Forge Items</h1>
        <p className="text-gray-600">Manage your items with GraphQL</p>
      </div>

      {/* Create/Edit Form */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h2 className="text-xl font-semibold mb-4">
          {editingId ? 'Edit Item' : 'Create New Item'}
        </h2>
        <div className="space-y-3">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Item name (required)"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={createLoading || updateLoading}
          />
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Item description (optional)"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={createLoading || updateLoading}
            rows={3}
          />
          <div className="flex gap-2">
            <button
              onClick={editingId ? handleUpdate : handleCreate}
              disabled={!name.trim() || createLoading || updateLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 font-medium"
            >
              {createLoading || updateLoading ? 'Saving...' : editingId ? 'Update' : 'Create'}
            </button>
            {editingId && (
              <button
                onClick={cancelEdit}
                className="px-4 py-2 bg-gray-300 text-gray-800 rounded-md hover:bg-gray-400 font-medium"
              >
                Cancel
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Items List */}
      <div>
        <h2 className="text-2xl font-semibold mb-4">Items</h2>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md mb-4">
            <p className="font-semibold">Error loading items</p>
            <p className="text-sm">{error.message}</p>
          </div>
        )}

        {loading ? (
          <div className="text-center py-8">
            <div className="inline-block animate-spin">⏳</div>
            <p className="mt-2 text-gray-600">Loading items...</p>
          </div>
        ) : data?.items?.length === 0 ? (
          <div className="text-center py-8 bg-gray-50 rounded-md">
            <p className="text-gray-500">No items yet. Create one to get started!</p>
          </div>
        ) : (
          <div className="space-y-3">
            {data?.items?.map((item: Item) => (
              <div
                key={item.id}
                className="p-4 border border-gray-200 rounded-lg hover:shadow-md transition-shadow"
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h3 className="font-bold text-lg text-gray-900">{item.name}</h3>
                    {item.description && (
                      <p className="text-gray-600 mt-1">{item.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-2">ID: {item.id}</p>
                  </div>
                  <div className="flex gap-2 ml-4">
                    <button
                      onClick={() => startEdit(item)}
                      disabled={editingId !== null}
                      className="px-3 py-1 bg-amber-500 text-white rounded text-sm hover:bg-amber-600 disabled:bg-gray-400"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(item.id)}
                      disabled={deleteLoading}
                      className="px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700 disabled:bg-gray-400"
                    >
                      {deleteLoading ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
