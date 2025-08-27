import { 
  LoginRequest, 
  RegisterRequest, 
  AuthResponse, 
  Book, 
  Question, 
  ApiResponse 
} from '../types';

const API_BASE_URL = 'http://localhost:8080'; // Backend URL'ini buraya yazın

class ApiService {
  private async request<T>(
    endpoint: string, 
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // Auth endpoints
  async login(data: LoginRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>('/users/login', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async register(data: RegisterRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>('/users/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Book endpoints
  async getBooksForUser(userId: number): Promise<Book[]> {
    return this.request<Book[]>(`/api/books/for-user/${userId}`);
  }

  async useBookCode(userId: number, code: string): Promise<string> {
    return this.request<string>(`/api/books/use-code?userId=${userId}&code=${code}`);
  }

  // Question endpoints (placeholder - backend'de henüz yok)
  async getQuestionsForBook(bookId: number): Promise<Question[]> {
    return this.request<Question[]>(`/api/books/${bookId}/questions`);
  }

  // Test endpoints (placeholder - backend'de henüz yok)
  async getTestsForBook(bookId: number): Promise<any[]> {
    return this.request<any[]>(`/api/books/${bookId}/tests`);
  }
}

export const apiService = new ApiService();
export default apiService;
