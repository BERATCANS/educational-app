package com.beratcan.app.constants;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Tek noktada zaman üretimi. Tüm uygulama "Europe/Istanbul" zone'unu kullanır;
 * sunucu farklı bir tz'de çalışsa bile timestamp'ler tutarlı kalır.
 *
 * Why: Geçmişte servis ve entity'lerde `LocalDateTime.now()` (system tz) ile
 * `LocalDateTime.now(TURKEY_ZONE)` karışık kullanılıyordu; raporlama ve
 * süre/expiry karşılaştırmaları sunucu zone değişiminde kayıyordu.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private AppTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
