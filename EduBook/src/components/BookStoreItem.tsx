import React from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Dimensions
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BookStoreResponse } from '../types';

interface BookStoreItemProps {
  book: BookStoreResponse;
  onAddToCart: (bookId: number) => Promise<void>;
  isInCart?: boolean;
}

const { width } = Dimensions.get('window');
const CARD_WIDTH = width / 2 - 24; // 2 sütun, kenarlardan 16px padding

export const BookStoreItem: React.FC<BookStoreItemProps> = ({
  book,
  onAddToCart,
  isInCart = false
}) => {
  const [loading, setLoading] = React.useState(false);

  const handleAddToCart = async () => {
    if (loading || isInCart) return;
    
    setLoading(true);
    try {
      await onAddToCart(book.id);
    } catch (error) {
      console.error('Error adding to cart:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Image
        source={{ uri: book.coverImageUrl }}
        style={styles.image}
        resizeMode="cover"
      />
      
      <View style={styles.infoContainer}>
        <Text style={styles.title} numberOfLines={2}>
          {book.title}
        </Text>
        
        <Text style={styles.publisher} numberOfLines={1}>
          {book.publisher}
        </Text>

        <View style={styles.priceContainer}>
          <Text style={styles.price}>
            {book.price.toLocaleString('tr-TR', {
              style: 'currency',
              currency: 'TRY'
            })}
          </Text>

          <TouchableOpacity
            style={[
              styles.addButton,
              isInCart && styles.addButtonDisabled
            ]}
            onPress={handleAddToCart}
            disabled={loading || isInCart}
            activeOpacity={0.7}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : isInCart ? (
              <Ionicons name="checkmark" size={20} color="#fff" />
            ) : (
              <Ionicons name="cart-outline" size={20} color="#fff" />
            )}
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: CARD_WIDTH,
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  image: {
    width: '100%',
    height: CARD_WIDTH * 1.4, // 1.4 aspect ratio
    borderTopLeftRadius: 12,
    borderTopRightRadius: 12,
  },
  infoContainer: {
    padding: 12,
  },
  title: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1a1a1a',
    marginBottom: 4,
  },
  publisher: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
  },
  priceContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  price: {
    fontSize: 16,
    fontWeight: '700',
    color: '#10b981', // yeşil
  },
  addButton: {
    backgroundColor: '#10b981',
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  addButtonDisabled: {
    backgroundColor: '#6ee7b7', // daha açık yeşil
  },
});
