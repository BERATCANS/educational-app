import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  Modal,
  TouchableOpacity,
} from 'react-native';
import { BookList } from '../components/BookList';
import { BookCodeInput } from '../components/BookCodeInput';
import { useBooks } from '../hooks/useBooks';
import { Book } from '../types';

export const BookManagementScreen: React.FC = () => {
  const { useBookCode, loading } = useBooks();
  const [showCodeInput, setShowCodeInput] = useState(false);

  const handleCodeUsed = async (code: string) => {
    const result = await useBookCode(code);
    
    if (result.success) {
      Alert.alert('Success', result.message);
      setShowCodeInput(false);
    } else {
      Alert.alert('Error', result.message);
    }
  };

  const handleBookPress = (book: Book) => {
    Alert.alert(
      'Book Details',
      `Title: ${book.title}\nPublisher: ${book.publisher}\nType: ${book.paid ? 'Paid' : 'Free'}`,
      [{ text: 'OK' }]
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>My Books</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => setShowCodeInput(true)}
          disabled={loading}
        >
          <Text style={styles.addButtonText}>+ Add Book</Text>
        </TouchableOpacity>
      </View>

      <BookList
        onBookPress={handleBookPress}
        showAddButton={true}
        onAddBook={() => setShowCodeInput(true)}
      />

      <Modal
        visible={showCodeInput}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowCodeInput(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Add Book with Code</Text>
            <TouchableOpacity
              style={styles.closeButton}
              onPress={() => setShowCodeInput(false)}
            >
              <Text style={styles.closeButtonText}>✕</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <BookCodeInput
              onCodeUsed={handleCodeUsed}
              loading={loading}
            />
          </ScrollView>
        </View>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  addButton: {
    backgroundColor: '#3498db',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  addButtonText: {
    color: '#fff',
    fontWeight: '500',
  },
  modalContainer: {
    flex: 1,
    backgroundColor: '#fff',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  closeButton: {
    padding: 8,
  },
  closeButtonText: {
    fontSize: 18,
    color: '#7f8c8d',
  },
  modalContent: {
    flex: 1,
  },
});
