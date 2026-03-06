import request from '@/utils/request'

export function getAllShelves(params: { pageNum: number, pageSize: number }) {
  return request.get('/shelves/getAllShelves', { params })
}

export function getShelvesById(shelveId: number, params: { pageNum: number, pageSize: number }) {
  return request.get(`/shelves/getShelvesById/${shelveId}`, { params })
}

export function getShelvesByTitle(title: string, params: { pageNum: number, pageSize: number }) {
  return request.get(`/shelves/getShelvesByTitle/${encodeURIComponent(title)}`, { params })
}

export function addShelves(data: { shelveId: number, category: string, title: string }) {
  return request.post('/shelves/addShelves', data)
}

export function updateShelves(data: { shelveId: number, category: string, title: string }) {
  return request.put('/shelves/updateShelves', data)
}

export function deleteShelves(shelveId: number) {
  return request.delete('/shelves/deleteShelves', { params: { shelveId } })
}

export function deleteShelvesByTitle(title: string) {
  return request.delete(`/shelves/deleteShelvesByTitle/${encodeURIComponent(title)}`)
}

export function getBooksByShelveId(shelveId: number) {
  return request.get(`/shelves/getBooksByShelveId/${shelveId}`)
}

export function addBookToShelve(shelveId: number, title: string, category: string) {
  return request.post('/shelves/addBookToShelve', null, { params: { shelveId, title, category } })
}

export function removeBookFromShelve(shelveId: number, title: string) {
  return request.delete('/shelves/removeBookFromShelve', { params: { shelveId, title } })
}

export function getAllCategories() {
  return request.get('/shelves/getAllCategories')
} 