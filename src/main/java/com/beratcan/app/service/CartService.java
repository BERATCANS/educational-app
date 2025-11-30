package com.beratcan.app.service;

import com.beratcan.app.dto.response.CartItemResponse;
import com.beratcan.app.dto.response.CartResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.Cart;
import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.CartItemNotFoundException;
import com.beratcan.app.exception.CartNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.CartItemRepository;
import com.beratcan.app.repository.CartRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final UserRepository userRepository;
        private final BookRepository bookRepository;
        private final DiscountCodeService discountCodeService;

        /**
         * Kullanıcının sepetini getir, yoksa oluştur
         */
        @Transactional
        public Cart getOrCreateCart(Long userId) {
                return cartRepository.findByUserId(userId)
                                .orElseGet(() -> {
                                        User user = userRepository.findById(userId)
                                                        .orElseThrow(() -> new UserNotFoundException(userId));
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
                log.info("Adding book to cart: userId={}, bookId={}", userId, bookId);

                // Kullanıcının sepetini getir veya oluştur
                Cart cart = getOrCreateCart(userId);

                // Kitabı getir
                Book book = bookRepository.findById(bookId)
                                .orElseThrow(() -> {
                                        log.error("Book not found for cart: bookId={}", bookId);
                                        return new BookNotFoundException(bookId);
                                });

                // Kitap paralı mı kontrol et
                if (!book.isPaid()) {
                        throw new BadRequestException("Ücretsiz kitaplar sepete eklenemez");
                }

                // Fiyat kontrolü
                if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("Kitap fiyatı geçerli değil");
                }

                // Kullanıcı zaten bu kitaba sahip mi?
                User user = cart.getUser();
                boolean alreadyOwned = user.getOwnedBooks().stream()
                                .anyMatch(b -> b.getId().equals(bookId));
                if (alreadyOwned) {
                        log.info("Book already owned, not added to cart: userId={}, bookId={}, bookTitle={}",
                                        userId, bookId, book.getTitle());
                        // ✅ Exception değil, bilgi mesajı olarak dön
                        CartResponse response = mapToResponse(cart);
                        response.setInfoMessage(
                                        "'" + book.getTitle() + "' zaten kütüphanenizde mevcut. Sepete eklenmedi.");
                        return response;
                }

                // Sepette zaten var mı kontrol et
                if (cartItemRepository.existsByCartIdAndBookId(cart.getId(), bookId)) {
                        log.info("Book already in cart: userId={}, bookId={}, bookTitle={}",
                                        userId, bookId, book.getTitle());
                        // ✅ Exception değil, bilgi mesajı olarak dön
                        CartResponse response = mapToResponse(cart);
                        response.setInfoMessage("'" + book.getTitle() + "' zaten sepetinizde mevcut.");
                        return response;
                }

                // Sepete ekle
                CartItem cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setBook(book);
                cartItem.setPriceAtAdd(book.getPrice());

                cart.addItem(cartItem);
                cartItemRepository.save(cartItem);

                log.info("Book added to cart successfully: userId={}, bookId={}, bookTitle={}, price={}, cartItemCount={}",
                                userId, bookId, book.getTitle(), book.getPrice(), cart.getItems().size());

                CartResponse response = mapToResponse(cart);
                response.setInfoMessage("'" + book.getTitle() + "' sepete eklendi.");
                return response;
        }

        /**
         * Sepetten kitap çıkar
         */
        @Transactional
        public CartResponse removeFromCart(Long userId, Long cartItemId) {
                log.info("Removing item from cart: userId={}, cartItemId={}", userId, cartItemId);

                Cart cart = cartRepository.findByUserId(userId)
                                .orElseThrow(() -> new CartNotFoundException(userId));

                CartItem cartItem = cartItemRepository.findById(cartItemId)
                                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));

                // Bu öğe bu kullanıcının sepetine ait mi kontrol et
                if (!cartItem.getCart().getId().equals(cart.getId())) {
                        log.warn("Unauthorized cart item removal attempt: userId={}, cartItemId={}, actualCartId={}",
                                        userId, cartItemId, cartItem.getCart().getId());
                        throw new UnauthorizedOperationException("Bu ürün sizin sepetinizde değil");
                }

                String bookTitle = cartItem.getBook().getTitle();
                cart.removeItem(cartItem);
                cartItemRepository.delete(cartItem);

                log.info("Item removed from cart successfully: userId={}, cartItemId={}, bookTitle={}, remainingItems={}",
                                userId, cartItemId, bookTitle, cart.getItems().size());

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
                log.info("Clearing cart: userId={}", userId);

                Cart cart = cartRepository.findByUserId(userId)
                                .orElseThrow(() -> new CartNotFoundException(userId));

                int itemCount = cart.getItems().size();
                cart.getItems().clear();
                // İndirim kodu bilgilerini de temizle
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(null);
                cart.setTotalPriceAfterDiscount(null);
                cartRepository.save(cart);

                log.info("Cart cleared successfully: userId={}, itemsRemoved={}", userId, itemCount);
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

                // İndirim bilgilerini ekle
                if (cart.getAppliedDiscountCode() != null) {
                        response.setAppliedDiscountCode(cart.getAppliedDiscountCode().getCode());
                        response.setDiscountAmount(cart.getDiscountAmount());
                        response.setTotalPriceAfterDiscount(cart.getTotalPriceAfterDiscount());
                }

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
                response.setBookPublisher(
                                item.getBook().getPublisher() != null ? item.getBook().getPublisher().getName() : null);
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

        /**
         * Sepete indirim kodu uygula
         */
        @Transactional
        public CartResponse applyDiscountCode(Long userId, String discountCode) {
                log.info("Applying discount code to cart: userId={}, code={}", userId, discountCode);

                Cart cart = getOrCreateCart(userId);

                // İndirim kodunu validate et
                com.beratcan.app.dto.response.DiscountValidationResult validation = discountCodeService
                                .validateDiscountCode(discountCode, userId, cart);

                if (!validation.isValid()) {
                        log.warn("Discount code validation failed: userId={}, code={}, error={}",
                                        userId, discountCode, validation.getErrorMessage());
                        throw new BadRequestException(validation.getErrorMessage());
                }

                // Sepete uygula
                cart.setAppliedDiscountCode(validation.getDiscountCode());
                cart.setDiscountAmount(validation.getDiscountAmount());

                // İndirim sonrası toplamı hesapla
                BigDecimal cartTotal = calculateCartTotal(cart);
                cart.setTotalPriceAfterDiscount(cartTotal.subtract(validation.getDiscountAmount()));

                cartRepository.save(cart);

                log.info("Discount code applied successfully: userId={}, code={}, discountAmount={}",
                                userId, discountCode, validation.getDiscountAmount());

                return mapToResponse(cart);
        }

        /**
         * Sepetten indirim kodunu kaldır
         */
        @Transactional
        public CartResponse removeDiscountCode(Long userId) {
                log.info("Removing discount code from cart: userId={}", userId);

                Cart cart = getOrCreateCart(userId);
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(null);
                cart.setTotalPriceAfterDiscount(null);
                cartRepository.save(cart);

                log.info("Discount code removed successfully: userId={}", userId);

                return mapToResponse(cart);
        }

        /**
         * Sepet toplamını hesapla
         */
        private BigDecimal calculateCartTotal(Cart cart) {
                return cart.getItems().stream()
                                .map(item -> item.getBook().getPrice())
                                .filter(price -> price != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
}
