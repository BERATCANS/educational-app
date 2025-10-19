import { create } from 'zustand';
import { Cart, CartItem, Purchase } from '../types';
import { apiService } from '../services/api';

interface CartState {
  // State
  cart: Cart | null;
  itemCount: number;
  isLoading: boolean;
  error: string | null;

  // Actions
  fetchCart: () => Promise<void>;
  addToCart: (bookId: number) => Promise<{ success: boolean; cart?: Cart; warning?: string; error?: string }>;
  removeFromCart: (itemId: number) => Promise<void>;
  clearCart: () => Promise<void>;
  checkout: () => Promise<Purchase[]>;
  resetError: () => void;
}

export const useCartStore = create<CartState>((set, get) => ({
  // Initial state
  cart: null,
  itemCount: 0,
  isLoading: false,
  error: null,

  // Actions
  fetchCart: async () => {
    set({ isLoading: true, error: null });
    try {
      const cart = await apiService.getCart();
      set({ 
        cart,
        itemCount: cart.items.length
      });
    } catch (error) {
      set({ error: error instanceof Error ? error.message : 'Sepet yüklenirken bir hata oluştu' });
    } finally {
      set({ isLoading: false });
    }
  },

  addToCart: async (bookId: number) => {
    set({ isLoading: true, error: null });
    try {
      const result = await apiService.addToCart(bookId);
      if (result.success && result.cart) {
        set({ 
          cart: result.cart,
          itemCount: result.cart.items.length
        });
      }
      return result;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Kitap sepete eklenirken bir hata oluştu';
      set({ error: errorMessage });
      return {
        success: false,
        error: errorMessage
      };
    } finally {
      set({ isLoading: false });
    }
  },

  removeFromCart: async (itemId: number) => {
    set({ isLoading: true, error: null });
    try {
      const updatedCart = await apiService.removeFromCart(itemId);
      set({ 
        cart: updatedCart,
        itemCount: updatedCart.items.length
      });
    } catch (error) {
      set({ error: error instanceof Error ? error.message : 'Ürün sepetten çıkarılırken bir hata oluştu' });
      throw error;
    } finally {
      set({ isLoading: false });
    }
  },

  clearCart: async () => {
    set({ isLoading: true, error: null });
    try {
      await apiService.clearCart();
      set({ 
        cart: { id: get().cart?.id || 0, items: [], totalAmount: 0, itemCount: 0 },
        itemCount: 0
      });
    } catch (error) {
      set({ error: error instanceof Error ? error.message : 'Sepet temizlenirken bir hata oluştu' });
      throw error;
    } finally {
      set({ isLoading: false });
    }
  },

  checkout: async () => {
    set({ isLoading: true, error: null });
    try {
      const purchases = await apiService.checkout();
      // Checkout başarılı olduğunda sepeti sıfırla
      set({ 
        cart: { id: get().cart?.id || 0, items: [], totalAmount: 0, itemCount: 0 },
        itemCount: 0
      });
      return purchases;
    } catch (error) {
      set({ error: error instanceof Error ? error.message : 'Satın alma işlemi sırasında bir hata oluştu' });
      throw error;
    } finally {
      set({ isLoading: false });
    }
  },

  resetError: () => set({ error: null }),
}));
