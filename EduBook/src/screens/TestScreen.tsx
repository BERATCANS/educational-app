import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Alert,
  Dimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { SimpleDrawing } from '../components/SimpleDrawing';
import { useNavigation, useRoute } from '@react-navigation/native';
import { Book, Topic, Test, Question } from '../types';
import * as SecureStore from 'expo-secure-store';


const { width: screenWidth } = Dimensions.get('window');

export const TestScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { book, topic, test, initialQuestionIndex = 0 } = route.params as { 
    book: Book; 
    topic: Topic; 
    test: Test; 
    initialQuestionIndex?: number;
  };
  
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(initialQuestionIndex);
  const [selectedAnswers, setSelectedAnswers] = useState<Map<number, string>>(new Map());
  const [isTestCompleted, setIsTestCompleted] = useState(false);
  const [showDrawingCanvas, setShowDrawingCanvas] = useState(false);
  const [currentTool, setCurrentTool] = useState<'pen' | 'eraser' | 'line'>('pen');
  const [currentColor, setCurrentColor] = useState('#000000');
  const [currentThickness, setCurrentThickness] = useState(2);
  const [drawingsByQuestion, setDrawingsByQuestion] = useState<{[questionId: number]: Array<{id: string, path: string, color: string, thickness: number}>}>({});

  const questions = test.questions || [];
  const currentQuestion = questions[currentQuestionIndex];
  const totalQuestions = questions.length;
  
  // Current question'ın çizimlerini al
  const currentQuestionPaths = drawingsByQuestion[currentQuestion?.id] || [];

  // Test yüklendiğinde çizimleri yükle
  useEffect(() => {
    loadDrawingsFromStorage();
  }, [test.id]);

  // Çizimler değiştiğinde kaydet
  useEffect(() => {
    if (Object.keys(drawingsByQuestion).length > 0) {
      saveDrawingsToStorage();
    }
  }, [drawingsByQuestion]);

  const handleAnswerSelect = (answer: string) => {
    const newSelectedAnswers = new Map(selectedAnswers);
    newSelectedAnswers.set(currentQuestion.id, answer);
    setSelectedAnswers(newSelectedAnswers);
  };

  const handleNextQuestion = () => {
    if (currentQuestionIndex < totalQuestions - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    } else {
      // Test tamamlandı
      setIsTestCompleted(true);
    }
  };

  const handlePreviousQuestion = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
    }
  };

  const handleFinishTest = () => {
    const answeredQuestions = selectedAnswers.size;
    const correctAnswers = Array.from(selectedAnswers.entries()).filter(
      ([questionId, answer]) => {
        const question = questions.find(q => q.id === questionId);
        return question && question.correctAnswer === answer;
      }
    ).length;

    Alert.alert(
      'Test Tamamlandı!',
      `Sonuçlarınız:\n\n` +
      `Toplam Soru: ${totalQuestions}\n` +
      `Cevaplanan: ${answeredQuestions}\n` +
      `Doğru: ${correctAnswers}\n` +
      `Yanlış: ${answeredQuestions - correctAnswers}\n` +
      `Başarı Oranı: ${Math.round((correctAnswers / totalQuestions) * 100)}%`,
      [
        {
          text: 'Testi Kapat',
          onPress: async () => {
            await saveDrawingsToStorage();
            navigation.goBack();
          },
        },
        {
          text: 'Tekrar Çöz',
          onPress: () => {
            setCurrentQuestionIndex(0);
            setSelectedAnswers(new Map());
            setIsTestCompleted(false);
          },
        },
      ]
    );
  };

  const goBack = async () => {
    // Çıkmadan önce çizimleri kaydet
    await saveDrawingsToStorage();
    
    if (selectedAnswers.size > 0) {
      Alert.alert(
        'Testten Çık',
        'Testi yarıda bırakmak istediğinizden emin misiniz? İlerlemeniz kaydedilmeyecek.',
        [
          { text: 'İptal', style: 'cancel' },
          { text: 'Çık', style: 'destructive', onPress: () => navigation.goBack() },
        ]
      );
    } else {
      navigation.goBack();
    }
  };

  const handleStartTest = () => {
    navigation.navigate('Test', { 
      book, 
      topic, 
      test,
      initialQuestionIndex: 0 
    });
  };

  const handleDrawingToggle = () => {
    setShowDrawingCanvas(!showDrawingCanvas);
  };

  const handleToolSelect = (tool: 'pen' | 'eraser' | 'line') => {
    setCurrentTool(tool);
  };

  const handleColorSelect = (color: string) => {
    setCurrentColor(color);
    // Line modundaysa line'da kal, değilse pen'e geç
    if (currentTool === 'eraser') {
      setCurrentTool('pen');
    }
  };

  const handleThicknessSelect = (thickness: number) => {
    setCurrentThickness(thickness);
  };

  const handleUndo = () => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: (prev[currentQuestion.id] || []).slice(0, -1)
    }));
  };

  const handleClear = () => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: []
    }));
  };

  const handlePathsChange = (newPaths: any[]) => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: newPaths
    }));
  };

  // Storage fonksiyonları
  const getStorageKey = () => `drawings_book${book.id}_test${test.id}`;

  const saveDrawingsToStorage = async () => {
    try {
      const key = getStorageKey();
      await SecureStore.setItemAsync(key, JSON.stringify(drawingsByQuestion));
    } catch (error) {
      console.error('Çizim kaydetme hatası:', error);
    }
  };

  const loadDrawingsFromStorage = async () => {
    try {
      const key = getStorageKey();
      const saved = await SecureStore.getItemAsync(key);
      if (saved) {
        const parsed = JSON.parse(saved);
        setDrawingsByQuestion(parsed);
      }
    } catch (error) {
      console.error('Çizim yükleme hatası:', error);
    }
  };





  const renderQuestion = () => {
    if (!currentQuestion) return null;

    const selectedAnswer = selectedAnswers.get(currentQuestion.id);
    const optionLetters: ('A' | 'B' | 'C' | 'D' | 'E')[] = ['A', 'B', 'C', 'D', 'E'];

    return (
      <View style={styles.questionContainer}>
        {/* Question Header */}
        <View style={styles.questionHeader}>
          <Text style={styles.questionNumber}>
            Soru {currentQuestionIndex + 1} / {totalQuestions}
          </Text>
          <View style={styles.questionProgress}>
            <View 
              style={[
                styles.progressBar, 
                { width: `${((currentQuestionIndex + 1) / totalQuestions) * 100}%` }
              ]} 
            />
          </View>
        </View>

        {/* Question Image */}
        <View style={styles.questionImageContainer}>
          <Image 
            source={{ uri: currentQuestion.imageUrl }} 
            style={styles.questionImage}
            resizeMode="contain"
          />
          
          {/* Drawing Component Overlay */}
          <SimpleDrawing
            isActive={showDrawingCanvas}
            currentColor={currentColor}
            currentThickness={currentThickness}
            currentTool={currentTool}
            paths={currentQuestionPaths}
            onPathsChange={handlePathsChange}
          />
        </View>

        {/* Answer Options */}
        <View style={styles.answerOptions}>
          {optionLetters.slice(0, currentQuestion.answerNumber).map((letter) => (
            <TouchableOpacity
              key={letter}
              style={[
                styles.answerOption,
                selectedAnswer === letter && styles.selectedAnswerOption
              ]}
              onPress={() => handleAnswerSelect(letter)}
              activeOpacity={0.7}
            >
              <Text style={[
                styles.answerLetterText,
                selectedAnswer === letter && styles.selectedAnswerLetterText
              ]}>
                {letter}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>
    );
  };

  const renderTestCompleted = () => (
    <View style={styles.completedContainer}>
      <Ionicons name="checkmark-circle" size={80} color="#10b981" />
      <Text style={styles.completedTitle}>Test Tamamlandı!</Text>
      <Text style={styles.completedSubtitle}>
        Tüm soruları cevapladınız. Sonuçlarınızı görmek için "Testi Bitir" butonuna tıklayın.
      </Text>
      
      <View style={styles.completedStats}>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{totalQuestions}</Text>
          <Text style={styles.statLabel}>Toplam Soru</Text>
        </View>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{selectedAnswers.size}</Text>
          <Text style={styles.statLabel}>Cevaplanan</Text>
        </View>
      </View>

      <TouchableOpacity
        style={styles.finishButton}
        onPress={handleFinishTest}
        activeOpacity={0.8}
      >
        <Text style={styles.finishButtonText}>Testi Bitir</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Drawing Canvas - En üstte */}
        
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <View style={styles.headerInfo}>
            <Text style={styles.headerTitle}>{test.title}</Text>
            <Text style={styles.headerSubtitle}>
              {topic.title} • {book.title}
            </Text>
          </View>
          <View style={styles.placeholder} />
        </View>

        {/* Tool Buttons */}
        <View style={[styles.toolButtons, showDrawingCanvas && styles.expandedToolButtons]}>
          <View style={styles.mainToolButtons}>
            <TouchableOpacity style={styles.toolButton} activeOpacity={0.7}>
              <Ionicons name="bulb-outline" size={20} color="#f59e0b" />
              <Text style={styles.toolButtonText}>İpucu</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.toolButton} activeOpacity={0.7}>
              <Ionicons name="play-circle-outline" size={20} color="#3b82f6" />
              <Text style={styles.toolButtonText}>Çözüm Videosu</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[styles.toolButton, showDrawingCanvas && styles.activeToolButton]} 
              activeOpacity={0.7}
              onPress={handleDrawingToggle}
            >
              <Ionicons name="pencil-outline" size={20} color={showDrawingCanvas ? "#3b82f6" : "#10b981"} />
              <Text style={[styles.toolButtonText, showDrawingCanvas && styles.activeToolButtonText]}>
                Çizim
              </Text>
            </TouchableOpacity>
          </View>

          {showDrawingCanvas && (
            <View style={styles.drawingToolbar}>
              {/* Tool Selection */}
              <View style={styles.drawingSection}>
                <Text style={styles.sectionTitle}>Araç</Text>
                <View style={styles.toolRow}>
                  <TouchableOpacity 
                    style={[styles.drawingTool, currentTool === 'pen' && styles.activeTool]}
                    onPress={() => handleToolSelect('pen')}
                  >
                    <Ionicons name="pencil" size={18} color={currentTool === 'pen' ? "#3b82f6" : "#64748b"} />
                    <Text style={[styles.drawingToolText, currentTool === 'pen' && styles.activeToolText]}>Kalem</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={[styles.drawingTool, currentTool === 'line' && styles.activeTool]}
                    onPress={() => handleToolSelect('line')}
                  >
                    <Ionicons name="remove" size={18} color={currentTool === 'line' ? "#10b981" : "#64748b"} />
                    <Text style={[styles.drawingToolText, currentTool === 'line' && styles.activeToolText]}>Çizgi</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={[styles.drawingTool, currentTool === 'eraser' && styles.activeTool]}
                    onPress={() => handleToolSelect('eraser')}
                  >
                    <Ionicons name="trash" size={18} color={currentTool === 'eraser' ? "#ef4444" : "#64748b"} />
                    <Text style={[styles.drawingToolText, currentTool === 'eraser' && styles.activeToolText]}>Silgi</Text>
                  </TouchableOpacity>
                </View>
              </View>

              {/* Colors & Thickness */}
              <View style={styles.colorThicknessSection}>
                <View style={styles.colorSection}>
                  <Text style={styles.sectionTitle}>Renk</Text>
                  <View style={styles.colorRow}>
                    <TouchableOpacity 
                      style={[
                        styles.colorButton, 
                        { backgroundColor: '#000000' },
                        currentColor === '#000000' && styles.activeColorButton
                      ]}
                      onPress={() => handleColorSelect('#000000')}
                    />
                    <TouchableOpacity 
                      style={[
                        styles.colorButton, 
                        { backgroundColor: '#ff0000' },
                        currentColor === '#ff0000' && styles.activeColorButton
                      ]}
                      onPress={() => handleColorSelect('#ff0000')}
                    />
                    <TouchableOpacity 
                      style={[
                        styles.colorButton, 
                        { backgroundColor: '#0000ff' },
                        currentColor === '#0000ff' && styles.activeColorButton
                      ]}
                      onPress={() => handleColorSelect('#0000ff')}
                    />
                    <TouchableOpacity 
                      style={[
                        styles.colorButton, 
                        { backgroundColor: '#00ff00' },
                        currentColor === '#00ff00' && styles.activeColorButton
                      ]}
                      onPress={() => handleColorSelect('#00ff00')}
                    />
                  </View>
                </View>

                <View style={styles.thicknessSection}>
                  <Text style={styles.sectionTitle}>Kalınlık</Text>
                  <View style={styles.thicknessRow}>
                    <TouchableOpacity 
                      style={[styles.thicknessButton, currentThickness === 2 && styles.activeThickness]}
                      onPress={() => handleThicknessSelect(2)}
                    >
                      <View style={[styles.thicknessDot, { width: 2, height: 2 }]} />
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={[styles.thicknessButton, currentThickness === 4 && styles.activeThickness]}
                      onPress={() => handleThicknessSelect(4)}
                    >
                      <View style={[styles.thicknessDot, { width: 4, height: 4 }]} />
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={[styles.thicknessButton, currentThickness === 6 && styles.activeThickness]}
                      onPress={() => handleThicknessSelect(6)}
                    >
                      <View style={[styles.thicknessDot, { width: 6, height: 6 }]} />
                    </TouchableOpacity>
                  </View>
                </View>
              </View>

              {/* Actions */}
              <View style={styles.drawingSection}>
                <Text style={styles.sectionTitle}>İşlem</Text>
                <View style={styles.actionRow}>
                  <TouchableOpacity style={styles.actionButton} onPress={handleUndo}>
                    <Ionicons name="arrow-undo" size={16} color="#64748b" />
                    <Text style={styles.actionButtonText}>Geri</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.actionButton} onPress={handleClear}>
                    <Ionicons name="trash-outline" size={16} color="#ef4444" />
                    <Text style={styles.actionButtonText}>Temizle</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          )}
        </View>

        {/* Content */}
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {isTestCompleted ? renderTestCompleted() : renderQuestion()}
        </ScrollView>

        {/* Navigation Footer */}
        {!isTestCompleted && (
          <View style={styles.footer}>
            {currentQuestionIndex > 0 && (
              <TouchableOpacity
                style={styles.navButton}
                onPress={handlePreviousQuestion}
                activeOpacity={0.7}
              >
                <Ionicons name="chevron-back" size={20} color="#ffffff" />
                <Text style={styles.navButtonText}>Önceki</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity
              style={[
                styles.navButton,
                currentQuestionIndex === 0 && styles.singleButton
              ]}
              onPress={handleNextQuestion}
              activeOpacity={0.7}
            >
              <Text style={styles.navButtonText}>
                {currentQuestionIndex === totalQuestions - 1 ? 'Bitir' : 'Sonraki'}
              </Text>
              <Ionicons name="chevron-forward" size={20} color="#ffffff" />
            </TouchableOpacity>
          </View>
        )}
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
    marginRight: 12,
  },
  headerInfo: {
    flex: 1,
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 2,
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#64748b',
  },
  placeholder: {
    width: 40,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: screenWidth * 0.05, // 5% of screen width
    paddingBottom: screenWidth * 0.25, // 25% of screen width for footer
  },
  questionContainer: {
    backgroundColor: '#ffffff',
    borderRadius: screenWidth * 0.04, // 4% of screen width
    padding: screenWidth * 0.05,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    },
    shadowOpacity: 0.1,
    shadowRadius: screenWidth * 0.02, // 2% of screen width
    elevation: 4,
  },
  questionHeader: {
    marginBottom: screenWidth * 0.05,
  },
  questionNumber: {
    fontSize: Math.max(14, screenWidth * 0.04), // 4% of screen width, min 14px
    fontWeight: '600',
    color: '#374151',
    marginBottom: screenWidth * 0.03,
  },
  questionProgress: {
    height: Math.max(3, screenWidth * 0.01), // 1% of screen width, min 3px
    backgroundColor: '#e2e8f0',
    borderRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
  },
  questionImageContainer: {
    alignItems: 'center',
    marginBottom: screenWidth * 0.06,
  },
  questionImage: {
    width: '100%',
    height: screenWidth * 0.5, // 50% of screen width
    borderRadius: screenWidth * 0.03, // 3% of screen width
    backgroundColor: '#f1f5f9',
  },
  answerOptions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    marginTop: screenWidth * 0.05, // 5% of screen width
    paddingHorizontal: screenWidth * 0.05,
    width: '100%',
  },
  answerOption: {
    width: screenWidth * 0.12, // 12% of screen width
    height: screenWidth * 0.12,
    borderRadius: screenWidth * 0.06, // half of width/height
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    borderColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: Math.max(1, screenWidth * 0.002), // 0.2% of screen width, min 1px
    },
    shadowOpacity: 0.1,
    shadowRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    elevation: 1,
  },
  selectedAnswerOption: {
    backgroundColor: '#3b82f6',
    borderColor: '#3b82f6',
    shadowColor: '#3b82f6',
    shadowOpacity: 0.2,
  },
  answerLetterText: {
    fontSize: Math.max(16, screenWidth * 0.04), // 4% of screen width, min 16px
    fontWeight: 'bold',
    color: '#374151',
  },
  selectedAnswerLetterText: {
    color: '#ffffff',
  },
  completedContainer: {
    alignItems: 'center',
    padding: 40,
  },
  completedTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1e293b',
    marginTop: 16,
    marginBottom: 8,
  },
  completedSubtitle: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 24,
  },
  completedStats: {
    flexDirection: 'row',
    gap: 40,
    marginBottom: 32,
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#3b82f6',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 14,
    color: '#64748b',
  },
  finishButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 32,
    paddingVertical: 16,
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
  finishButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: -2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3b82f6',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: 100,
    justifyContent: 'center',
  },
  disabledButton: {
    backgroundColor: '#9ca3af',
  },
  singleButton: {
    flex: 1,
    justifyContent: 'center',
  },
  navButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
    marginHorizontal: 8,
  },
  toolButtons: {
    flexDirection: 'column',
    backgroundColor: '#ffffff',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
    minHeight: 60,
  },
  toolButton: {
    alignItems: 'center',
    flex: 1,
    paddingVertical: 8,
  },
  toolButtonText: {
    fontSize: 12,
    color: '#64748b',
    marginTop: 4,
  },
  activeToolButton: {
    backgroundColor: '#e0f2fe', // Light blue background for active tool
  },
  activeToolButtonText: {
    color: '#3b82f6', // Blue text for active tool
  },
  expandedToolButtons: {
    paddingBottom: 20,
  },
  mainToolButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: 20,
    paddingVertical: 15,
    backgroundColor: '#f8fafc',
    minHeight: 50,
  },
  drawingToolbar: {
    backgroundColor: '#f8fafc',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    maxWidth: screenWidth - 20,
  },
  drawingSection: {
    alignItems: 'center',
    marginHorizontal: 3,
    flex: 1,
  },
  colorThicknessSection: {
    alignItems: 'center',
    marginHorizontal: 3,
    flex: 1,
  },
  colorSection: {
    alignItems: 'center',
    marginBottom: 8,
  },
  thicknessSection: {
    alignItems: 'center',
  },
  sectionTitle: {
    fontSize: 10,
    color: '#64748b',
    fontWeight: '600',
    marginBottom: 4,
  },
  toolRow: {
    flexDirection: 'row',
    gap: 4,
  },
  drawingTool: {
    alignItems: 'center',
    padding: 4,
    borderRadius: 6,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    minWidth: 32,
    minHeight: 32,
  },
  drawingToolText: {
    fontSize: 8,
    color: '#64748b',
    marginTop: 2,
  },
  colorRow: {
    flexDirection: 'row',
    gap: 3,
  },
  colorButton: {
    width: 16,
    height: 16,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#e2e8f0',
  },
  thicknessRow: {
    flexDirection: 'row',
    gap: 4,
  },
  thicknessButton: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  thicknessDot: {
    backgroundColor: '#374151',
    borderRadius: 10,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 4,
  },
  actionButton: {
    alignItems: 'center',
    padding: 4,
    borderRadius: 6,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    minWidth: 32,
    minHeight: 32,
  },
  actionButtonText: {
    fontSize: 8,
    color: '#64748b',
    marginTop: 2,
  },
  activeTool: {
    backgroundColor: '#e0f2fe',
    borderColor: '#3b82f6',
    borderWidth: 2,
  },
  activeToolText: {
    color: '#3b82f6',
    fontWeight: '600',
  },
  activeColorButton: {
    borderColor: '#3b82f6',
    borderWidth: 3,
  },
  activeThickness: {
    backgroundColor: '#e0f2fe',
    borderColor: '#3b82f6',
    borderWidth: 2,
  },
});
