import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Question, Book } from '../types';

// Mock data - backend'deki flaggedQuestions ile uyumlu
const mockFlaggedQuestions: Question[] = [
  {
    id: 1,
    imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=İşaretlenen+Soru+1',
    answerNumber: 5,
    correctAnswer: 'A',
    sequenceNumber: 1,
    test: {
      id: 1,
      title: 'Üslü Sayılar Test 1',
      topic: {
        id: 1,
        title: 'Üslü Sayılar',
        book: {
          id: 1,
          title: 'Matematik 9. Sınıf',
          publisher: 'Metin Yayınları',
          coverImageUrl: 'https://via.placeholder.com/120x150/3b82f6/ffffff?text=M9',
          paid: true,
        }
      }
    }
  },
  {
    id: 2,
    imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=İşaretlenen+Soru+2',
    answerNumber: 5,
    correctAnswer: 'B',
    sequenceNumber: 2,
    test: {
      id: 2,
      title: 'Hareket Test 1',
      topic: {
        id: 4,
        title: 'Hareket',
        book: {
          id: 2,
          title: 'Fizik 10. Sınıf',
          publisher: 'Aydın Yayınları',
          coverImageUrl: 'https://via.placeholder.com/120x150/ef4444/ffffff?text=F10',
          paid: true,
        }
      }
    }
  },
  {
    id: 3,
    imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=İşaretlenen+Soru+3',
    answerNumber: 5,
    correctAnswer: 'C',
    sequenceNumber: 3,
    test: {
      id: 9,
      title: 'Kimyasal Bağlar Test 1',
      topic: {
        id: 7,
        title: 'Kimyasal Bağlar',
        book: {
          id: 3,
          title: 'Kimya 11. Sınıf',
          publisher: 'Palme Yayınları',
          coverImageUrl: 'https://via.placeholder.com/120x150/10b981/ffffff?text=K11',
          paid: true,
        }
      }
    }
  },
  {
    id: 4,
    imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=İşaretlenen+Soru+4',
    answerNumber: 5,
    correctAnswer: 'D',
    sequenceNumber: 4,
    test: {
      id: 11,
      title: 'Hücre Test 1',
      topic: {
        id: 9,
        title: 'Hücre',
        book: {
          id: 4,
          title: 'Biyoloji 9. Sınıf',
          publisher: 'Aydın Yayınları',
          coverImageUrl: 'https://via.placeholder.com/120x150/f59e0b/ffffff?text=B9',
          paid: true,
        }
      }
    }
  },
  {
    id: 5,
    imageUrl: 'https://via.placeholder.com/300x200/6b7280/ffffff?text=İşaretlenen+Soru+5',
    answerNumber: 5,
    correctAnswer: 'E',
    sequenceNumber: 5,
    test: {
      id: 14,
      title: 'Osmanlı Test 1',
      topic: {
        id: 11,
        title: 'Osmanlı Devleti',
        book: {
          id: 5,
          title: 'Tarih 10. Sınıf',
          publisher: 'Kök Yayınları',
          coverImageUrl: 'https://via.placeholder.com/120x150/8b5cf6/ffffff?text=T10',
          paid: true,
        }
      }
    }
  },
];

export const FlaggedQuestionsScreen: React.FC = () => {
  const [questions, setQuestions] = useState<Question[]>(mockFlaggedQuestions);
  const navigation = useNavigation();

  const handleRetryQuestion = (question: Question) => {
    Alert.alert(
      'Soru Tekrarı',
      `Bu soruyu tekrar çözmek istiyor musunuz?`,
      [
        { text: 'İptal', style: 'cancel' },
        { 
          text: 'Tekrar Dene', 
          onPress: () => {
            Alert.alert('Başarılı', 'Soru tekrar listesine eklendi!');
          }
        },
      ]
    );
  };

  const handleClearQuestion = (questionId: number) => {
    Alert.alert(
      'Soru Temizle',
      'Bu soruyu işaretlenen sorular listesinden kaldırmak istediğinizden emin misiniz?',
      [
        { text: 'İptal', style: 'cancel' },
        { 
          text: 'Temizle', 
          style: 'destructive',
          onPress: () => {
            setQuestions(prev => prev.filter(q => q.id !== questionId));
            Alert.alert('Başarılı', 'Soru listeden kaldırıldı!');
          }
        },
      ]
    );
  };

  const goBack = () => {
    navigation.goBack();
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>İşaretlenen Sorular</Text>
          <View style={styles.placeholder} />
        </View>

        {/* Content */}
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {questions.length === 0 ? (
            <View style={styles.emptyState}>
              <Ionicons name="checkmark-circle" size={80} color="#10b981" />
              <Text style={styles.emptyTitle}>Harika!</Text>
              <Text style={styles.emptySubtitle}>
                İşaretlenen sorunuz kalmadı. Tüm soruları başarıyla çözdünüz!
              </Text>
            </View>
          ) : (
            <View style={styles.questionsList}>
              {questions.map((item) => (
                <View key={item.id} style={styles.questionCard}>
                  <View style={styles.questionHeader}>
                    <View style={styles.bookInfo}>
                      <Ionicons name="book" size={16} color="#3b82f6" />
                      <Text style={styles.bookTitle}>
                        {item.test?.topic?.book?.title || 'Bilinmeyen Kitap'}
                      </Text>
                    </View>
                    <View style={styles.topicInfo}>
                      <Ionicons name="bookmark" size={14} color="#10b981" />
                      <Text style={styles.topicTitle}>
                        {item.test?.topic?.title || 'Bilinmeyen Konu'}
                      </Text>
                    </View>
                  </View>

                  <Image 
                    source={{ uri: item.imageUrl }} 
                    style={styles.questionImage}
                    resizeMode="cover"
                  />

                  <View style={styles.questionFooter}>
                    <View style={styles.questionMeta}>
                      <Text style={styles.testTitle}>
                        {item.test?.title || 'Bilinmeyen Test'}
                      </Text>
                      <Text style={styles.answerInfo}>
                        {item.answerNumber} seçenek • Doğru: {item.correctAnswer}
                      </Text>
                    </View>

                    <View style={styles.actionButtons}>
                      <TouchableOpacity
                        style={styles.actionButton}
                        onPress={() => handleRetryQuestion(item)}
                      >
                        <Ionicons name="refresh" size={16} color="#3b82f6" />
                        <Text style={styles.actionButtonText}>Tekrar Dene</Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[styles.actionButton, styles.clearButton]}
                        onPress={() => handleClearQuestion(item.id)}
                      >
                        <Ionicons name="close-circle" size={16} color="#ef4444" />
                        <Text style={[styles.actionButtonText, styles.clearButtonText]}>
                          Temizle
                        </Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>
              ))}
            </View>
          )}

          {/* Help Section */}
          <View style={styles.helpSection}>
            <Text style={styles.helpTitle}>💡 İpuçları</Text>
            <View style={styles.helpTips}>
              <Text style={styles.helpTip}>
                • İşaretlenen soruları tekrar deneyin
              </Text>
              <Text style={styles.helpTip}>
                • Konu tekrarı yaparak bilgilerinizi pekiştirin
              </Text>
              <Text style={styles.helpTip}>
                • Yardım almak için öğretmeninize danışın
              </Text>
            </View>
          </View>
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
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: 4,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1e293b',
  },
  placeholder: {
    width: 32,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    padding: 20,
  },
  emptyState: {
    alignItems: 'center',
    padding: 60,
  },
  emptyTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#10b981',
    marginTop: 20,
    marginBottom: 12,
  },
  emptySubtitle: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
    lineHeight: 24,
  },
  questionsList: {
    gap: 16,
  },
  questionCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  questionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  bookInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bookTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#3b82f6',
    marginLeft: 8,
  },
  topicInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  topicTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
    marginLeft: 8,
  },
  questionImage: {
    width: '100%',
    height: 200,
    borderRadius: 12,
    marginBottom: 16,
  },
  questionFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  questionMeta: {
    flex: 1,
  },
  testTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 4,
  },
  answerInfo: {
    fontSize: 12,
    color: '#64748b',
  },
  subjectBadge: {
    backgroundColor: '#f1f5f9',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
  },
  subjectText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#475569',
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#f8fafc',
  },
  actionButtonText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#3b82f6',
    marginLeft: 6,
  },
  clearButton: {
    backgroundColor: '#fef2f2',
  },
  clearButtonText: {
    color: '#ef4444',
  },
  helpSection: {
    backgroundColor: '#ffffff',
    marginTop: 32,
    padding: 20,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  helpTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 16,
  },
  helpTips: {
    gap: 8,
  },
  helpTip: {
    fontSize: 14,
    color: '#64748b',
    lineHeight: 20,
  },
});
