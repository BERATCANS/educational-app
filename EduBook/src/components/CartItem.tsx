import React from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { CartItem as CartItemType } from '../types';

interface CartItemProps {
  item: CartItemType;
  onRemove: (itemId: number) => Promise<void>;
}

export const CartItem: React.FC<CartItemProps> = ({ item, onRemove }) => {
  const [loading, setLoading] = React.useState(false);

  const handleRemove = async () => {
    if (loading) return;
    
    setLoading(true);
    try {
      await onRemove(item.id);
    } catch (error) {
      console.error('Error removing from cart:', error);
    } finally {
      setLoading(false);
    }
  };

  const priceChanged = item.currentPrice !== item.priceAtAdd;

  return (
    <View style={styles.container}>
      <Image
        source={{ uri: item.bookCoverImageUrl }}
        style={styles.image}
        resizeMode="cover"
      />
      
      <View style={styles.infoContainer}>
        <Text style={styles.title} numberOfLines={2}>
          {item.bookTitle}
        </Text>
        
        <Text style={styles.publisher} numberOfLines={1}>
          {item.bookPublisher}
        </Text>

        <View style={styles.priceContainer}>
          {priceChanged && (
            <Text style={styles.oldPrice}>
              {item.priceAtAdd.toLocaleString('tr-TR', {
                style: 'currency',
                currency: 'TRY'
              })}
            </Text>
          )}
          
          <Text style={[
            styles.price,
            priceChanged && (
              item.currentPrice > item.priceAtAdd
                ? styles.priceIncreased
                : styles.priceDecreased
            )
          ]}>
            {item.currentPrice.toLocaleString('tr-TR', {
              style: 'currency',
              currency: 'TRY'
            })}
          </Text>
        </View>
      </View>

      <TouchableOpacity
        style={styles.removeButton}
        onPress={handleRemove}
        disabled={loading}
        activeOpacity={0.7}
      >
        {loading ? (
          <ActivityIndicator size="small" color="#ef4444" />
        ) : (
          <Ionicons name="trash-outline" size={20} color="#ef4444" />
        )}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 16,
    padding: 12,
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
    width: 80,
    height: 120,
    borderRadius: 8,
  },
  infoContainer: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'space-between',
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1a1a1a',
    marginBottom: 4,
  },
  publisher: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  priceContainer: {
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
  oldPrice: {
    fontSize: 14,
    color: '#666',
    textDecorationLine: 'line-through',
    marginBottom: 2,
  },
  price: {
    fontSize: 18,
    fontWeight: '700',
    color: '#10b981',
  },
  priceIncreased: {
    color: '#ef4444', // kırmızı
  },
  priceDecreased: {
    color: '#10b981', // yeşil
  },
  removeButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#fee2e2',
    justifyContent: 'center',
    alignItems: 'center',
    alignSelf: 'center',
    marginLeft: 12,
  },
});
