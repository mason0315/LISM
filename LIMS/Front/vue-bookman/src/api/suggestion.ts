import axios from 'axios';

export function fetchSuggestions() {
  return axios.get('/api/suggestions');
}

export function addSuggestion(data: { userName: string; content: string }) {
  return axios.post('/api/suggestions', data);
} 