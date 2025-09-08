import React, { useState } from 'react';
import { View, Text, StyleSheet, Alert } from 'react-native';
import { Input } from './Input';
import { Button } from './Button';
import { bookService } from '../services/bookService';

interface BookCodeInputProps {
  onCodeUsed: (message: string) => void;
  loading?: boolean;
}

export const BookCodeInput: React.FC<BookCodeInputProps> = ({ 
  onCodeUsed, 
  loading = false 
}) => {
  const [code, setCode] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!code.trim()) {
      Alert.alert('Error', 'Please enter a book code');
      return;
    }

    // Validate code format
    const validation = bookService.validateBookCode(code);
    if (!validation.isValid) {
      Alert.alert('Invalid Code', validation.message || 'Please enter a valid book code');
      return;
    }

    setIsSubmitting(true);

    try {
      // This will be handled by the parent component
      onCodeUsed(code);
    } catch (error) {
      console.error('Error using book code:', error);
      Alert.alert('Error', 'Failed to use book code. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClear = () => {
    setCode('');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Enter Book Code</Text>
      <Text style={styles.description}>
        Enter the code from your book to unlock access to digital content.
      </Text>
      
      <View style={styles.inputContainer}>
        <Input
          placeholder="Enter book code (e.g., ABC123-XYZ789)"
          value={code}
          onChangeText={setCode}
          autoCapitalize="characters"
          autoCorrect={false}
          editable={!loading && !isSubmitting}
          style={styles.input}
        />
      </View>

      <View style={styles.buttonContainer}>
        <Button
          title="Clear"
          onPress={handleClear}
          variant="secondary"
          disabled={loading || isSubmitting || !code.trim()}
          style={[styles.button, styles.clearButton]}
        />
        <Button
          title={isSubmitting ? "Using Code..." : "Use Code"}
          onPress={handleSubmit}
          disabled={loading || isSubmitting || !code.trim()}
          style={[styles.button, styles.submitButton]}
        />
      </View>

      <View style={styles.helpContainer}>
        <Text style={styles.helpTitle}>Code Format:</Text>
        <Text style={styles.helpText}>• 6-20 characters long</Text>
        <Text style={styles.helpText}>• Letters, numbers, and hyphens only</Text>
        <Text style={styles.helpText}>• Example: ABC123-XYZ789</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    margin: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 8,
    textAlign: 'center',
  },
  description: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    marginBottom: 20,
    lineHeight: 20,
  },
  inputContainer: {
    marginBottom: 16,
  },
  input: {
    fontSize: 16,
    letterSpacing: 1,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
  },
  button: {
    flex: 1,
  },
  clearButton: {
    backgroundColor: '#95a5a6',
  },
  submitButton: {
    backgroundColor: '#3498db',
  },
  helpContainer: {
    backgroundColor: '#ecf0f1',
    padding: 12,
    borderRadius: 8,
  },
  helpTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  helpText: {
    fontSize: 12,
    color: '#7f8c8d',
    marginBottom: 2,
  },
});
