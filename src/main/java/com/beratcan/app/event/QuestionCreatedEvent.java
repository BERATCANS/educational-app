package com.beratcan.app.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Soru oluşturulduğunda yayınlanan event
 * Transaction commit edildikten sonra AI analiz başlatmak için kullanılır
 */
@Getter
public class QuestionCreatedEvent extends ApplicationEvent {
    private final Long questionId;

    public QuestionCreatedEvent(Object source, Long questionId) {
        super(source);
        this.questionId = questionId;
    }
}
