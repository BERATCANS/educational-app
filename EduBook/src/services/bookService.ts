import { Book } from '../types';
import { apiService } from './api';

class BookService {
  // Get books for a specific user
  async getBooksForUser(userId: number): Promise<Book[]> {
    try {
      return await apiService.getBooksForUser(userId);
    } catch (error) {
      console.error('Failed to fetch books for user:', error);
      throw error;
    }
  }

  // Use a book code to add a book to user's library
  async useBookCode(userId: number, code: string): Promise<string> {
    try {
      return await apiService.useBookCode(userId, code);
    } catch (error) {
      console.error('Failed to use book code:', error);
      throw error;
    }
  }

  // Add a new book (admin/teacher function)
  async addBook(bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }): Promise<Book> {
    try {
      return await apiService.addBook(bookData);
    } catch (error) {
      console.error('Failed to add book:', error);
      throw error;
    }
  }

  // Update an existing book
  async updateBook(bookId: number, bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }): Promise<Book> {
    try {
      return await apiService.updateBook(bookId, bookData);
    } catch (error) {
      console.error('Failed to update book:', error);
      throw error;
    }
  }

  // Delete a book
  async deleteBook(bookId: number): Promise<string> {
    try {
      return await apiService.deleteBook(bookId);
    } catch (error) {
      console.error('Failed to delete book:', error);
      throw error;
    }
  }

  // Get questions for a specific book
  async getQuestionsForBook(bookId: number): Promise<any[]> {
    try {
      return await apiService.getQuestionsForBook(bookId);
    } catch (error) {
      console.error('Failed to fetch questions for book:', error);
      throw error;
    }
  }

  // Get tests for a specific book
  async getTestsForBook(bookId: number): Promise<any[]> {
    try {
      return await apiService.getTestsForBook(bookId);
    } catch (error) {
      console.error('Failed to fetch tests for book:', error);
      throw error;
    }
  }

  // Validate book code format (client-side validation)
  validateBookCode(code: string): { isValid: boolean; message?: string } {
    if (!code || code.trim().length === 0) {
      return { isValid: false, message: 'Book code is required' };
    }

    if (code.length < 6) {
      return { isValid: false, message: 'Book code must be at least 6 characters' };
    }

    if (code.length > 20) {
      return { isValid: false, message: 'Book code must not exceed 20 characters' };
    }

    // Check if code contains only alphanumeric characters and hyphens
    const codePattern = /^[A-Za-z0-9-]+$/;
    if (!codePattern.test(code)) {
      return { isValid: false, message: 'Book code can only contain letters, numbers, and hyphens' };
    }

    return { isValid: true };
  }

  // Validate book data before sending to API
  validateBookData(bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!bookData.title || bookData.title.trim().length === 0) {
      errors.push('Title is required');
    } else if (bookData.title.length > 200) {
      errors.push('Title must not exceed 200 characters');
    }

    if (!bookData.publisher || bookData.publisher.trim().length === 0) {
      errors.push('Publisher is required');
    } else if (bookData.publisher.length > 100) {
      errors.push('Publisher must not exceed 100 characters');
    }

    if (bookData.coverImageUrl && bookData.coverImageUrl.length > 500) {
      errors.push('Cover image URL must not exceed 500 characters');
    }

    if (bookData.topics && bookData.topics.length > 0) {
      bookData.topics.forEach((topic, index) => {
        if (!topic || topic.trim().length === 0) {
          errors.push(`Topic ${index + 1} cannot be empty`);
        } else if (topic.length > 100) {
          errors.push(`Topic ${index + 1} must not exceed 100 characters`);
        }
      });
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  // Format book data for display
  formatBookForDisplay(book: Book): Book & { 
    displayTitle: string;
    displayPublisher: string;
    isNew?: boolean;
  } {
    return {
      ...book,
      displayTitle: book.title || 'Untitled Book',
      displayPublisher: book.publisher || 'Unknown Publisher',
      isNew: false
    };
  }

  // Filter books by type (paid/free)
  filterBooksByType(books: Book[], paid: boolean): Book[] {
    return books.filter(book => book.paid === paid);
  }

  // Search books by title or publisher
  searchBooks(books: Book[], searchTerm: string): Book[] {
    if (!searchTerm || searchTerm.trim().length === 0) {
      return books;
    }

    const term = searchTerm.toLowerCase().trim();
    return books.filter(book => 
      book.title.toLowerCase().includes(term) ||
      book.publisher.toLowerCase().includes(term)
    );
  }

  // Sort books by different criteria
  sortBooks(books: Book[], sortBy: 'title' | 'publisher' | 'paid' = 'title'): Book[] {
    return [...books].sort((a, b) => {
      switch (sortBy) {
        case 'title':
          return a.title.localeCompare(b.title);
        case 'publisher':
          return a.publisher.localeCompare(b.publisher);
        case 'paid':
          return Number(b.paid) - Number(a.paid); // Paid books first
        default:
          return 0;
      }
    });
  }
}

export const bookService = new BookService();
export default bookService;
