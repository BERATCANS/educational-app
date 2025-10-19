import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Alert
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useCartStore } from '../store/cartStore';
import { CartItem } from '../components/CartItem';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';

export const CartScreen = () => {
  const navigation = useNavigation();
  const {
    cart,
    isLoading,
    error,
    fetchCart,
    removeFromCart,
    clearCart,
    checkout
  } = useCartStore();

  useEffect(() => {
    fetchCart();
  }, []);

  const handleRemoveItem = async (itemId: number) => {
    try {
      await removeFromCart(itemId);
    } catch (error) {
      Alert.alert('Hata', error instanceof Error ? error.message : 'Ürün sepetten çıkarılamadı');
    }
  };

  const handleClearCart = () => {
    Alert.alert(
      'Sepeti Temizle',
      'Sepetinizdeki tüm ürünler silinecek. Emin misiniz?',
      [
        { text: 'İptal', style: 'cancel' },
        {
          text: 'Temizle',
          style: 'destructive',
          onPress: async () => {
            try {
              await clearCart();
            } catch (error) {
              Alert.alert('Hata', error instanceof Error ? error.message : 'Sepet temizlenemedi');
            }
          },
        },
      ]
    );
  };

  const handleCheckout = async () => {
    try {
      const purchases = await checkout();
      Alert.alert(
        'Başarılı',
        'Satın alma işlemi tamamlandı. Kitaplar kütüphanenize eklendi.',
        [
          {
            text: 'Kütüphaneye Git',
            onPress: () => navigation.navigate('Library' as never),
          },
          { text: 'Tamam' },
        ]
      );
    } catch (error) {
      Alert.alert('Hata', error instanceof Error ? error.message : 'Satın alma işlemi başarısız');
    }
  };

  if (error) {
    return (
      <View style={styles.centerContainer}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity
          style={styles.retryButton}
          onPress={fetchCart}
        >
          <Text style={styles.retryButtonText}>Tekrar Dene</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (isLoading && !cart) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#10b981" />
      </View>
    );
  }

  if (!cart?.items.length) {
    return (
      <View style={styles.centerContainer}>
        <Ionicons name="cart-outline" size={48} color="#666" />
        <Text style={styles.emptyText}>Sepetiniz boş</Text>
        <TouchableOpacity
          style={styles.shopButton}
          onPress={() => navigation.navigate('BookStore' as never)}
        >
          <Text style={styles.shopButtonText}>Alışverişe Başla</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* Sepet Başlığı */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>
          Sepetim ({cart.items.length} ürün)
        </Text>
        <TouchableOpacity
          onPress={handleClearCart}
          style={styles.clearButton}
        >
          <Text style={styles.clearButtonText}>Sepeti Temizle</Text>
        </TouchableOpacity>
      </View>

      {/* Sepet Listesi */}
      <FlatList
        data={cart.items}
        renderItem={({ item }) => (
          <CartItem
            item={item}
            onRemove={handleRemoveItem}
          />
        )}
        keyExtractor={item => item.id.toString()}
        contentContainerStyle={styles.listContent}
      />

      {/* Alt Toplam ve Satın Al */}
      <View style={styles.footer}>
        <View style={styles.totalContainer}>
          <Text style={styles.totalLabel}>Toplam</Text>
          <Text style={styles.totalAmount}>
            {cart.totalAmount.toLocaleString('tr-TR', {
              style: 'currency',
              currency: 'TRY'
            })}
          </Text>
        </View>

        <TouchableOpacity
          style={styles.checkoutButton}
          onPress={handleCheckout}
          disabled={isLoading}
        >
          {isLoading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <>
              <Text style={styles.checkoutButtonText}>Satın Al</Text>
              <Ionicons name="arrow-forward" size={20} color="#fff" />
            </>
          )}
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
  },
  clearButton: {
    padding: 8,
  },
  clearButtonText: {
    fontSize: 14,
    color: '#ef4444',
  },
  listContent: {
    padding: 16,
  },
  footer: {
    padding: 16,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  totalContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  totalLabel: {
    fontSize: 16,
    color: '#4b5563',
  },
  totalAmount: {
    fontSize: 20,
    fontWeight: '700',
    color: '#10b981',
  },
  checkoutButton: {
    backgroundColor: '#10b981',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
    borderRadius: 8,
    gap: 8,
  },
  checkoutButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  emptyText: {
    fontSize: 16,
    color: '#666',
    marginTop: 16,
    marginBottom: 24,
  },
  shopButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  shopButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  errorText: {
    fontSize: 16,
    color: '#ef4444',
    textAlign: 'center',
    marginBottom: 16,
  },
  retryButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
