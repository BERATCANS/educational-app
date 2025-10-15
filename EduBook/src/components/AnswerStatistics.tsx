import React from 'react';
import { View, Text, StyleSheet, Dimensions } from 'react-native';
import { OptionLetter } from '../types';

const { width } = Dimensions.get('window');
const BAR_WIDTH = width - 64; // 32px padding on each side

type Props = {
  distribution: Record<OptionLetter, number>;
  totalAnswers: number;
  correctAnswer: OptionLetter;
  userAnswer?: OptionLetter;
  answerNumber: number;
};

export default function AnswerStatistics({ distribution, totalAnswers, correctAnswer, userAnswer, answerNumber }: Props) {
  const maxCount = Math.max(...Object.values(distribution || {}));

  const getBarColor = (option: OptionLetter) => {
    if (option === correctAnswer) return '#4caf50';
    if (option === userAnswer && userAnswer !== correctAnswer) return '#f44336';
    return '#e0e0e0';
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Cevap Dağılımı</Text>
        <Text style={styles.totalAnswers}>Toplam: {totalAnswers}</Text>
      </View>

      {(['A', 'B', 'C', 'D', 'E'] as OptionLetter[]).slice(0, answerNumber).map((option) => {
        console.log('🔍 Option:', option, 'Count:', distribution?.[option], 'Distribution:', distribution);
        const count = distribution?.[option] || 0;
        const percentage = totalAnswers > 0 ? Math.round((count / totalAnswers) * 100) : 0;
        const barWidth = maxCount > 0 ? (count / maxCount) * BAR_WIDTH : 0;

        return (
          <View key={option} style={styles.row}>
            <View style={styles.optionContainer}>
              <Text style={styles.option}>{option}</Text>
              <Text style={styles.count}>{count}</Text>
            </View>
            <View style={styles.barContainer}>
              <View style={styles.barWrapper}>
                <View
                  style={[
                    styles.bar,
                    {
                      width: barWidth,
                      backgroundColor: getBarColor(option),
                    },
                  ]}
                />
                <Text style={styles.percentage}>{percentage}%</Text>
              </View>
            </View>
          </View>
        );
      })}

      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendColor, { backgroundColor: '#4caf50' }]} />
          <Text style={styles.legendText}>Doğru Cevap</Text>
        </View>
        {userAnswer && userAnswer !== correctAnswer && (
          <View style={styles.legendItem}>
            <View style={[styles.legendColor, { backgroundColor: '#f44336' }]} />
            <Text style={styles.legendText}>Senin Cevabın</Text>
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
  },
  totalAnswers: {
    fontSize: 14,
    color: '#666',
  },
  row: {
    marginBottom: 12,
  },
  optionContainer: {
    flexDirection: 'row',
    marginBottom: 4,
  },
  option: {
    fontSize: 16,
    fontWeight: '600',
    width: 30,
  },
  count: {
    fontSize: 16,
    color: '#666',
    marginLeft: 8,
  },
  barContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  barWrapper: {
    position: 'relative',
    flex: 1,
  },
  bar: {
    height: 20,
    borderRadius: 4,
  },
  percentage: {
    position: 'absolute',
    top: -20,
    right: 0,
    fontSize: 12,
    color: '#666',
    fontWeight: '600',
  },
  legend: {
    flexDirection: 'row',
    marginTop: 16,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 16,
  },
  legendColor: {
    width: 16,
    height: 16,
    borderRadius: 4,
    marginRight: 8,
  },
  legendText: {
    fontSize: 14,
    color: '#666',
  },
});
