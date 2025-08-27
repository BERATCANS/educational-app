// User entity
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  ownedBooks: Book[];
  flaggedQuestions?: Question[];
}

// Book entity
export interface Book {
  id: number;
  title: string;
  publisher: string;
  coverImageUrl: string;
  paid: boolean;
  codes?: BookCode[];
  topics?: Topic[];
  owners?: User[];
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
  test?: Test;
  imageUrl: string; // Sadece resim URL'i
  answerNumber: number; // Sabit 5
  correctAnswer?: 'A' | 'B' | 'C' | 'D' | 'E';
  sequenceNumber: number;
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
  username: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password: string;
  role: 'STUDENT' | 'TEACHER';
}

export interface AuthResponse {
  user: User;
  token: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}
