package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakResponse {
    private Integer currentStreak; // Mevcut günlük giriş serisi
    private Integer longestStreak; // En uzun günlük giriş serisi
    private String lastLoginDate; // Son giriş tarihi (yyyy-MM-dd formatında)
    private Boolean loggedInToday; // Bugün giriş yapılmış mı
}
