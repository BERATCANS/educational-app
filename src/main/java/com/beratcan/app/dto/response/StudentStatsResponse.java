package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard "Hızlı İstatistikler" yanıtı.
 *
 * <p>totalSolvedQuestions: kullanıcının cevapladığı farklı soru sayısı
 * (aynı soruya verilen birden çok cevap için son cevap baz alınır).
 *
 * <p>accuracyRate: 0–100 arası, iki ondalık basamağa yuvarlanmış doğruluk yüzdesi.
 * total = 0 ise 0.0 döner.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentStatsResponse {
    private long totalSolvedQuestions;
    private double accuracyRate;
}
