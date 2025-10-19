import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { OptionLetter } from '../types';

const { width: screenWidth } = Dimensions.get('window');

interface AnswerOptionsProps {
  answerNumber: number;
  selectedAnswer: OptionLetter | null;
  correctAnswer?: OptionLetter | null;
  onAnswerSelect: (answer: OptionLetter) => void;
  disabled?: boolean;
}

export const AnswerOptions: React.FC<AnswerOptionsProps> = ({
  answerNumber,
  selectedAnswer,
  correctAnswer,
  onAnswerSelect,
  disabled = false,
}) => {
  const options: OptionLetter[] = ['A', 'B', 'C', 'D', 'E'];
  const availableOptions = options.slice(0, answerNumber);

  return (
    <View style={styles.answerOptions}>
      {availableOptions.map((letter) => (
        <TouchableOpacity
          key={letter}
          style={[
            styles.answerOption,
            selectedAnswer === letter && styles.selectedAnswerOption,
            correctAnswer === letter && styles.correctAnswerOption,
            disabled && styles.disabledAnswerOption,
          ]}
          onPress={() => !disabled && onAnswerSelect(letter)}
          activeOpacity={disabled ? 1 : 0.7}
          disabled={disabled}
        >
          <Text style={[
            styles.answerLetterText,
            selectedAnswer === letter && styles.selectedAnswerLetterText,
            disabled && styles.disabledAnswerLetterText,
          ]}>
            {letter}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
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
  disabledAnswerOption: {
    backgroundColor: '#f1f5f9',
    borderColor: '#cbd5e1',
    opacity: 0.6,
  },
  answerLetterText: {
    fontSize: Math.max(16, screenWidth * 0.04),
    fontWeight: 'bold',
    color: '#374151',
  },
  selectedAnswerLetterText: {
    color: '#ffffff',
  },
  disabledAnswerLetterText: {
    color: '#94a3b8',
  },
});
