import * as SecureStore from 'expo-secure-store';
import { 
  LoginRequest, 
  RegisterRequest, 
  AuthResponse, 
  Book, 
  Topic,
  Test,
  Question, 
  ApiResponse,
  User
} from '../types';

const API_BASE_URL = 'http://192.168.182.220:8080/api'; // Backend URL - local development
const BASE_URL = 'http://192.168.182.220:8080'; // Base URL for file access

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
        // Backend'den gelen hata mesajını al
        let errorMessage = 'Bir hata oluştu';
        
        try {
          const errorText = await response.text();
          // Eğer JSON ise parse et, değilse direkt metni kullan
          try {
            const errorData = JSON.parse(errorText);
            errorMessage = errorData.message || errorText;
          } catch {
            // JSON değilse, direkt text mesajını kullan
            errorMessage = errorText;
          }
        } catch {
          // Response okunamazsa status'a göre mesaj ver
          if (response.status === 400) {
            errorMessage = 'Kullanıcı adı veya şifre hatalı';
          } else if (response.status === 401) {
            errorMessage = 'Yetkisiz erişim';
          } else if (response.status === 500) {
            errorMessage = 'Sunucu hatası';
          }
        }
        
        throw new Error(errorMessage);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      
      // Network hatası için özel mesaj
      if (error instanceof TypeError && error.message.includes('Network request failed')) {
        throw new Error('İnternet bağlantısını kontrol edin');
      }
      
      throw error;
    }
  }

  // Auth endpoints
  async login(data: LoginRequest): Promise<AuthResponse> {
    try {
      return await this.request<AuthResponse>('/users/login', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error) {
      // Login için özel hata mesajları
      if (error instanceof Error) {
        const message = error.message.toLowerCase();
        
        if (message.includes('kullanıcı bulunamadı') || message.includes('user not found')) {
          throw new Error('Kullanıcı adı, email veya telefon numarası hatalı');
        } else if (message.includes('geçersiz şifre') || message.includes('invalid password')) {
          throw new Error('Şifre hatalı');
        } else if (message.includes('kullanıcı adı veya şifre hatalı')) {
          throw new Error('Kullanıcı adı veya şifre hatalı');
        }
      }
      
      throw error;
    }
  }

  async register(data: RegisterRequest): Promise<User> {
    try {
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
    } catch (error) {
      // Register için özel hata mesajları
      if (error instanceof Error) {
        const message = error.message.toLowerCase();
        
        if (message.includes('bu email zaten kayıtlı') || message.includes('email already exists')) {
          throw new Error('Bu email adresi zaten kullanılıyor');
        } else if (message.includes('bu telefon numarası zaten kayıtlı') || message.includes('phone already exists')) {
          throw new Error('Bu telefon numarası zaten kullanılıyor');
        } else if (message.includes('bu kullanıcı adı zaten kayıtlı') || message.includes('username already exists')) {
          throw new Error('Bu kullanıcı adı zaten kullanılıyor');
        }
      }
      
      throw error;
    }
  }

  // Book endpoints
  async getBooksForUser(userId: number): Promise<Book[]> {
    const response = await this.request<any[]>(`/books/for-user/${userId}`);
    // Backend BookResponse'u frontend Book interface'ine çevir
    return response.map(book => ({
      id: book.id,
      title: book.title,
      publisher: book.publisher,
      coverImageUrl: book.coverImageUrl,
      paid: true, // Frontend varsayılanı
    }));
  }

  async useBookCode(userId: number, code: string): Promise<string> {
    return this.request<string>(`/books/use-code?userId=${userId}&code=${code}`);
  }

  // Question endpoints - Test'ten soruları getir (limit ile)
  async getQuestionsForTest(testId: number, limit?: number): Promise<Question[]> {
    try {
      console.log('🔍 Fetching questions for testId:', testId, 'limit:', limit);
      const response = await this.request<any[]>(`/questions/by-test/${testId}`);
      console.log('✅ Raw questions response:', response);
      console.log('📊 Questions count:', response?.length || 0);
      
      // Limit uygula (frontend'de)
      const limitedResponse = limit ? response.slice(0, limit) : response;
      
      // Backend QuestionResponse'u frontend Question interface'ine çevir
      const mappedQuestions = limitedResponse.map((question, index) => ({
        id: Number(question.id),
        imageUrl: question.imageUrl ? `${BASE_URL}/${question.imageUrl}` : '',
        answerNumber: Number(question.answerNumber) || 5,
        correctAnswer: question.correctAnswer || 'A',
        hints: Array.isArray(question.hints) ? question.hints : [],
        explanation: question.explanation || '',
        videoUrl: question.videoUrl || '',
      }));
      
      console.log('🎯 Mapped questions:', mappedQuestions);
      console.log('🖼️ First question imageUrl:', mappedQuestions[0]?.imageUrl);
      console.log('🚀 Returning questions count:', mappedQuestions.length, '(limited from', response.length, ')');
      return mappedQuestions;
    } catch (error) {
      console.error('❌ Error fetching questions for test:', testId, error);
      throw error;
    }
  }

  async getQuestionById(questionId: number): Promise<Question> {
    const response = await this.request<any>(`/questions/${questionId}`);
    // Backend QuestionResponse'u frontend Question interface'ine çevir
    return {
      id: Number(response.id),
      imageUrl: response.imageUrl ? `${BASE_URL}/${response.imageUrl}` : '',
      answerNumber: Number(response.answerNumber) || 5,
      correctAnswer: response.correctAnswer || 'A',
      hints: Array.isArray(response.hints) ? response.hints : [],
      explanation: response.explanation || '',
      videoUrl: response.videoUrl || '',
    };
  }

  // Test endpoints - Topic'ten testleri getir
  async getTestsForTopic(topicId: number): Promise<Test[]> {
    const response = await this.request<any[]>(`/tests/by-topic/${topicId}`);
    // Backend TestResponse'u frontend Test interface'ine çevir
    return response.map(test => ({
      id: test.id,
      title: test.title,
    }));
  }

  async getTestById(testId: number): Promise<Test> {
    const response = await this.request<any>(`/tests/${testId}`);
    // Backend TestResponse'u frontend Test interface'ine çevir
    return {
      id: response.id,
      title: response.title,
    };
  }

  // Topic endpoints - Kitaptan topic'leri getir
  async getTopicsForBook(bookId: number): Promise<Topic[]> {
    const response = await this.request<any[]>(`/topics/by-book/${bookId}`);
    // Backend TopicResponse'u frontend Topic interface'ine çevir
    return response.map(topic => ({
      id: topic.id,
      title: topic.title,
    }));
  }

  // Flagged Questions endpoint - İşaretlenen soruları getir
  async getFlaggedQuestions(): Promise<Question[]> {
    // userId'yi auth store'dan al
    const token = await SecureStore.getItemAsync('auth-storage');
    let userId = 1; // Default fallback
    
    if (token) {
      try {
        const authData = JSON.parse(token);
        if (authData.state?.user?.id) {
          userId = authData.state.user.id;
        }
      } catch (error) {
        console.error('Error parsing auth data:', error);
      }
    }
    
    const response = await this.request<any[]>(`/users/${userId}/flagged`);
    console.log('🏁 Raw flagged questions response:', response);
    console.log('🏁 Flagged questions count:', response?.length || 0);
    
    return response.map(question => ({
      id: question.id,
      imageUrl: question.imageUrl ? `${BASE_URL}/${question.imageUrl}` : '',
      answerNumber: question.answerNumber || 5,
      correctAnswer: question.correctAnswer || 'A',
      hints: question.hints || [],
      explanation: question.explanation || '',
      videoUrl: question.videoUrl || '',
    }));
  }

  // Redeem Code endpoint - Kitap kodunu kullan
  async redeemCode(code: string): Promise<any> {
    // Backend'deki doğru endpoint: /books/use-code
    // userId'yi auth store'dan al
    const token = await SecureStore.getItemAsync('auth-storage');
    let userId = 1; // Default fallback
    
    if (token) {
      try {
        const authData = JSON.parse(token);
        if (authData.state?.user?.id) {
          userId = authData.state.user.id;
        }
      } catch (error) {
        console.error('Error parsing auth data:', error);
      }
    }
    
    const response = await this.request<any>(`/books/use-code?userId=${userId}&code=${code}`, {
      method: 'POST',
    });
    
    // Backend'den gelen response'u kontrol et
    if (response.message) {
      return response; // Başarılı mesaj
    } else if (response.error) {
      throw new Error(response.error); // Hata mesajı
    }
    
    return response;
  }

  // Kullanıcı profile bilgileri
  async getUserProfile(userId: number): Promise<User> {
    return await this.request<User>(`/users/${userId}/profile`);
  }

  // Flagleme fonksiyonları
  async flagQuestion(userId: number, questionId: number): Promise<any> {
    return await this.request<any>(`/users/${userId}/flag/${questionId}`, {
      method: 'POST',
    });
  }

  async unflagQuestion(userId: number, questionId: number): Promise<any> {
    return await this.request<any>(`/users/${userId}/flag/${questionId}`, {
      method: 'DELETE',
    });
  }
}

export const apiService = new ApiService();
export default apiService;
