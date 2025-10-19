package com.beratcan.app.service;

import com.beratcan.app.dto.response.CartItemResponse;
import com.beratcan.app.dto.response.CartResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.CartItemRepository;
import com.beratcan.app.repository.CartRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Kullanıcının sepetini getir, yoksa oluştur
     */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Sepete kitap ekle
     */
    @Transactional
    public CartResponse addToCart(Long userId, Long bookId) {
        // Kullanıcının sepetini getir veya oluştur
        Cart cart = getOrCreateCart(userId);

        // Kitabı getir
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı"));

        // Kitap paralı mı kontrol et
        if (!book.isPaid()) {
            throw new RuntimeException("Ücretsiz kitaplar sepete eklenemez");
        }

        // Fiyat kontrolü
        if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Kitap fiyatı geçerli değil");
        }

        // Kullanıcı zaten bu kitaba sahip mi?
        User user = cart.getUser();
        boolean alreadyOwned = user.getOwnedBooks().stream()
                .anyMatch(b -> b.getId().equals(bookId));
        if (alreadyOwned) {
            throw new RuntimeException("Bu kitap zaten kütüphanenizde mevcut");
        }

        // Sepette zaten var mı kontrol et
        if (cartItemRepository.existsByCartIdAndBookId(cart.getId(), bookId)) {
            throw new RuntimeException("Bu kitap zaten sepetinizde mevcut");
        }

        // Sepete ekle
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setBook(book);
        cartItem.setPriceAtAdd(book.getPrice());

        cart.addItem(cartItem);
        cartItemRepository.save(cartItem);

        return mapToResponse(cart);
    }

    /**
     * Sepetten kitap çıkar
     */
    @Transactional
    public CartResponse removeFromCart(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sepet ögesi bulunamadı"));

        // Bu öğe bu kullanıcının sepetine ait mi kontrol et
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Bu ürün sizin sepetinizde değil");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToResponse(cart);
    }

    /**
     * Sepeti görüntüle
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> getOrCreateCart(userId));
        return mapToResponse(cart);
    }

    /**
     * Sepeti temizle
     */
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Sepet bulunamadı"));

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    /**
     * Sepetteki kitap sayısı
     */
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().size())
                .orElse(0);
    }

    /**
     * Cart'ı CartResponse'a dönüştür
     */
    private CartResponse mapToResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUser().getId());
        response.setCreatedAt(cart.getCreatedAt());
        response.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        response.setItemCount(itemResponses.size());

        // Toplam fiyatı hesapla
        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getCurrentPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalPrice(totalPrice);

        return response;
    }

    /**
     * CartItem'ı CartItemResponse'a dönüştür
     */
    private CartItemResponse mapItemToResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setBookId(item.getBook().getId());
        response.setBookTitle(item.getBook().getTitle());
        response.setBookPublisher(item.getBook().getPublisher());
        response.setBookCoverImageUrl(item.getBook().getCoverImageUrl());
        response.setPriceAtAdd(item.getPriceAtAdd());
        response.setCurrentPrice(item.getBook().getPrice());
        response.setAddedAt(item.getAddedAt());

        // Fiyat değişmiş mi kontrol et
        boolean priceChanged = item.getPriceAtAdd() != null &&
                item.getBook().getPrice() != null &&
                item.getPriceAtAdd().compareTo(item.getBook().getPrice()) != 0;
        response.setPriceChanged(priceChanged);

        return response;
    }
}
