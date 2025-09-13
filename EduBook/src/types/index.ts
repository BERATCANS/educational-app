// User entity - Backend User entity ile uyumlu
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  ownedBooks: Book[]; // Backend'de Set<Book> ama frontend'de array olarak kullanılıyor
  flaggedQuestions?: Question[]; // Backend'de Set<Question> ama frontend'de array olarak kullanılıyor
}

// Book entity - Backend BookResponse'u ile uyumlu
export interface Book {
  id: number;
  title: string;
  publisher: string;
  coverImageUrl: string;
  paid?: boolean; // Backend'de yok, frontend'de varsayılan true
  topics?: Topic[]; // Backend'de yok, frontend'de boş array
}

// Topic entity
export interface Topic {
  id: number;
  title: string;
  book?: Book;
  tests?: Test[];
}

// Test entity
export interface Test {
  id: number;
  title: string;
  topic?: Topic;
  questions?: Question[];
}

// Question entity - Backend'e uygun güncellendi
export interface Question {
  id: number;
  imageUrl: string;
  answerNumber: number;
  correctAnswer?: 'A' | 'B' | 'C' | 'D' | 'E';
  hints: string[];
  explanation: string;
  videoUrl?: string;
}

// OptionLetter enum - Backend'e uygun
export type OptionLetter = 'A' | 'B' | 'C' | 'D' | 'E';

// Drawing types
export interface DrawingPoint {
  x: number;
  y: number;
}

export interface DrawingLine {
  id: string;
  points: DrawingPoint[];
  color: string;
  thickness: number;
  timestamp: number;
}

export interface Drawing {
  id: string;
  lines: DrawingLine[];
  questionId: number;
  testId: number;
  bookId: number;
}

export type DrawingTool = 'pen' | 'eraser' | 'line' | 'clear';
export type DrawingColor = '#000000' | '#ff0000' | '#0000ff' | '#00ff00' | '#ffff00';
export type DrawingThickness = 1 | 3 | 5 | 8;

// BookCode entity
export interface BookCode {
  id: number;
  code: string;
  book?: Book;
  redeemedAt?: Date;
}

// API Request/Response types
export interface LoginRequest {
  identifier: string; // Backend identifier bekliyor (email, telefon veya username)
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}
