import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Book, Topic, Test } from '../types';

export const BookDetailScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { book } = route.params as { book: Book };
  const [expandedTopics, setExpandedTopics] = useState<Set<number>>(new Set());

  const handleTopicPress = (topic: Topic) => {
    const newExpandedTopics = new Set(expandedTopics);
    if (newExpandedTopics.has(topic.id)) {
      newExpandedTopics.delete(topic.id);
    } else {
      newExpandedTopics.add(topic.id);
    }
    setExpandedTopics(newExpandedTopics);
  };

  const handleTestPress = (topic: Topic, test: Test) => {
    navigation.navigate('TestPreview', { book, topic, test });
  };

  const renderTestItem = (test: Test, topic: Topic) => (
    <TouchableOpacity
      key={test.id}
      style={styles.testItem}
      onPress={() => handleTestPress(topic, test)}
      activeOpacity={0.7}
    >
      <View style={styles.testItemContent}>
        <Ionicons name="document-text" size={16} color="#3b82f6" />
        <Text style={styles.testTitle}>{test.title}</Text>
      </View>
      <View style={styles.testActions}>
        <Text style={styles.questionCount}>
          {test.questions?.length || 0} Soru
        </Text>
        <Ionicons name="chevron-forward" size={16} color="#9ca3af" />
      </View>
    </TouchableOpacity>
  );

  const renderTopicItem = (topic: Topic) => {
    const isExpanded = expandedTopics.has(topic.id);
    const testCount = topic.tests?.length || 0;
    
    return (
      <View key={topic.id} style={styles.topicItem}>
        <TouchableOpacity
          style={styles.topicHeader}
          onPress={() => handleTopicPress(topic)}
          activeOpacity={0.7}
        >
          <View style={styles.topicHeaderContent}>
            <Ionicons name="bookmark" size={20} color="#10b981" />
            <Text style={styles.topicTitle}>{topic.title}</Text>
            <View style={styles.topicBadge}>
              <Text style={styles.topicBadgeText}>{testCount} Test</Text>
            </View>
          </View>
          <Ionicons 
            name={isExpanded ? "chevron-up" : "chevron-down"} 
            size={20} 
            color="#9ca3af" 
          />
        </TouchableOpacity>
        
        {isExpanded && topic.tests && (
          <View style={styles.testsList}>
            {topic.tests.map(test => renderTestItem(test, topic))}
          </View>
        )}
      </View>
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
          <Text style={styles.headerTitle}>Kitap Detayı</Text>
          <View style={styles.placeholder} />
        </View>

        {/* Content */}
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* Book Info */}
          <View style={styles.bookInfo}>
            <Image source={{ uri: book.coverImageUrl }} style={styles.bookCover} />
            <View style={styles.bookDetails}>
              <Text style={styles.bookTitle}>{book.title}</Text>
              <Text style={styles.bookPublisher}>{book.publisher}</Text>
              <View style={styles.bookStatus}>
                <Ionicons
                  name={book.paid ? "checkmark-circle" : "lock-closed"}
                  size={16}
                  color={book.paid ? "#10b981" : "#f59e0b"}
                />
                <Text style={[styles.statusText, { color: book.paid ? "#10b981" : "#f59e0b" }]}>
                  {book.paid ? "Erişilebilir" : "Kilitli"}
                </Text>
              </View>
            </View>
          </View>

          {/* Topics Section */}
          <View style={styles.topicsSection}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Konular</Text>
              <Text style={styles.sectionSubtitle}>
                {book.topics?.length || 0} konu bulundu
              </Text>
            </View>
            
            {book.topics && book.topics.length > 0 ? (
              <View style={styles.topicsList}>
                {book.topics.map(topic => renderTopicItem(topic))}
              </View>
            ) : (
              <View style={styles.emptyTopics}>
                <Ionicons name="bookmark-outline" size={48} color="#9ca3af" />
                <Text style={styles.emptyTopicsTitle}>Henüz konu yok</Text>
                <Text style={styles.emptyTopicsSubtitle}>
                  Bu kitap için henüz konu eklenmemiş
                </Text>
              </View>
            )}
          </View>

          {/* Quick Actions */}
          <View style={styles.quickActions}>
            <TouchableOpacity
              style={styles.actionButton}
              onPress={() => {
                Alert.alert('Bilgi', 'Yakında eklenecek!');
              }}
            >
              <Ionicons name="play-circle" size={20} color="#ffffff" />
              <Text style={styles.actionButtonText}>Hızlı Test Başlat</Text>
            </TouchableOpacity>
            
            <TouchableOpacity
              style={[styles.actionButton, styles.secondaryAction]}
              onPress={() => {
                Alert.alert('Bilgi', 'Yakında eklenecek!');
              }}
            >
              <Ionicons name="bookmark" size={20} color="#3b82f6" />
              <Text style={styles.secondaryActionText}>Favorilere Ekle</Text>
            </TouchableOpacity>
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
    backgroundColor: '#ffffff',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: 8,
    marginRight: 8,
  },
  headerTitle: {
    flex: 1,
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
    textAlign: 'center',
  },
  placeholder: {
    width: 40,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
  },
  bookInfo: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    marginBottom: 24,
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
    width: 80,
    height: 100,
    borderRadius: 12,
    marginRight: 20,
  },
  bookDetails: {
    flex: 1,
    justifyContent: 'center',
  },
  bookTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1e293b',
    marginBottom: 8,
  },
  bookPublisher: {
    fontSize: 14,
    color: '#64748b',
    marginBottom: 12,
  },
  bookStatus: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusText: {
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 8,
  },
  topicsSection: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    marginBottom: 24,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  sectionHeader: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1e293b',
    marginBottom: 4,
  },
  sectionSubtitle: {
    fontSize: 14,
    color: '#64748b',
  },
  topicsList: {
    // No additional padding needed
  },
  topicItem: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    marginBottom: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  topicHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#ffffff',
  },
  topicHeaderContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  topicTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginLeft: 12,
    flex: 1,
  },
  topicBadge: {
    backgroundColor: '#dbeafe',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    marginLeft: 12,
  },
  topicBadgeText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#1d4ed8',
  },
  testsList: {
    backgroundColor: '#f8fafc',
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
  },
  testItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  testItemContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  testTitle: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginLeft: 12,
  },
  testActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  questionCount: {
    fontSize: 12,
    color: '#64748b',
    marginRight: 8,
  },
  emptyTopics: {
    alignItems: 'center',
    padding: 40,
  },
  emptyTopicsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyTopicsSubtitle: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 20,
  },
  quickActions: {
    gap: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#3b82f6',
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  actionButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  secondaryAction: {
    backgroundColor: '#ffffff',
    borderWidth: 2,
    borderColor: '#3b82f6',
  },
  secondaryActionText: {
    color: '#3b82f6',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
});
