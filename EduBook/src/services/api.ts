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
  User,
  FlaggedQuestion,
  TestSubmissionRequest,
  DailyQuestion,
  DailyQuestionAnswerRequest,
  GetCurrentQuestionResponse,
  SubmitAnswerResponse,
  BookStoreResponse,
  Cart,
  CartItem,
  Purchase
} from '../types';
const API_BASE_URL = 'http://10.235.179.220:8080/api'; // Backend URL - local development
const BASE_URL = 'http://10.235.179.220:8080'; // Base URL for file access

class ApiService {
  private async getAuthHeaders(): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    
    // Token'ı SecureStore'dan al
    try {
      const token = await SecureStore.getItemAsync('auth-storage');
      console.log('🔑 Auth storage token:', token ? 'exists' : 'missing');
      
      if (token) {
        const authData = JSON.parse(token);
        console.log('🔑 Auth data:', { hasState: !!authData.state, hasToken: !!authData.state?.token });
        
        if (authData.state?.token) {
          headers['Authorization'] = `Bearer ${authData.state.token}`;
          console.log('✅ Authorization header added');
        } else {
          console.warn('⚠️ Token not found in auth data');
        }
      } else {
        console.warn('⚠️ No auth storage found');
      }
    } catch (error) {
      console.error('❌ Error getting auth token:', error);
    }
    
    return headers;
  }

  private async request<T>(
    endpoint: string, 
    options: RequestInit = {},
    retryCount = 0
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
        let shouldRetry = false;
        
        try {
          const errorText = await response.text();
          try {
            const errorData = JSON.parse(errorText);
            errorMessage = errorData.message || errorText;
          } catch {
            errorMessage = errorText;
          }
        } catch {
          switch (response.status) {
            case 400:
              errorMessage = 'Geçersiz istek';
              break;
            case 401:
              errorMessage = 'Yetkisiz erişim';
              break;
            case 403:
              errorMessage = 'Erişim reddedildi';
              break;
            case 404:
              errorMessage = 'Kaynak bulunamadı';
              break;
            case 408:
              errorMessage = 'İstek zaman aşımına uğradı';
              shouldRetry = true;
              break;
            case 429:
              errorMessage = 'Çok fazla istek gönderildi';
              shouldRetry = true;
              break;
            case 500:
              errorMessage = 'Sunucu hatası';
              shouldRetry = true;
              break;
            case 502:
            case 503:
            case 504:
              errorMessage = 'Sunucu geçici olarak kullanılamıyor';
              shouldRetry = true;
              break;
          }
        }

        // Retry logic
        if (shouldRetry && retryCount < 3) {
          console.log(`🔄 Retrying request (${retryCount + 1}/3)...`);
          await new Promise(resolve => setTimeout(resolve, 1000 * (retryCount + 1)));
          return this.request<T>(endpoint, options, retryCount + 1);
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
    console.log('📚 Fetching books for userId:', userId);
    try {
      const response = await this.request<any[]>(`/books/for-user/${userId}`);
      console.log('✅ Raw books response:', response);
      console.log('📊 Books count:', response?.length || 0);
      
      // Backend BookResponse'u frontend Book interface'ine çevir
      const books = response.map(book => ({
        id: book.id,
        title: book.title,
        publisher: book.publisher,
        coverImageUrl: book.coverImageUrl ? `${BASE_URL}/${book.coverImageUrl}` : '',
        paid: book.paid,
        price: book.price,
        description: book.description,
        numberOfTopics: book.numberOfTopics,
        numberOfTests: book.numberOfTests,
        numberOfQuestions: book.numberOfQuestions
      }));
      
      console.log('🔄 Mapped books:', books);
      return books;
    } catch (error) {
      console.error('❌ Error fetching books for user:', error);
      throw error;
    }
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
      numberOfQuestions: test.numberOfQuestions || 0,
    }));
  }

  async getTestById(testId: number): Promise<Test> {
    const response = await this.request<any>(`/tests/${testId}`);
    // Backend TestResponse'u frontend Test interface'ine çevir
    return {
      id: response.id,
      title: response.title,
      numberOfQuestions: response.numberOfQuestions || 0,
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

  // Flagged Questions endpoint - İşaretlenen soruları getir (FlaggedQuestion format'ında)
  async getFlaggedQuestions(): Promise<FlaggedQuestion[]> {
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
    
    return response.map(item => ({
      questionId: item.questionId,
      imageUrl: item.imageUrl ? `${BASE_URL}/${item.imageUrl}` : '',
      answerNumber: item.answerNumber || 5,
      correctAnswer: item.correctAnswer || 'A',
      hints: item.hints || [],
      explanation: item.explanation || '',
      videoUrl: item.videoUrl || '',
      bookId: item.bookId || 0,
      topicId: item.topicId || 0,
      testId: item.testId || 0,
      bookTitle: item.bookTitle || '',
      topicTitle: item.topicTitle || '',
      testTitle: item.testTitle || '',
    }));
  }

  // Tek bir flagged question için detaylı bilgi alma - navigation için
  async getFlaggedQuestionById(questionId: number): Promise<FlaggedQuestion> {
    console.log('🎯 Fetching flagged question by ID:', questionId);
    try {
      const response = await this.request<any>(`/users/flagged/${questionId}`);
      console.log('✅ Raw flagged question response:', response);
      
      return {
        questionId: response.questionId,
        imageUrl: response.imageUrl ? `${BASE_URL}/${response.imageUrl}` : '',
        answerNumber: response.answerNumber || 5,
        correctAnswer: response.correctAnswer || 'A',
        hints: response.hints || [],
        explanation: response.explanation || '',
        videoUrl: response.videoUrl || '',
        bookId: response.bookId,
        topicId: response.topicId,
        testId: response.testId,
        bookTitle: response.bookTitle || '',
        topicTitle: response.topicTitle || '',
        testTitle: response.testTitle || '',
      };
    } catch (error) {
      console.error('❌ Error fetching flagged question by ID:', error);
      throw error;
    }
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
      // Mesajın içeriğine göre başarı/hata durumunu ayırt et
      const message = response.message.toLowerCase();
      
      // Sadece gerçek hata mesajlarını kontrol et
      if (message.includes('geçersiz') || 
          message.includes('bulunamadı') ||
          message.includes('sahipsiniz') ||
          message.includes('hata') ||
          message.includes('invalid') ||
          message.includes('not found') ||
          message.includes('error')) {
        throw new Error(response.message); // Hata mesajı
      }
      
      // Başarı mesajları için hata fırlatma
      if (message.includes('başarıyla') || 
          message.includes('successfully') ||
          (message.includes('kullanıldı') && message.includes('başarıyla'))) {
        return response; // Başarı mesajı
      }
      
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

  // Test Results endpoints
  async submitTest(testSubmission: TestSubmissionRequest): Promise<any> {
    return await this.request<any>('/test-results/submit', {
      method: 'POST',
      body: JSON.stringify(testSubmission),
    });
  }

  async getUserTestResults(userId: number): Promise<any[]> {
    return await this.request<any[]>(`/test-results/user/${userId}`);
  }

  async getTestResult(testResultId: number): Promise<any> {
    return await this.request<any>(`/test-results/${testResultId}`);
  }

  async getUserStats(userId: number): Promise<any> {
    return await this.request<any>(`/test-results/user/${userId}/stats`);
  }

  async getUserTestResult(userId: number, testId: number): Promise<any> {
    return await this.request<any>(`/test-results/user/${userId}/test/${testId}`);
  }

  // Daily Question endpoints
  async getCurrentDailyQuestion(userId: number): Promise<DailyQuestion> {
    try {
      console.log('📅 Fetching current daily question for userId:', userId);
      const response = await this.request<DailyQuestion>(`/daily-questions/current?userId=${userId}`);
      console.log('✅ Daily question response:', response);
      // Resim URL'sini düzelt
      if (response && response.imageUrl) {
        console.log('🔧 Original imageUrl:', response.imageUrl);
        response.imageUrl = `${BASE_URL}${response.imageUrl}`;
        console.log('🔧 Fixed imageUrl:', response.imageUrl);
      }
      // Distribution verisini logla
      if (response && response.afterEndTime && response.afterEndTime.statistics) {
        console.log('📊 Backend distribution:', response.afterEndTime.statistics.distribution);
      }
      // Eğer response null ise veya id yoksa boş bir DailyQuestion objesi dön
      if (!response || !response.id) {
        return {
          id: 0,
          imageUrl: '',
          answerNumber: 0,
          videoUrl: '',
          displayDate: '',
          startTime: '',
          endTime: '',
          hasUserAnswered: false,
          userAnswer: undefined,
          afterEndTime: undefined,
          errorMessage: response?.errorMessage || 'Aktif günlük soru bulunamadı',
          active: false
        };
      }
      return response;
    } catch (error) {
      console.error('❌ Error fetching daily question:', error);
      throw error;
    }
  }

  async submitDailyQuestionAnswer(userId: number, answer: DailyQuestionAnswerRequest): Promise<DailyQuestion> {
    try {
      console.log('📝 Submitting answer for daily question:', answer);
      const response = await this.request<DailyQuestion>(`/daily-questions/answer?userId=${userId}`, {
        method: 'POST',
        body: JSON.stringify(answer),
      });
      console.log('✅ Answer submission response:', response);
      console.log('📊 Answer distribution:', response?.afterEndTime?.statistics?.distribution);
      // Resim URL'sini düzelt
      if (response && response.imageUrl) {
        response.imageUrl = `${BASE_URL}${response.imageUrl}`;
      }
      return response;
    } catch (error) {
      console.error('❌ Error submitting answer:', error);
      throw error;
    }
  }

  // Cart endpoints
  async getCart(): Promise<Cart> {
    console.log('🛒 Fetching cart');
    try {
      const response = await this.request<any>('/cart');
      console.log('✅ Cart response:', response);
      return {
        id: response.id,
        items: response.items.map((item: any) => ({
          id: item.id,
          bookId: item.bookId,
          bookTitle: item.bookTitle,
          bookPublisher: item.bookPublisher,
          bookCoverImageUrl: item.bookCoverImageUrl ? `${BASE_URL}/${item.bookCoverImageUrl}` : '',
          priceAtAdd: item.priceAtAdd,
          currentPrice: item.currentPrice,
          addedAt: item.addedAt,
          isPriceChanged: item.priceChanged || false
        })),
        totalAmount: response.totalPrice || 0,
        itemCount: response.itemCount || response.items.length
      };
    } catch (error) {
      console.error('❌ Error fetching cart:', error);
      throw error;
    }
  }

  async addToCart(bookId: number): Promise<{ success: boolean; cart?: Cart; warning?: string; error?: string }> {
    console.log('🛍️ Adding book to cart:', bookId);
    try {
      const response = await this.request<any>('/cart/add', {
        method: 'POST',
        body: JSON.stringify({ bookId })
      });
      console.log('✅ Add to cart response:', response);
      
      // Backend'den gelen infoMessage'ı kontrol et
      if (response.infoMessage) {
        return {
          success: false,
          warning: response.infoMessage
        };
      }
      
      return {
        success: true,
        cart: {
          id: response.id,
          items: response.items.map((item: any) => ({
            id: item.id,
            bookId: item.bookId,
            bookTitle: item.bookTitle,
            bookPublisher: item.bookPublisher,
            bookCoverImageUrl: item.bookCoverImageUrl ? `${BASE_URL}/${item.bookCoverImageUrl}` : '',
            priceAtAdd: item.priceAtAdd,
            currentPrice: item.currentPrice,
            addedAt: item.addedAt,
            isPriceChanged: item.isPriceChanged || false
          })),
          totalAmount: response.totalPrice || 0,
          itemCount: response.itemCount || response.items.length
        }
      };
    } catch (error: any) {
      console.error('❌ Error adding to cart:', error);
      return {
        success: false,
        error: error.message || 'Kitap sepete eklenemedi'
      };
    }
  }

  async removeFromCart(cartItemId: number): Promise<Cart> {
    console.log('🗑️ Removing item from cart:', cartItemId);
    try {
      const response = await this.request<any>(`/cart/items/${cartItemId}`, {
        method: 'DELETE'
      });
      console.log('✅ Remove from cart response:', response);
      return {
        id: response.id,
        items: response.items.map((item: any) => ({
          id: item.id,
          bookId: item.bookId,
          bookTitle: item.bookTitle,
          bookPublisher: item.bookPublisher,
          bookCoverImageUrl: item.bookCoverImageUrl ? `${BASE_URL}/${item.bookCoverImageUrl}` : '',
          priceAtAdd: item.priceAtAdd,
          currentPrice: item.currentPrice,
          addedAt: item.addedAt,
          isPriceChanged: item.priceChanged || false
        })),
        totalAmount: response.totalPrice || 0,
        itemCount: response.itemCount || response.items.length
      };
    } catch (error) {
      console.error('❌ Error removing from cart:', error);
      throw error;
    }
  }

  async clearCart(): Promise<void> {
    console.log('🧹 Clearing cart');
    try {
      await this.request('/cart/clear', {
        method: 'DELETE'
      });
      console.log('✅ Cart cleared');
    } catch (error) {
      console.error('❌ Error clearing cart:', error);
      throw error;
    }
  }

  async getCartItemCount(): Promise<number> {
    console.log('🔢 Fetching cart item count');
    try {
      const response = await this.request<{ count: number }>('/cart/count');
      console.log('✅ Cart count:', response.count);
      return response.count;
    } catch (error) {
      console.error('❌ Error fetching cart count:', error);
      throw error;
    }
  }

  async checkout(): Promise<Purchase[]> {
    console.log('💳 Processing checkout');
    try {
      const response = await this.request<{ message: string; purchases: Purchase[] }>('/cart/checkout', {
        method: 'POST'
      });
      console.log('✅ Checkout response:', response);
      return response.purchases;
    } catch (error) {
      console.error('❌ Error during checkout:', error);
      throw error;
    }
  }

  // Book store endpoints
  async getBookStore(): Promise<BookStoreResponse[]> {
    console.log('🏪 Fetching book store');
    try {
      const response = await this.request<BookStoreResponse[]>('/books/store');
      console.log('✅ Book store response:', response);
      
      const books = response.map(book => ({
        ...book,
        coverImageUrl: book.coverImageUrl ? `${BASE_URL}/${book.coverImageUrl}` : ''
      }));
      
      return books;
    } catch (error) {
      console.error('❌ Error fetching book store:', error);
      throw error;
    }
  }
}

export const apiService = new ApiService();
export default apiService;
