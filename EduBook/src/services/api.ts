import * as SecureStore from 'expo-secure-store';
import { 
  LoginRequest, 
  RegisterRequest, 
  AuthResponse, 
  Book, 
  Question, 
  ApiResponse,
  User
} from '../types';

const API_BASE_URL = 'http://localhost:8080/api'; // Backend URL'ini buraya yazın

class ApiService {
  private async getAuthHeaders(): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    
    // Token'ı SecureStore'dan al
    try {
      const token = await SecureStore.getItemAsync('auth-storage');
      if (token) {
        const authData = JSON.parse(token);
        if (authData.state?.token) {
          headers['Authorization'] = `Bearer ${authData.state.token}`;
        }
      }
    } catch (error) {
      console.error('Error getting auth token:', error);
    }
    
    return headers;
  }

  private async request<T>(
    endpoint: string, 
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const authHeaders = await this.getAuthHeaders();
    
    const config: RequestInit = {
      headers: {
        ...authHeaders,
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

  async register(data: RegisterRequest): Promise<User> {
    const response = await this.request<any>('/users/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    
    // Backend'den gelen User entity'sini frontend'e uygun hale getir
    return {
      id: response.id,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      phoneNumber: response.phoneNumber,
      username: response.username,
      password: '', // Backend'den password dönmez
      role: response.role,
      ownedBooks: Array.from(response.ownedBooks || []), // Set'i Array'e çevir
      flaggedQuestions: Array.from(response.flaggedQuestions || []), // Set'i Array'e çevir
    };
  }

  // Book endpoints
  async getBooksForUser(userId: number): Promise<Book[]> {
    return this.request<Book[]>(`/books/for-user/${userId}`);
  }

  async useBookCode(userId: number, code: string): Promise<string> {
    return this.request<string>(`/books/use-code?userId=${userId}&code=${code}`);
  }

  // Question endpoints - Test'ten soruları getir
  async getQuestionsForTest(testId: number): Promise<Question[]> {
    return this.request<Question[]>(`/questions/by-test/${testId}`);
  }

  async getQuestionById(questionId: number): Promise<Question> {
    return this.request<Question>(`/questions/${questionId}`);
  }

  // Test endpoints - Topic'ten testleri getir
  async getTestsForTopic(topicId: number): Promise<any[]> {
    return this.request<any[]>(`/tests/by-topic/${topicId}`);
  }

  async getTestById(testId: number): Promise<any> {
    return this.request<any>(`/tests/${testId}`);
  }

  // Topic endpoints - Kitaptan topic'leri getir
  async getTopicsForBook(bookId: number): Promise<any[]> {
    return this.request<any[]>(`/topics/by-book/${bookId}`);
  }
}

export const apiService = new ApiService();
export default apiService;
