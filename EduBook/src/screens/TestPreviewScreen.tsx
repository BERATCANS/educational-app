import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Dimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import { Book, Topic, Test, Question } from '../types';

const { width: screenWidth } = Dimensions.get('window');

export const TestPreviewScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { book, topic, test } = route.params as { book: Book; topic: Topic; test: Test };
  
  const questions = test.questions || [];
  const totalQuestions = questions.length;

  const handleQuestionPress = (question: Question, questionIndex: number) => {
    navigation.navigate('Test', { 
      book, 
      topic, 
      test,
      initialQuestionIndex: questionIndex 
    });
  };

  const handleStartTest = () => {
    navigation.navigate('Test', { 
      book, 
      topic, 
      test,
      initialQuestionIndex: 0 
    });
  };

  const goBack = () => {
    navigation.goBack();
  };

  const renderQuestion = (question: Question, index: number) => (
    <TouchableOpacity
      key={question.id}
      style={styles.questionItem}
      onPress={() => handleQuestionPress(question, index)}
      activeOpacity={0.8}
    >
      <View style={styles.questionImagePlaceholder}>
        {/* Sadece gri alan, yazı yok */}
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <View style={styles.headerInfo}>
            <Text style={styles.headerTitle}>{topic.title}</Text>
            <Text style={styles.headerSubtitle}>
              Test {test.id}
            </Text>
          </View>
          <TouchableOpacity
            style={styles.startButton}
            onPress={handleStartTest}
            activeOpacity={0.8}
          >
            <Ionicons name="play" size={20} color="#ffffff" />
          </TouchableOpacity>
        </View>

        {/* Content */}
        <View style={styles.contentContainer}>
          <ScrollView 
            style={styles.scrollView}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            {/* Questions Container */}
            <View style={styles.questionsContainer}>
              {/* Vertical Center Line */}
              <View style={styles.verticalCenterLine} />
              
              {questions.map((question, index) => renderQuestion(question, index))}
            </View>
            
            {/* Start Test Button */}
            <View style={styles.bottomSection}>
              <TouchableOpacity
                style={styles.startTestButton}
                onPress={handleStartTest}
                activeOpacity={0.8}
              >
                <Ionicons name="play-circle" size={24} color="#ffffff" />
                <Text style={styles.startTestButtonText}>Testi Başlat</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    padding: screenWidth * 0.04,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: screenWidth * 0.02,
    marginRight: screenWidth * 0.03,
  },
  headerInfo: {
    flex: 1,
  },
  headerTitle: {
    fontSize: Math.max(16, screenWidth * 0.04),
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 2,
  },
  headerSubtitle: {
    fontSize: Math.max(12, screenWidth * 0.03),
    color: '#64748b',
  },
  startButton: {
    backgroundColor: '#10b981',
    width: screenWidth * 0.1,
    height: screenWidth * 0.1,
    borderRadius: screenWidth * 0.05,
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: screenWidth * 0.05,
  },
  questionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: screenWidth * 0.05,
    position: 'relative', // Dikey çizgi için
  },
  questionItem: {
    width: (screenWidth * 0.4), // 40% of screen width - 2 tane yan yana sığacak
    marginBottom: screenWidth * 0.03,
    marginHorizontal: screenWidth * 0.01,
  },
  questionImagePlaceholder: {
    height: screenWidth * 0.25, // 25% of screen width
    backgroundColor: '#f1f5f9',
    borderRadius: screenWidth * 0.02,
  },
  placeholderText: {
    fontSize: Math.max(14, screenWidth * 0.035),
    color: '#64748b',
    textAlign: 'center',
  },
  bottomSection: {
    alignItems: 'center',
    paddingVertical: screenWidth * 0.05,
  },
  startTestButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#10b981',
    paddingVertical: screenWidth * 0.04,
    paddingHorizontal: screenWidth * 0.08,
    borderRadius: screenWidth * 0.03,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: Math.max(1, screenWidth * 0.005),
    },
    shadowOpacity: 0.1,
    shadowRadius: screenWidth * 0.02,
    elevation: 4,
  },
  startTestButtonText: {
    color: '#ffffff',
    fontSize: Math.max(16, screenWidth * 0.04),
    fontWeight: '600',
    marginLeft: screenWidth * 0.02,
  },
  verticalCenterLine: {
    position: 'absolute',
    left: '50%',
    top: 0,
    width: 1,
    height: '100%', // Soruların yüksekliği kadar
    backgroundColor: '#e2e8f0',
    zIndex: 1,
  },
  contentContainer: {
    flex: 1,
    position: 'relative',
  },
});
