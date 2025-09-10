import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  FlatList,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Book } from '../types';

// Mock data - kullanıcının kitapları
const mockBooks: Book[] = [
  {
    id: 1,
    title: 'Matematik 9. Sınıf',
    publisher: 'Metin Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Matematik+9.+Sinif&v=2',
    paid: true,
    topics: [
      {
        id: 1,
        title: 'Üslü Sayılar',
        tests: [
          {
            id: 1,
            title: 'Üslü Sayılar Test 1',
            questions: [
              {
                id: 1,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+1',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 2,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+2',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 3,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+3',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 4,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+4',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
              {
                id: 5,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+5',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 5,
              },
              {
                id: 6,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+6',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 6,
              },
              {
                id: 7,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+7',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 7,
              },
              {
                id: 8,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+8',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 8,
              },
              {
                id: 9,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+9',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 9,
              },
              {
                id: 10,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+10',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 10,
              },
              {
                id: 11,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+11',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 11,
              },
              {
                id: 12,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+12',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 12,
              },
            ],
          },
          {
            id: 2,
            title: 'Üslü Sayılar Test 2',
            questions: [
              {
                id: 6,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+6',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 7,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+7',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 8,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+8',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
      {
        id: 2,
        title: 'Köklü Sayılar',
        tests: [
          {
            id: 3,
            title: 'Köklü Sayılar Test 1',
            questions: [
              {
                id: 9,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+9',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 10,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+10',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 11,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+11',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 12,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+12',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
        ],
      },
      {
        id: 3,
        title: 'Denklemler',
        tests: [
          {
            id: 4,
            title: 'Denklemler Test 1',
            questions: [
              {
                id: 13,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+13',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 14,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+14',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 15,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+15',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
      {
        id: 7,
        title: 'Köklü Sayılar',
        tests: [
          {
            id: 1,
            title: 'Köklü Sayılar Test 1',
            questions: [
              {
                id: 16,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+16',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 17,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+17',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 18,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+18',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 19,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+19',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
              {
                id: 20,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+20',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 5,
              },
              {
                id: 21,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+21',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 6,
              },
            ],
          },
          {
            id: 2,
            title: 'Köklü Sayılar Test 2',
            questions: [
              {
                id: 22,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+22',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 1,
              },
              {
                id: 23,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+23',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 2,
              },
              {
                id: 24,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+24',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 3,
              },
              {
                id: 25,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+25',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 4,
              },
            ],
          },
        ],
      },
      {
        id: 8,
        title: 'Çarpanlara Ayırma',
        tests: [
          {
            id: 1,
            title: 'Çarpanlara Ayırma Test 1',
            questions: [
              {
                id: 26,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+26',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 27,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+27',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 28,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+28',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 29,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+29',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
              {
                id: 30,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+30',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 5,
              },
              {
                id: 31,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+31',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 6,
              },
              {
                id: 32,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+32',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 7,
              },
              {
                id: 33,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+33',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 8,
              },
            ],
          },
          {
            id: 2,
            title: 'Çarpanlara Ayırma Test 2',
            questions: [
              {
                id: 34,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+34',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 1,
              },
              {
                id: 35,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+35',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 2,
              },
              {
                id: 36,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+36',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 3,
              },
              {
                id: 37,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+37',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 4,
              },
              {
                id: 38,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+38',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 5,
              },
            ],
          },
          {
            id: 3,
            title: 'Çarpanlara Ayırma Test 3',
            questions: [
              {
                id: 39,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+39',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 1,
              },
              {
                id: 40,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+40',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 2,
              },
              {
                id: 41,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+41',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 3,
              },
              {
                id: 42,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+42',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 4,
              },
              {
                id: 43,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+43',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 5,
              },
              {
                id: 44,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+44',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 6,
              },
            ],
          },
        ],
      },
      {
        id: 9,
        title: 'Fonksiyonlar',
        tests: [
          {
            id: 1,
            title: 'Fonksiyonlar Test 1',
            questions: [
              {
                id: 45,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+45',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 1,
              },
              {
                id: 46,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+46',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 2,
              },
              {
                id: 47,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+47',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 3,
              },
              {
                id: 48,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+48',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 4,
              },
              {
                id: 49,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+49',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 5,
              },
              {
                id: 50,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+50',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 6,
              },
              {
                id: 51,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+51',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 7,
              },
              {
                id: 52,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+52',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 8,
              },
              {
                id: 53,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+53',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 9,
              },
              {
                id: 54,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+54',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 10,
              },
            ],
          },
          {
            id: 2,
            title: 'Fonksiyonlar Test 2',
            questions: [
              {
                id: 55,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+55',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 1,
              },
              {
                id: 56,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+56',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 2,
              },
              {
                id: 57,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+57',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 3,
              },
              {
                id: 58,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+58',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 4,
              },
              {
                id: 59,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+59',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 5,
              },
              {
                id: 60,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+60',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 6,
              },
            ],
          },
        ],
      },
      {
        id: 10,
        title: 'Denklem Sistemleri',
        tests: [
          {
            id: 1,
            title: 'Denklem Sistemleri Test 1',
            questions: [
              {
                id: 61,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+61',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 62,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+62',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 63,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+63',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 64,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+64',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
              {
                id: 65,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+65',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 5,
              },
              {
                id: 66,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+66',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 6,
              },
              {
                id: 67,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+67',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 7,
              },
            ],
          },
        ],
      },
      {
        id: 11,
        title: 'Eşitsizlikler',
        tests: [
          {
            id: 1,
            title: 'Eşitsizlikler Test 1',
            questions: [
              {
                id: 68,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+68',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 1,
              },
              {
                id: 69,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+69',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 2,
              },
              {
                id: 70,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+70',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 3,
              },
              {
                id: 71,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+71',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 4,
              },
              {
                id: 72,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+72',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 5,
              },
            ],
          },
          {
            id: 2,
            title: 'Eşitsizlikler Test 2',
            questions: [
              {
                id: 73,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+73',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 1,
              },
              {
                id: 74,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+74',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 2,
              },
              {
                id: 75,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+75',
                answerNumber: 5,
                correctAnswer: 'E',
                sequenceNumber: 3,
              },
              {
                id: 76,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+76',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 4,
              },
              {
                id: 77,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+77',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 5,
              },
              {
                id: 78,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+78',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 6,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 2,
    title: 'Fizik 10. Sınıf',
    publisher: 'Aydın Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Fizik+10.+Sinif&v=2',
    paid: true,
    topics: [
      {
        id: 4,
        title: 'Hareket',
        tests: [
          {
            id: 1,
            title: 'Hareket Test 1',
            questions: [
              {
                id: 16,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+16',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 17,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+17',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 18,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+18',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 19,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+19',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
          {
            id: 2,
            title: 'Hareket Test 2',
            questions: [
              {
                id: 20,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+20',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 21,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+21',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 22,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+22',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
      {
        id: 5,
        title: 'Kuvvet',
        tests: [
          {
            id: 1,
            title: 'Kuvvet Test 1',
            questions: [
              {
                id: 23,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+23',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 24,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+24',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 25,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+25',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 26,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+26',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
        ],
      },
      {
        id: 6,
        title: 'Enerji',
        tests: [
          {
            id: 8,
            title: 'Enerji Test 1',
            questions: [
              {
                id: 27,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+27',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 28,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+28',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 29,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+29',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 3,
    title: 'Kimya 11. Sınıf',
    publisher: 'Palme Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Kimya+11.+Sinif&v=2',
    paid: true,
    topics: [
      {
        id: 7,
        title: 'Kimyasal Bağlar',
        tests: [
          {
            id: 1,
            title: 'Kimyasal Bağlar Test 1',
            questions: [
              {
                id: 30,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+30',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 31,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+31',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 32,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+32',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 33,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+33',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
        ],
      },
      {
        id: 8,
        title: 'Asitler ve Bazlar',
        tests: [
          {
            id: 2,
            title: 'Asitler ve Bazlar Test 1',
            questions: [
              {
                id: 34,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+34',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 35,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+35',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 36,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+36',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 4,
    title: 'Biyoloji 9. Sınıf',
    publisher: 'Aydın Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Biyoloji+9.+Sinif&v=2',
    paid: true,
    topics: [
      {
        id: 9,
        title: 'Hücre',
        tests: [
          {
            id: 1,
            title: 'Hücre Test 1',
            questions: [
              {
                id: 37,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+37',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 38,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+38',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 39,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+39',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 40,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+40',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
          {
            id: 2,
            title: 'Hücre Test 2',
            questions: [
              {
                id: 41,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+41',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 42,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+42',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 43,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+43',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
      {
        id: 10,
        title: 'Genetik',
        tests: [
          {
            id: 1,
            title: 'Genetik Test 1',
            questions: [
              {
                id: 44,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+44',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 45,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+45',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 46,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+46',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 5,
    title: 'Tarih 10. Sınıf',
    publisher: 'Kök Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Tarih+10.+Sinif&v=2',
    paid: true,
    topics: [
      {
        id: 11,
        title: 'Osmanlı Devleti',
        tests: [
          {
            id: 1,
            title: 'Osmanlı Test 1',
            questions: [
              {
                id: 47,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+47',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 48,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+48',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 49,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+49',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
              {
                id: 50,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+50',
                answerNumber: 5,
                correctAnswer: 'D',
                sequenceNumber: 4,
              },
            ],
          },
        ],
      },
      {
        id: 12,
        title: 'İnkılap Tarihi',
        tests: [
          {
            id: 1,
            title: 'İnkılap Test 1',
            questions: [
              {
                id: 51,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+51',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 52,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+52',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 53,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+53',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 6,
    title: 'Coğrafya 9. Sınıf',
    publisher: 'Mega Yayınları',
    coverImageUrl: 'https://via.placeholder.com/120x150/2563eb/ffffff?text=Cografya+9.+Sinif&v=2',
    paid: false,
    topics: [
      {
        id: 13,
        title: 'Doğal Ortam',
        tests: [
          {
            id: 1,
            title: 'Doğal Ortam Test 1',
            questions: [
              {
                id: 54,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+54',
                answerNumber: 5,
                correctAnswer: 'A',
                sequenceNumber: 1,
              },
              {
                id: 55,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+55',
                answerNumber: 5,
                correctAnswer: 'B',
                sequenceNumber: 2,
              },
              {
                id: 56,
                imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=Soru+56',
                answerNumber: 5,
                correctAnswer: 'C',
                sequenceNumber: 3,
              },
            ],
          },
        ],
      },
    ],
  },
];

export const LibraryScreen: React.FC = () => {
  const navigation = useNavigation<any>();

  const handleBookPress = (book: Book) => {
    navigation.navigate('BookDetail', { book });
  };

  const handleAddBook = () => {
    navigation.navigate('AddBook');
  };

  const renderBookItem = ({ item }: { item: Book }) => {
    const topicCount = item.topics?.length || 0;
    const testCount = item.topics?.reduce((total, topic) => total + (topic.tests?.length || 0), 0) || 0;
    
    return (
      <TouchableOpacity
        style={styles.bookCard}
        onPress={() => handleBookPress(item)}
        activeOpacity={0.8}
      >
        <Image source={{ uri: item.coverImageUrl }} style={styles.bookCover} />
        <View style={styles.bookInfo}>
          <Text style={styles.bookTitle} numberOfLines={2}>
            {item.title}
          </Text>
          <Text style={styles.bookPublisher} numberOfLines={1}>
            {item.publisher}
          </Text>
          
          <View style={styles.bookStats}>
            <View style={styles.bookStatItem}>
              <Ionicons name="library" size={12} color="#64748b" />
              <Text style={styles.statText}>{topicCount} Konu</Text>
            </View>
            <View style={styles.bookStatItem}>
              <Ionicons name="document-text" size={12} color="#64748b" />
              <Text style={styles.statText}>{testCount} Test</Text>
            </View>
          </View>
          
          <View style={styles.bookStatus}>
            <Ionicons
              name={item.paid ? "checkmark-circle" : "lock-closed"}
              size={16}
              color={item.paid ? "#10b981" : "#f59e0b"}
            />
            <Text style={[styles.statusText, { color: item.paid ? "#10b981" : "#f59e0b" }]}>
              {item.paid ? "Erişilebilir" : "Kilitli"}
            </Text>
          </View>
        </View>
        
        <View style={styles.arrowIcon}>
          <Ionicons name="chevron-forward" size={20} color="#9ca3af" />
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Kütüphanem</Text>
          <TouchableOpacity
            style={styles.addButton}
            onPress={handleAddBook}
            activeOpacity={0.8}
          >
            <Ionicons name="add" size={24} color="#ffffff" />
          </TouchableOpacity>
        </View>

        {/* Content */}
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {mockBooks.length === 0 ? (
            <View style={styles.emptyState}>
              <Ionicons name="library-outline" size={80} color="#9ca3af" />
              <Text style={styles.emptyTitle}>Henüz kitabınız yok</Text>
              <Text style={styles.emptySubtitle}>
                Kitap kodunu girerek ilk kitabınızı ekleyin
              </Text>
              <TouchableOpacity
                style={styles.addFirstBookButton}
                onPress={handleAddBook}
              >
                <Text style={styles.addFirstBookText}>İlk Kitabımı Ekle</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.booksList}>
              {mockBooks.map(book => (
                <View key={book.id} style={styles.bookWrapper}>
                  {renderBookItem({ item: book })}
                </View>
              ))}
            </View>
          )}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1e293b',
  },
  addButton: {
    backgroundColor: '#3b82f6',
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
  },
  emptyState: {
    alignItems: 'center',
    padding: 40,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#374151',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
  },
  addFirstBookButton: {
    backgroundColor: '#3b82f6',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  addFirstBookText: {
    color: '#ffffff',
    fontWeight: '600',
    fontSize: 14,
  },
  booksList: {
    // paddingHorizontal: 16, // This is now handled by scrollContent
  },
  bookWrapper: {
    marginBottom: 16,
  },
  bookCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  bookCover: {
    width: 60,
    height: 80,
    borderRadius: 8,
    marginRight: 16,
  },
  bookInfo: {
    flex: 1,
  },
  bookTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 4,
  },
  bookPublisher: {
    fontSize: 12,
    color: '#64748b',
    marginBottom: 8,
  },
  bookStats: {
    flexDirection: 'row',
    marginBottom: 8,
    gap: 16,
  },
  bookStatItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statText: {
    fontSize: 11,
    color: '#64748b',
    marginLeft: 4,
  },
  bookStatus: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusText: {
    fontSize: 11,
    fontWeight: '500',
    marginLeft: 4,
  },
  arrowIcon: {
    paddingLeft: 10,
  },
});
