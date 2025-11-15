package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookStoreResponse {
    private Long id;
    private String coverImageUrl;
    private String publisher;
    private boolean paid;
    private BigDecimal price;
    private Double averageRating;
    private boolean isOwned;
    private Long reviewCount;
}
