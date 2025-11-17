package com.beratcan.app.service;

import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookRecommendationService {

    /**
     * Kullanıcının sınav tipine, sınıfına ve alanına göre kitapları sıralar
     */
    public List<Book> sortBooksByRecommendation(List<Book> books, User user) {
        // Sınav tipi veya sınıf yoksa, tüm kitapları olduğu gibi dön
        if (user.getExamType() == null && user.getGradeLevel() == null) {
            return books;
        }

        List<Book> recommendedBooks = new ArrayList<>();
        List<Book> otherBooks = new ArrayList<>();

        for (Book book : books) {
            boolean isRecommended = false;

            // LGS için özel mantık
            if (user.getExamType() == ExamType.LGS) {
                isRecommended = isRecommendedForLGS(book, user.getGradeLevel());
            }
            // YKS için özel mantık
            else if (user.getExamType() == ExamType.YKS) {
                isRecommended = isRecommendedForYKS(book, user.getGradeLevel());
            }
            // KPSS veya diğer sınavlar için
            else if (user.getExamType() == ExamType.KPSS ||
                    user.getExamType() == ExamType.DIGER) {
                isRecommended = book.getExamType() == user.getExamType();
            }

            if (isRecommended) {
                recommendedBooks.add(book);
            } else {
                otherBooks.add(book);
            }
        }

        // YKS için sınıf ve sınav tipine göre özel sıralama
        if (user.getExamType() == ExamType.YKS) {
            recommendedBooks = sortYKSRecommendedBooks(recommendedBooks, user.getGradeLevel());
        }
        // LGS için sınıf ve sınav tipine göre özel sıralama
        else if (user.getExamType() == ExamType.LGS) {
            recommendedBooks = sortLGSRecommendedBooks(recommendedBooks, user.getGradeLevel());
        }

        // Önce önerilen kitaplar, sonra diğerleri
        recommendedBooks.addAll(otherBooks);
        return recommendedBooks;
    }

    /**
     * YKS kitaplarını sınıf ve sınav tipine göre sıralar
     * Sıralama: Sınıfa uygun kitaplar -> Sınav tipine uygun kitaplar -> Diğerleri
     */
    private List<Book> sortYKSRecommendedBooks(List<Book> books, GradeLevel userGradeLevel) {
        List<Book> matchingGradeBooks = new ArrayList<>();
        List<Book> otherYKSBooks = new ArrayList<>();

        for (Book book : books) {
            // Sınıfa uygun kitaplar
            if (userGradeLevel != null && book.getGradeLevel() == userGradeLevel) {
                matchingGradeBooks.add(book);
            }
            // Diğer YKS kitapları
            else {
                otherYKSBooks.add(book);
            }
        }

        // Sıralamayı birleştir: sınıfa uygun -> diğer YKS kitapları
        List<Book> sortedBooks = new ArrayList<>();
        sortedBooks.addAll(matchingGradeBooks);
        sortedBooks.addAll(otherYKSBooks);
        return sortedBooks;
    }

    /**
     * LGS kitaplarını sınıf ve sınav tipine göre sıralar
     * 5-6-7. sınıf için: Sınıfa uygun kitaplar -> LGS kitapları
     * 8. sınıf için: LGS kitapları -> 5-6-7. sınıf kitapları
     */
    private List<Book> sortLGSRecommendedBooks(List<Book> books, GradeLevel userGradeLevel) {
        // 5-6-7. sınıf için sıralama
        if (userGradeLevel == GradeLevel.G5 ||
                userGradeLevel == GradeLevel.G6 ||
                userGradeLevel == GradeLevel.G7) {

            List<Book> matchingGradeBooks = new ArrayList<>();
            List<Book> lgsBooks = new ArrayList<>();

            for (Book book : books) {
                // Sınıfa uygun kitaplar
                if (book.getGradeLevel() == userGradeLevel) {
                    matchingGradeBooks.add(book);
                }
                // LGS kitapları
                else if (book.getExamType() == ExamType.LGS) {
                    lgsBooks.add(book);
                }
            }

            // Sıralamayı birleştir: sınıfa uygun -> LGS
            List<Book> sortedBooks = new ArrayList<>();
            sortedBooks.addAll(matchingGradeBooks);
            sortedBooks.addAll(lgsBooks);
            return sortedBooks;
        }

        // 8. sınıf için sıralama
        if (userGradeLevel == GradeLevel.G8) {
            List<Book> lgsBooks = new ArrayList<>();
            List<Book> grade5To7Books = new ArrayList<>();

            for (Book book : books) {
                // LGS kitapları
                if (book.getExamType() == ExamType.LGS) {
                    lgsBooks.add(book);
                }
                // 5-6-7. sınıf kitapları
                else if (book.getGradeLevel() == GradeLevel.G5 ||
                        book.getGradeLevel() == GradeLevel.G6 ||
                        book.getGradeLevel() == GradeLevel.G7) {
                    grade5To7Books.add(book);
                }
            }

            // Sıralamayı birleştir: LGS -> 5-6-7. sınıf
            List<Book> sortedBooks = new ArrayList<>();
            sortedBooks.addAll(lgsBooks);
            sortedBooks.addAll(grade5To7Books);
            return sortedBooks;
        }

        // Diğer durumlar için orijinal sırayı koru
        return books;
    }

    /**
     * LGS için öneri mantığı
     * - 5-6-7. sınıf: Sınıfına uygun kitaplar ve LGS kitapları
     * - 8. sınıf: LGS kitapları ve 5-6-7. sınıf kitapları
     */
    private boolean isRecommendedForLGS(Book book, GradeLevel userGradeLevel) {
        if (userGradeLevel == null) {
            return book.getExamType() == ExamType.LGS;
        }

        // 5-6-7. sınıf: Sınıfına uygun kitaplar ve LGS kitapları
        if (userGradeLevel == GradeLevel.G5 ||
                userGradeLevel == GradeLevel.G6 ||
                userGradeLevel == GradeLevel.G7) {
            // Sınıfına uygun kitaplar
            if (book.getGradeLevel() == userGradeLevel) {
                return true;
            }
            // LGS kitapları
            if (book.getExamType() == ExamType.LGS) {
                return true;
            }
        }

        // 8. sınıf: LGS kitapları öncelikli
        if (userGradeLevel == GradeLevel.G8) {
            // LGS kitapları
            if (book.getExamType() == ExamType.LGS) {
                return true;
            }
            // 5-6-7. sınıf kitapları da göster
            if (book.getGradeLevel() == GradeLevel.G5 ||
                    book.getGradeLevel() == GradeLevel.G6 ||
                    book.getGradeLevel() == GradeLevel.G7) {
                return true;
            }
        }

        return false;
    }

    /**
     * YKS için öneri mantığı
     * YKS kitapları her zaman önerilir, ayrıca sınıfına uygun kitaplar da önerilir
     */
    private boolean isRecommendedForYKS(Book book, GradeLevel userGradeLevel) {
        // YKS kitapları her zaman önerilir
        if (book.getExamType() == ExamType.YKS) {
            return true;
        }

        if (userGradeLevel == null) {
            return false;
        }

        // Sınıfına uygun kitaplar
        if (book.getGradeLevel() == userGradeLevel) {
            return true;
        }

        return false;
    }
}
