import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  Image,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Question, Book } from '../types';
import { apiService as api } from '../services/api';


export const FlaggedQuestionsScreen: React.FC = () => {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchFlaggedQuestions = async () => {
      try {
        const data = await api.getFlaggedQuestions();
        setQuestions(data);
        setError(null);
      } catch (err) {
        setError('İşaretlenen soruları yüklerken bir hata oluştu.');
      } finally {
        setLoading(false);
      }
    };

    fetchFlaggedQuestions();
  }, []);
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
            // Test bilgisini al (şimdilik mock data)
            const mockBook = {
              id: 1,
              title: 'Matematik 9. Sınıf',
              publisher: 'EduBook Yayınları',
              coverImageUrl: 'https://via.placeholder.com/300x400/2563eb/ffffff?text=Matematik+9.+Sinif&v=2',
              paid: true,
            };
            
            const mockTopic = {
              id: 1,
              title: 'Sayılar ve İşlemler',
            };
            
            const mockTest = {
              id: question.test?.id || 1,
              title: question.test?.title || 'Sayılar Test 1',
            };
            
            // TestScreen'e navigate et
            (navigation as any).navigate('Test', {
              book: mockBook,
              topic: mockTopic,
              test: mockTest,
              initialQuestionIndex: 0, // İlk sorudan başla
            });
          }
        },
      ]
    );
  };

  const handleClearQuestion = async (questionId: number) => {
    Alert.alert(
      'Soru Temizle',
      'Bu soruyu işaretlenen sorular listesinden kaldırmak istediğinizden emin misiniz?',
      [
        { text: 'İptal', style: 'cancel' },
        { 
          text: 'Temizle', 
          style: 'destructive',
          onPress: async () => {
            try {
              // Backend'de unflag yap
              await api.unflagQuestion(1, questionId); // userId=1 şimdilik
              
              // Frontend'den kaldır
              setQuestions(prev => prev.filter(q => q.id !== questionId));
              Alert.alert('Başarılı', 'Soru listeden kaldırıldı!');
            } catch (error) {
              console.error('Unflag error:', error);
              Alert.alert('Hata', 'Soru kaldırılırken bir hata oluştu.');
            }
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

        {loading ? (
          <View style={styles.centerContainer}>
            <ActivityIndicator size="large" color="#0284c7" />
          </View>
        ) : error ? (
          <View style={styles.centerContainer}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        ) : questions.length === 0 ? (
          <View style={styles.centerContainer}>
            <Text style={styles.emptyText}>Henüz işaretlenen soru bulunmuyor.</Text>
          </View>
        ) : (
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
                        Matematik Kitabı
                      </Text>
                    </View>
                    <View style={styles.topicInfo}>
                      <Ionicons name="bookmark" size={14} color="#10b981" />
                      <Text style={styles.topicTitle}>
                        Matematik
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
                        Matematik Test 1
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
        )}
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    color: '#ef4444',
    fontSize: 16,
    textAlign: 'center',
  },
  emptyText: {
    color: '#6b7280',
    fontSize: 16,
    textAlign: 'center',
  },
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
