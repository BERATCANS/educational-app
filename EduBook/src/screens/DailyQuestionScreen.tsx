import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Dimensions,
  Modal,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiService from '../services/api';
import { DailyQuestion, OptionLetter, DailyQuestionAnswerRequest } from '../types';
import { useAuthStore } from '../store/authStore';
import * as Localization from 'expo-localization';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import AnswerStatistics from '../components/AnswerStatistics';
import { SimpleDrawing } from '../components/SimpleDrawing';

const { width: screenWidth } = Dimensions.get('window');

type RootStackParamList = {
  DailyQuestion: undefined;
};

type DailyQuestionScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'DailyQuestion'>;

export const DailyQuestionScreen: React.FC = () => {
  const navigation = useNavigation<DailyQuestionScreenNavigationProp>();
  const queryClient = useQueryClient();
  const userId = useAuthStore(state => state.user?.id) || 1;
  const [selectedAnswer, setSelectedAnswer] = useState<OptionLetter | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const [showDrawingCanvas, setShowDrawingCanvas] = useState(false);
  const [showDrawingToolbar, setShowDrawingToolbar] = useState(true);
  const [currentTool, setCurrentTool] = useState<'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter'>('pen');
  const [currentColor, setCurrentColor] = useState('#000000');
  const [currentThickness, setCurrentThickness] = useState(2);
  
  // Ayrı renk ve kalınlık ayarları
  const [penColor, setPenColor] = useState('#000000');
  const [penThickness, setPenThickness] = useState(2);
  const [highlighterColor, setHighlighterColor] = useState('#ffff00');
  const [highlighterThickness, setHighlighterThickness] = useState(8);
  const [showColorPalette, setShowColorPalette] = useState(false);
  const [showThicknessPalette, setShowThicknessPalette] = useState(false);
  const [showShapePalette, setShowShapePalette] = useState(false);
  const [isDrawingShape, setIsDrawingShape] = useState(false);
  const [shapeStartPoint, setShapeStartPoint] = useState<{x: number, y: number} | null>(null);
  const [drawingsByQuestion, setDrawingsByQuestion] = useState<{[questionId: number]: Array<{id: string, path: string, color: string, thickness: number}>}>({});

  // Günün sorusunu getir
  const { data: question, isLoading, error } = useQuery({
    queryKey: ['dailyQuestion'],
    queryFn: () => apiService.getCurrentDailyQuestion(userId),
    refetchInterval: 60000, // Her dakika güncelle
  });

  // Debug: Question response'unu logla
  useEffect(() => {
    console.log('🔍 Question state:', {
      question: question,
      isLoading: isLoading,
      error: error,
      hasQuestion: !!question,
      hasImageUrl: question?.imageUrl,
      imageUrlValue: question?.imageUrl
    });
  }, [question, isLoading, error]);

  // Cevap gönderme mutasyonu
  const submitAnswer = useMutation<DailyQuestion, Error, OptionLetter, unknown>({
    mutationFn: (answer: OptionLetter) => {
      const request: DailyQuestionAnswerRequest = {
        dailyQuestionId: question!.id,
        selectedAnswer: answer
      };
      return apiService.submitDailyQuestionAnswer(userId, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dailyQuestion'] });
    },
  });

  // Kalan süreyi hesapla
  const updateTimeRemaining = useCallback(() => {
    if (!question?.startTime || !question?.endTime) return;

    const now = new Date();
    const start = new Date(question.displayDate + 'T' + question.startTime);
    const end = new Date(question.displayDate + 'T' + question.endTime);

    // Eğer başlangıç zamanından önceyse
    if (now < start) {
      const diff = start.getTime() - now.getTime();
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      setTimeRemaining(`Başlamasına ${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')} kaldı`);
      return;
    }

    // Eğer bitiş zamanından sonraysa
    if (now > end) {
      setTimeRemaining('Süre doldu');
      return;
    }

    // Soru aktif
    const diff = end.getTime() - now.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    setTimeRemaining(`${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`);
  }, [question]);

  useEffect(() => {
    if (!question) return;
    const timer = setInterval(updateTimeRemaining, 1000);
    return () => clearInterval(timer);
  }, [updateTimeRemaining, question]);

  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <ActivityIndicator size="large" color="#0000ff" />
        </View>
      </SafeAreaView>
    );
  }

  if (error) {
    const errorMessage = error && typeof error === 'object' && 'message' in error
      ? String(error.message)
      : 'Bilinmeyen bir hata oluştu';

    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <Text style={styles.errorText}>{errorMessage}</Text>
          <TouchableOpacity 
            style={styles.retryButton}
            onPress={() => queryClient.invalidateQueries({ queryKey: ['dailyQuestion'] })}
          >
            <Text style={styles.retryButtonText}>Tekrar Dene</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  if (!question || !question.id) {
    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <Text style={styles.noQuestionText}>
            {question?.errorMessage || 'Bugün için soru bulunmuyor.'}
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  const handleAnswerSelect = (answer: OptionLetter) => {
    if (question.hasUserAnswered) return;
    setSelectedAnswer(answer);
  };

  const handleSubmit = () => {
    if (!selectedAnswer || question.hasUserAnswered) return;
    submitAnswer.mutate(selectedAnswer);
  };

  // Çizim fonksiyonları
  const handleDrawingToggle = () => {
    if (!showDrawingCanvas) {
      // Çizim modu açılırken toolbar'ı gizli başlat
      setShowDrawingToolbar(false);
    }
    setShowDrawingCanvas(!showDrawingCanvas);
  };

  const handleToolSelect = (tool: 'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter') => {
    setCurrentTool(tool);
    
    // Tool değiştiğinde kendi renk ve kalınlığına geç
    if (tool === 'pen') {
      setCurrentColor(penColor);
      setCurrentThickness(penThickness);
    } else if (tool === 'highlighter') {
      setCurrentColor(highlighterColor);
      setCurrentThickness(highlighterThickness);
    }
  };

  const handleColorSelect = (color: string) => {
    setCurrentColor(color);
    setShowColorPalette(false); // Renk seçildikten sonra paleti kapat
    
    // Tool'a göre ayrı state'leri güncelle
    if (currentTool === 'pen') {
      setPenColor(color);
    } else if (currentTool === 'highlighter') {
      setHighlighterColor(color);
    }
    
    // Line modundaysa line'da kal, değilse pen'e geç
    if (currentTool === 'eraser') {
      setCurrentTool('pen');
    }
  };

  const handleThicknessSelect = (thickness: number) => {
    setCurrentThickness(thickness);
    setShowThicknessPalette(false); // Kalınlık seçildikten sonra paleti kapat
    
    // Tool'a göre ayrı state'leri güncelle
    if (currentTool === 'pen') {
      setPenThickness(thickness);
    } else if (currentTool === 'highlighter') {
      setHighlighterThickness(thickness);
    }
  };

  const handleShapeSelect = (shape: 'line' | 'triangle' | 'circle' | 'rectangle' | 'square') => {
    setCurrentTool(shape);
    setShowShapePalette(false);
  };

  const handleShapeStart = (x: number, y: number) => {
    setShapeStartPoint({ x, y });
    setIsDrawingShape(true);
  };

  const handleShapeEnd = (x: number, y: number) => {
    if (!shapeStartPoint || !question) return;
    
    const shapePath = generateShapePath(shapeStartPoint.x, shapeStartPoint.y, x, y, currentTool);
    if (shapePath) {
      const newPathObj = {
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        path: shapePath,
        color: currentColor,
        thickness: currentThickness,
        tool: currentTool
      };
      
      const newDrawings = {
        ...drawingsByQuestion,
        [question.id]: [...(drawingsByQuestion[question.id] || []), newPathObj]
      };
      
      setDrawingsByQuestion(newDrawings);
    }
    
    setShapeStartPoint(null);
    setIsDrawingShape(false);
  };

  const handleUndo = () => {
    if (!question) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [question.id]: (drawingsByQuestion[question.id] || []).slice(0, -1)
    };
    
    setDrawingsByQuestion(newDrawings);
  };

  const handleClear = () => {
    if (!question) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [question.id]: []
    };
    
    setDrawingsByQuestion(newDrawings);
  };

  const handlePathsChange = (newPaths: any[]) => {
    if (!question) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [question.id]: newPaths
    }));
  };


  // Şekil çizme fonksiyonları
  const generateShapePath = (startX: number, startY: number, endX: number, endY: number, shapeType: string): string => {
    const width = Math.abs(endX - startX);
    const height = Math.abs(endY - startY);
    const minX = Math.min(startX, endX);
    const minY = Math.min(startY, endY);

    switch (shapeType) {
      case 'triangle':
        const centerX = minX + width / 2;
        const topY = minY;
        const leftX = minX;
        const rightX = minX + width;
        const bottomY = minY + height;
        return `M${centerX},${topY} L${rightX},${bottomY} L${leftX},${bottomY} Z`;
      
      case 'circle':
        // Merkez: başlangıç noktası (startX, startY)
        // Yarıçap: başlangıç noktasından bitiş noktasına olan mesafe
        const radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        const centerX_circle = startX;
        const centerY_circle = startY;
        return `M${centerX_circle - radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle + radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle - radius},${centerY_circle}`;
      
      case 'rectangle':
        return `M${minX},${minY} L${minX + width},${minY} L${minX + width},${minY + height} L${minX},${minY + height} Z`;
      
      case 'square':
        const size = Math.min(width, height);
        return `M${minX},${minY} L${minX + size},${minY} L${minX + size},${minY + size} L${minX},${minY + size} Z`;
      
      default:
        return '';
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      {/* Combined Header & Tools */}
      <View style={styles.headerToolsContainer}>
        {/* Header Section */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <View style={styles.headerInfo}>
            <Text style={styles.headerTitle}>Günün Sorusu</Text>
            <Text style={styles.date}>
              {new Date(question.displayDate + 'T00:00:00').toLocaleDateString(Localization.getLocales()[0].languageTag, {
                day: 'numeric',
                month: 'long',
                weekday: 'long'
              })}
            </Text>
          </View>

          {/* Timer Section */}
          <View style={styles.timerContainer}>
            <Text style={styles.timerLabel}>Kalan Süre</Text>
            <Text style={styles.timer}>{timeRemaining}</Text>
          </View>
        </View>

        {/* Tool Buttons Section */}
        <View style={styles.toolButtons}>
          <View style={styles.mainToolButtons}>
            <TouchableOpacity 
              style={[styles.toolButton, showDrawingCanvas && styles.activeToolButton]} 
              activeOpacity={0.7}
              onPress={handleDrawingToggle}
            >
              <Ionicons name="pencil-outline" size={14} color={showDrawingCanvas ? "#3b82f6" : "#10b981"} />
              <Text style={[styles.toolButtonText, showDrawingCanvas && styles.activeToolButtonText]}>
                Çizim
              </Text>
            </TouchableOpacity>
          </View>

          {/* Drawing Tools */}
          {showDrawingCanvas && (
            <View style={styles.drawingContainer}>
              {/* Compact Drawing Toolbar */}
              <View style={[styles.compactDrawingToolbar, !showDrawingToolbar && styles.hiddenToolbar]}>
                {/* Sol Grup - Araçlar */}
                <View style={styles.toolbarGroup}>
                  <TouchableOpacity 
                    style={[styles.compactTool, currentTool === 'pen' && styles.activeCompactTool]}
                    onPress={() => handleToolSelect('pen')}
                  >
                    <Ionicons name="pencil" size={18} color={currentTool === 'pen' ? "#3b82f6" : "#64748b"} />
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={[styles.compactTool, currentTool === 'highlighter' && styles.activeCompactTool]}
                    onPress={() => handleToolSelect('highlighter')}
                  >
                    <Ionicons name="brush" size={18} color={currentTool === 'highlighter' ? "#f59e0b" : "#64748b"} />
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={[styles.compactTool, currentTool === 'eraser' && styles.activeCompactTool]}
                    onPress={() => handleToolSelect('eraser')}
                  >
                    <Ionicons name="remove-circle" size={18} color={currentTool === 'eraser' ? "#ef4444" : "#64748b"} />
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={[styles.compactTool, ['line', 'triangle', 'circle', 'rectangle', 'square'].includes(currentTool) && styles.activeCompactTool]}
                    onPress={() => setShowShapePalette(true)}
                  >
                    <Ionicons name="shapes" size={18} color={['line', 'triangle', 'circle', 'rectangle', 'square'].includes(currentTool) ? "#3b82f6" : "#64748b"} />
                  </TouchableOpacity>
                </View>

                {/* Orta Grup - Renk ve Kalınlık */}
                <View style={styles.toolbarGroup}>
                  <TouchableOpacity 
                    style={[
                      styles.compactColorButton, 
                      { backgroundColor: currentColor },
                      currentTool === 'highlighter' && styles.highlighterButton
                    ]}
                    onPress={() => setShowColorPalette(true)}
                  />

                  <TouchableOpacity 
                    style={styles.compactThicknessButton}
                    onPress={() => setShowThicknessPalette(true)}
                  >
                    <View style={[styles.compactThicknessDot, { 
                      width: Math.min(currentThickness * 1.5, 12), 
                      height: Math.min(currentThickness * 1.5, 12) 
                    }]} />
                  </TouchableOpacity>
                </View>

                {/* Sağ Grup - Aksiyonlar */}
                <View style={styles.toolbarGroup}>
                  <TouchableOpacity 
                    style={styles.compactActionButton}
                    onPress={handleUndo}
                  >
                    <Ionicons name="arrow-undo" size={18} color="#64748b" />
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={styles.compactActionButton}
                    onPress={handleClear}
                  >
                    <Ionicons name="trash-outline" size={18} color="#ef4444" />
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          )}
        </View>
      </View>

      {/* Main Content */}
      <ScrollView 
        style={styles.scrollContent}
        contentContainerStyle={styles.scrollContentContainer}
      >
        {/* Question Header with Toolbar Toggle */}
        {showDrawingCanvas && (
          <View style={styles.questionHeader}>
            <View style={styles.questionHeaderTop}>
              <View style={styles.placeholder} />
              <TouchableOpacity 
                style={styles.toolbarToggle}
                onPress={() => setShowDrawingToolbar(!showDrawingToolbar)}
              >
                <Ionicons 
                  name={showDrawingToolbar ? "chevron-up" : "chevron-down"} 
                  size={14} 
                  color="#64748b" 
                />
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* Soru Resmi - Sadece soru aktifken görünür */}
        {question && question.imageUrl && 
         new Date() >= new Date(question.displayDate + 'T' + question.startTime) && (
          <View style={styles.questionImageContainer}>
            <Image
              source={{ uri: question.imageUrl }}
              style={styles.questionImage}
              resizeMode="contain"
            />
            
            {/* Drawing Component Overlay */}
            <SimpleDrawing
              isActive={showDrawingCanvas}
              currentColor={currentColor}
              currentThickness={currentThickness}
              currentTool={currentTool}
              paths={drawingsByQuestion[question.id] || []}
              onPathsChange={handlePathsChange}
              onShapeStart={handleShapeStart}
              onShapeEnd={handleShapeEnd}
            />
          </View>
        )}

        {/* Soru başlamadan önce - Gün değişiminden sonra */}
        {new Date() < new Date(question.displayDate + 'T' + question.startTime) && (
          <View style={styles.waitingContainer}>
            <Text style={styles.waitingText}>
              Soru henüz başlamadı. Başlangıç saatinde tekrar gelin!
            </Text>
          </View>
        )}

        {/* Soru bitti - Gün sonuna kadar istatistikler */}
        {new Date() > new Date(question.displayDate + 'T' + question.endTime) && 
         new Date() < new Date(question.displayDate + 'T23:59:59') && (
          <View style={styles.resultContainer}>
            {/* Kullanıcının cevabı */}
            {question.hasUserAnswered && (
              <>
                <Text style={styles.resultTitle}>
                  {question.userAnswer?.selectedAnswer === question.afterEndTime?.correctAnswer
                    ? '✅ Doğru Cevap!'
                    : '❌ Yanlış Cevap'}
                </Text>
                <Text style={styles.resultText}>
                  {`Senin cevabın: ${question.userAnswer?.selectedAnswer}`}
                </Text>
                <Text style={styles.resultText}>
                  {`Doğru cevap: ${question.afterEndTime?.correctAnswer || 'bekleniyor...'}`}
                </Text>
              </>
            )}

            {/* Açıklama */}
            <Text style={styles.explanationTitle}>Açıklama</Text>
            <Text style={styles.explanationText}>{question.afterEndTime?.explanation}</Text>

            {/* İstatistikler */}
            <Text style={styles.statsTitle}>İstatistikler</Text>
            {(() => {
              console.log('📊 İstatistikler:', {
                distribution: question.afterEndTime?.statistics.distribution,
                totalAnswers: question.afterEndTime?.statistics.totalAnswers,
                correctAnswer: question.afterEndTime?.correctAnswer,
                userAnswer: question.userAnswer?.selectedAnswer,
                answerNumber: question.answerNumber
              });
              return null;
            })()}
            <AnswerStatistics
              distribution={question.afterEndTime?.statistics.distribution || { A: 0, B: 0, C: 0, D: 0, E: 0 }}
              totalAnswers={question.afterEndTime?.statistics.totalAnswers || 0}
              correctAnswer={question.afterEndTime?.correctAnswer || 'A'}
              userAnswer={question.userAnswer?.selectedAnswer}
              answerNumber={question.answerNumber}
            />

            {/* Video */}
            {question.videoUrl && (
              <TouchableOpacity style={styles.videoButton}>
                <MaterialCommunityIcons name="video" size={24} color="#fff" />
                <Text style={styles.videoButtonText}>Çözüm Videosunu İzle</Text>
              </TouchableOpacity>
            )}
          </View>
        )}
      </ScrollView>

      {/* Soru aktif */}
        {new Date() >= new Date(question.displayDate + 'T' + question.startTime) && 
         new Date() <= new Date(question.displayDate + 'T' + question.endTime) && (
          <>
            {/* Combined Answer Options & Navigation */}
            {!question.hasUserAnswered && (
              <View style={styles.combinedFooter}>
                {/* Answer Options */}
                <View style={styles.answerOptions}>
                  {(['A', 'B', 'C', 'D', 'E'] as OptionLetter[]).slice(0, question.answerNumber).map((letter) => (
                    <TouchableOpacity
                      key={letter}
                      style={[
                        styles.answerOption,
                        selectedAnswer === letter && styles.selectedAnswerOption,
                        question.afterEndTime?.correctAnswer === letter && styles.correctAnswerOption
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

                {/* Submit Button */}
                <TouchableOpacity
                  style={[
                    styles.navButton,
                    (!selectedAnswer || submitAnswer.status === 'pending') && styles.submitButtonDisabled
                  ]}
                  onPress={handleSubmit}
                  disabled={!selectedAnswer || submitAnswer.status === 'pending'}
                  activeOpacity={0.7}
                >
                  {submitAnswer.status === 'pending' ? (
                    <ActivityIndicator color="#fff" />
                  ) : (
                    <>
                      <Text style={styles.navButtonText}>Cevapla</Text>
                      <Ionicons name="chevron-forward" size={20} color="#ffffff" />
                    </>
                  )}
                </TouchableOpacity>
              </View>
            )}
          </>
        )}


      {/* Color Palette Modal */}
      <Modal
        visible={showColorPalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowColorPalette(false)}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowColorPalette(false)}
        >
          <View style={styles.colorPalette}>
            <Text style={styles.paletteTitle}>Renk Seç</Text>
            <View style={styles.colorGrid}>
              {['#000000', '#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF', '#FFA500', '#800080', '#FFC0CB', '#A52A2A', '#808080'].map((color) => (
                <TouchableOpacity
                  key={color}
                  style={[styles.colorOption, { backgroundColor: color }]}
                  onPress={() => handleColorSelect(color)}
                />
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Thickness Palette Modal */}
      <Modal
        visible={showThicknessPalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowThicknessPalette(false)}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowThicknessPalette(false)}
        >
          <View style={styles.thicknessPalette}>
            <Text style={styles.paletteTitle}>Kalınlık Seç</Text>
            <View style={styles.thicknessGrid}>
              {[1, 2, 4, 6, 8, 10, 12, 16].map((thickness) => (
                <TouchableOpacity
                  key={thickness}
                  style={styles.thicknessOption}
                  onPress={() => handleThicknessSelect(thickness)}
                >
                  <View style={[styles.thicknessLine, { 
                    height: thickness, 
                    backgroundColor: currentColor 
                  }]} />
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Shape Palette Modal */}
      <Modal
        visible={showShapePalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowShapePalette(false)}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowShapePalette(false)}
        >
          <View style={styles.shapePalette}>
            <Text style={styles.paletteTitle}>Şekil Seç</Text>
            <View style={styles.shapeGrid}>
              {[
                { type: 'line', icon: 'remove' },
                { type: 'triangle', icon: 'triangle' },
                { type: 'circle', icon: 'ellipse' },
                { type: 'rectangle', icon: 'square' },
                { type: 'square', icon: 'stop' }
              ].map((shape) => (
                <TouchableOpacity
                  key={shape.type}
                  style={[styles.shapeOption, currentTool === shape.type && styles.activeShapeOption]}
                  onPress={() => handleShapeSelect(shape.type as any)}
                >
                  <Ionicons 
                    name={shape.icon as any} 
                    size={24} 
                    color={currentTool === shape.type ? "#3b82f6" : "#64748b"} 
                  />
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  waitingContainer: {
    position: 'absolute',
    top: '50%',
    left: 16,
    right: 16,
    transform: [{ translateY: -50 }],
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    padding: 16,
    borderRadius: 8,
  },
  waitingText: {
    fontSize: 16,
    textAlign: 'center',
    color: '#666',
  },
  resultTitle: {
    fontSize: 24,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 16,
  },
  resultText: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 8,
    color: '#666',
  },
  statsTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 24,
    marginBottom: 16,
  },
  safeArea: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  mainContent: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollContent: {
    flex: 1,
  },
  scrollContentContainer: {
    paddingBottom: 100,
    flexGrow: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
  },
  timerContainer: {
    alignItems: 'flex-end',
    marginLeft: 12,
  },
  timerLabel: {
    fontSize: 12,
    color: '#64748b',
    marginBottom: 4,
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
  },
  questionHeader: {
    marginBottom: 8,
  },
  questionHeaderTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  questionNumber: {
    fontSize: Math.max(14, screenWidth * 0.04),
    fontWeight: '600',
    color: '#374151',
    marginTop: 10,
    marginBottom: 0,
  },
  questionProgress: {
    height: Math.max(3, screenWidth * 0.01),
    backgroundColor: '#e2e8f0',
    borderRadius: Math.max(1, screenWidth * 0.005),
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: Math.max(1, screenWidth * 0.005),
  },
  toolbarToggle: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 12,
    padding: 0,
    minWidth: 28,
    minHeight: 28,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerToolsContainer: {
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  toolButtons: {
    flexDirection: 'column',
    backgroundColor: '#ffffff',
  },
  toolButton: {
    alignItems: 'center',
    flex: 1,
    paddingVertical: 4,
  },
  toolButtonText: {
    fontSize: 12,
    color: '#64748b',
    marginTop: 4,
  },
  activeToolButton: {
    backgroundColor: '#e0f2fe',
  },
  activeToolButtonText: {
    color: '#3b82f6',
  },
  mainToolButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: '#ffffff',
    minHeight: 32,
  },
  drawingContainer: {
    backgroundColor: '#ffffff',
  },
  compactDrawingToolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 8,
    paddingVertical: 6,
    backgroundColor: '#f8fafc',
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    flexWrap: 'nowrap',
  },
  hiddenToolbar: {
    height: 0,
    paddingVertical: 0,
    borderTopWidth: 0,
    opacity: 0,
  },
  toolbarGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  compactTool: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  activeCompactTool: {
    borderColor: '#3b82f6',
    backgroundColor: '#e0f2fe',
  },
  compactColorButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  compactThicknessButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  compactThicknessDot: {
    backgroundColor: '#374151',
    borderRadius: 10,
  },
  compactActionButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  highlighterButton: {
    borderWidth: 2,
    borderColor: '#f59e0b',
    borderRadius: 4,
  },
  date: {
    fontSize: 12,
    color: '#64748b',
  },
  timer: {
    fontSize: 16,
    fontWeight: '700',
    color: '#e53935',
  },
  questionImageContainer: {
    position: 'relative',
    width: '100%',
    minHeight: 300,
    alignItems: 'center',
    marginBottom: 12,
    backgroundColor: '#ffffff',
    borderRadius: 8,
  },
  questionImage: {
    width: '100%',
    minHeight: screenWidth * 1.2,
    borderRadius: 8,
    backgroundColor: '#ffffff',
  },
  answersContainer: {
    padding: 16,
  },
  combinedFooter: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#ffffff',
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 10,
    zIndex: 5,
  },
  answerOptions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    width: '100%',
    marginBottom: 16,
  },
  answerOption: {
    width: screenWidth * 0.12,
    height: screenWidth * 0.12,
    borderRadius: screenWidth * 0.06,
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: Math.max(1, screenWidth * 0.005),
    borderColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: Math.max(1, screenWidth * 0.002),
    },
    shadowOpacity: 0.1,
    shadowRadius: Math.max(1, screenWidth * 0.005),
    elevation: 1,
  },
  selectedAnswerOption: {
    backgroundColor: '#3b82f6',
    borderColor: '#3b82f6',
    shadowColor: '#3b82f6',
    shadowOpacity: 0.2,
  },
  correctAnswerOption: {
    borderColor: '#22c55e',
    borderWidth: 5,
    shadowColor: '#22c55e',
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3,
  },
  answerLetterText: {
    fontSize: Math.max(16, screenWidth * 0.04),
    fontWeight: 'bold',
    color: '#374151',
  },
  selectedAnswerLetterText: {
    color: '#ffffff',
  },
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3b82f6',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    justifyContent: 'center',
  },
  navButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
    marginHorizontal: 8,
  },
  submitButtonDisabled: {
    backgroundColor: '#bdbdbd',
  },
  resultContainer: {
    padding: 16,
  },
  explanationTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
  },
  explanationText: {
    fontSize: 16,
    lineHeight: 24,
    marginBottom: 16,
  },
  statsText: {
    fontSize: 16,
    marginBottom: 8,
  },
  distributionContainer: {
    marginTop: 8,
  },
  distributionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  distributionOption: {
    width: 30,
    fontWeight: '600',
  },
  distributionCount: {
    marginHorizontal: 8,
  },
  distributionPercentage: {
    color: '#666',
  },
  videoButton: {
    backgroundColor: '#f44336',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    borderRadius: 8,
    margin: 16,
  },
  videoButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  errorText: {
    color: '#f44336',
    fontSize: 16,
    textAlign: 'center',
    margin: 16,
  },
  noQuestionText: {
    fontSize: 16,
    textAlign: 'center',
    margin: 16,
  },
  retryButton: {
    backgroundColor: '#2196f3',
    padding: 12,
    borderRadius: 8,
    marginTop: 16,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  colorPalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  thicknessPalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  shapePalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  paletteTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 16,
    textAlign: 'center',
  },
  colorGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  colorOption: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  thicknessGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  thicknessOption: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  thicknessLine: {
    width: 30,
    borderRadius: 2,
  },
  shapeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  shapeOption: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  activeShapeOption: {
    borderColor: '#3b82f6',
    backgroundColor: '#e0f2fe',
  },
  placeholder: {
    flex: 1,
  },
});
