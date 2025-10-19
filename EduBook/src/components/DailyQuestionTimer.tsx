import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { DailyQuestion } from '../types';

interface DailyQuestionTimerProps {
  question: DailyQuestion | null;
  onTimeUpdate?: (timeRemaining: string) => void;
}

export const DailyQuestionTimer: React.FC<DailyQuestionTimerProps> = ({ 
  question, 
  onTimeUpdate 
}) => {
  const [timeRemaining, setTimeRemaining] = useState<string>('');

  // Kalan süreyi hesapla
  const updateTimeRemaining = useCallback(() => {
    if (!question?.startTime || !question?.endTime) {
      setTimeRemaining('');
      onTimeUpdate?.('');
      return;
    }

    const now = new Date();
    const start = new Date(question.displayDate + 'T' + question.startTime);
    const end = new Date(question.displayDate + 'T' + question.endTime);

    // Eğer başlangıç zamanından önceyse
    if (now < start) {
      const diff = start.getTime() - now.getTime();
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      const timeText = `Başlamasına ${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')} kaldı`;
      setTimeRemaining(timeText);
      onTimeUpdate?.(timeText);
      return;
    }

    // Eğer bitiş zamanından sonraysa
    if (now > end) {
      const timeText = 'Süre doldu';
      setTimeRemaining(timeText);
      onTimeUpdate?.(timeText);
      return;
    }

    // Soru aktif
    const diff = end.getTime() - now.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    const timeText = `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    setTimeRemaining(timeText);
    onTimeUpdate?.(timeText);
  }, [question, onTimeUpdate]);

  useEffect(() => {
    if (!question) return;
    
    // İlk güncelleme
    updateTimeRemaining();
    
    // Her saniye güncelle
    const timer = setInterval(updateTimeRemaining, 1000);
    return () => clearInterval(timer);
  }, [updateTimeRemaining, question]);

  if (!question || !timeRemaining) {
    return null;
  }

  return (
    <View style={styles.timerContainer}>
      <Text style={styles.timerLabel}>Kalan Süre</Text>
      <Text style={styles.timer}>{timeRemaining}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  timerContainer: {
    alignItems: 'flex-end',
    marginLeft: 12,
  },
  timerLabel: {
    fontSize: 12,
    color: '#64748b',
    marginBottom: 4,
  },
  timer: {
    fontSize: 16,
    fontWeight: '700',
    color: '#e53935',
  },
});
