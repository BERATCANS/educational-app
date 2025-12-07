package com.beratcan.app.service;

import com.beratcan.app.dto.request.DailyQuestionAnswerRequest;
import com.beratcan.app.dto.request.CreateDailyQuestionRequest;
import com.beratcan.app.dto.request.UpdateDailyQuestionRequest;
import com.beratcan.app.dto.response.DailyQuestionResponse;
import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.entity.DailyQuestionAnswer;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.DailyQuestionRepository;
import com.beratcan.app.repository.DailyQuestionAnswerRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyQuestionService {

    private final DailyQuestionRepository dailyQuestionRepository;
    private final DailyQuestionAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    public DailyQuestionResponse getCurrentQuestion(Long userId) {
        if (userId == null) {
            throw new UnauthorizedOperationException("Kullanıcı ID'si gerekli");
        }

        // Kullanıcı kontrolü (gerekli değil ama tutarlılık için)
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        DailyQuestion question = dailyQuestionRepository.findByDisplayDate(LocalDate.now())
                .orElse(null);

        if (question == null) {
            DailyQuestionResponse response = new DailyQuestionResponse();
            response.setErrorMessage("Aktif günlük soru bulunamadı");
            return response;
        }

        return toDailyQuestionResponse(question, userId);
    }

    @Transactional
    public DailyQuestionResponse submitAnswer(Long userId, DailyQuestionAnswerRequest request) {
        // Input validasyonu
        if (userId == null) {
            throw new UnauthorizedOperationException("Kullanıcı ID'si gerekli");
        }
        if (request == null) {
            throw new BadRequestException("Cevap bilgisi gerekli");
        }
        if (request.getDailyQuestionId() == null) {
            throw new BadRequestException("Soru ID'si gerekli");
        }
        if (request.getSelectedAnswer() == null) {
            throw new BadRequestException("Seçilen cevap gerekli");
        }

        // Kullanıcı kontrolü
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Soru kontrolü
        DailyQuestion question = dailyQuestionRepository.findById(request.getDailyQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Soru bulunamadı"));

        // Zaman kontrolü
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = LocalDateTime.of(question.getDisplayDate(), question.getStartTime());
        LocalDateTime endTime = LocalDateTime.of(question.getDisplayDate(), question.getEndTime());

        if (now.isBefore(startTime)) {
            throw new BadRequestException("Soru henüz başlamadı");
        }
        if (now.isAfter(endTime)) {
            throw new BadRequestException("Soru süresi sona erdi");
        }

        // Daha önce cevaplanmış mı kontrolü
        if (answerRepository.existsByDailyQuestionIdAndUserId(question.getId(), userId)) {
            throw new ConflictException("Bu soruyu daha önce cevapladınız");
        }

        // Cevabı kaydet
        DailyQuestionAnswer answer = new DailyQuestionAnswer();
        answer.setDailyQuestion(question);
        answer.setUser(user);
        answer.setSelectedAnswer(request.getSelectedAnswer());
        answerRepository.save(answer);

        return toDailyQuestionResponse(question, userId);
    }

    @Transactional
    public DailyQuestion createDailyQuestion(CreateDailyQuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("CreateDailyQuestionRequest cannot be null");
        }

        log.info("Günlük soru oluşturma başlatıldı: displayDate={}, correctAnswer={}",
                request.getDisplayDate(), request.getCorrectAnswer());

        // 1. Görseli kaydet (FileService kullan)
        String imageUrl = fileService.saveFile(request.getImage());
        log.info("Görsel kaydedildi: {}", imageUrl);

        // 2. Yeni soru oluştur
        DailyQuestion question = new DailyQuestion();
        question.setImageUrl(imageUrl); // FileService'ten gelen filename
        question.setAnswerNumber(request.getAnswerNumber());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setVideoUrl(request.getVideoUrl());
        question.setExplanation(request.getExplanation());
        question.setDisplayDate(request.getDisplayDate());
        question.setStartTime(request.getStartTime());
        question.setEndTime(request.getEndTime());
        question.setActive(false); // Başlangıçta pasif, scheduler aktifleştirecek

        DailyQuestion saved = dailyQuestionRepository.save(question);
        log.info("Günlük soru oluşturuldu: questionId={}", saved.getId());
        return saved;
    }

    /**
     * Admin için tekil günlük soru getir (Zaman kontrolü yapmadan, tüm bilgileri
     * gösterir)
     */
    @Transactional(readOnly = true)
    public DailyQuestionResponse getDailyQuestionByIdForAdmin(Long questionId) {
        DailyQuestion question = dailyQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Günlük soru bulunamadı: " + questionId));

        // Admin için özel response - zaman kontrolü yapmadan tüm bilgileri göster
        return toDailyQuestionResponseForAdmin(question);
    }

    /**
     * Tüm günlük soruları getir (Admin için)
     */
    @Transactional(readOnly = true)
    public List<DailyQuestionResponse> getAllDailyQuestions() {
        List<DailyQuestion> questions = dailyQuestionRepository.findAll();
        return questions.stream()
                .map(question -> toDailyQuestionResponse(question, null))
                .collect(Collectors.toList());
    }

    /**
     * Günlük soruyu güncelle (Admin için)
     */
    @Transactional
    public DailyQuestionResponse updateDailyQuestion(Long questionId, UpdateDailyQuestionRequest request) {
        if (request == null) {
            throw new BadRequestException("UpdateDailyQuestionRequest cannot be null");
        }

        DailyQuestion question = dailyQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Günlük soru bulunamadı: " + questionId));

        log.info("Günlük soru güncelleme başlatıldı: questionId={}", questionId);

        // Görsel güncelleme (opsiyonel)
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // Eski görseli sil
            if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
                fileService.deleteFile(question.getImageUrl());
            }
            // Yeni görseli kaydet
            String newImageUrl = fileService.saveFile(request.getImage());
            question.setImageUrl(newImageUrl);
            log.info("Görsel güncellendi: {}", newImageUrl);
        }

        // Diğer alanları güncelle (sadece null değilse)
        if (request.getAnswerNumber() != null) {
            question.setAnswerNumber(request.getAnswerNumber());
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getVideoUrl() != null) {
            question.setVideoUrl(request.getVideoUrl());
        }
        if (request.getExplanation() != null) {
            question.setExplanation(request.getExplanation());
        }
        if (request.getDisplayDate() != null) {
            question.setDisplayDate(request.getDisplayDate());
        }
        if (request.getStartTime() != null) {
            question.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            question.setEndTime(request.getEndTime());
        }
        if (request.getActive() != null) {
            question.setActive(request.getActive());
        }

        DailyQuestion updated = dailyQuestionRepository.save(question);
        log.info("Günlük soru güncellendi: questionId={}", updated.getId());

        return toDailyQuestionResponse(updated, null);
    }

    /**
     * Günlük soruyu sil (Admin için)
     */
    @Transactional
    public void deleteDailyQuestion(Long questionId) {
        DailyQuestion question = dailyQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Günlük soru bulunamadı: " + questionId));

        log.info("Günlük soru silme başlatıldı: questionId={}", questionId);

        // Görseli sil
        if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
            fileService.deleteFile(question.getImageUrl());
        }

        // İlişkili cevapları sil
        List<DailyQuestionAnswer> answers = answerRepository.findByDailyQuestionId(questionId);
        if (!answers.isEmpty()) {
            answerRepository.deleteAll(answers);
            log.info("{} adet cevap silindi", answers.size());
        }

        // Soruyu sil
        dailyQuestionRepository.delete(question);
        log.info("Günlük soru silindi: questionId={}", questionId);
    }

    private DailyQuestionResponse toDailyQuestionResponse(DailyQuestion question, Long userId) {
        DailyQuestionResponse response = new DailyQuestionResponse();
        response.setId(question.getId());
        // Database'de filename var, response'da URL oluştur
        // Eğer zaten URL formatındaysa (eski kayıtlar için) olduğu gibi bırak
        String imageUrl = question.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("/api/files/")) {
            response.setImageUrl("/api/files/" + imageUrl);
        } else {
            response.setImageUrl(imageUrl);
        }
        response.setAnswerNumber(question.getAnswerNumber());
        response.setVideoUrl(question.getVideoUrl());
        response.setDisplayDate(question.getDisplayDate());
        response.setStartTime(question.getStartTime());
        response.setEndTime(question.getEndTime());
        response.setActive(question.isActive());

        // Kullanıcının cevap bilgisi
        answerRepository.findByDailyQuestionIdAndUserId(question.getId(), userId).ifPresent(answer -> {
            DailyQuestionResponse.UserAnswerInfo userAnswer = new DailyQuestionResponse.UserAnswerInfo();
            userAnswer.setSelectedAnswer(answer.getSelectedAnswer());
            userAnswer.setAnsweredAt(answer.getAnsweredAt().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setUserAnswer(userAnswer);
            response.setHasUserAnswered(true);
        });

        // Bitiş zamanı geçtiyse ek bilgileri ekle
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(question.getDisplayDate(), question.getEndTime());
        if (now.isAfter(endTime)) {
            DailyQuestionResponse.AfterEndTimeInfo afterEndTime = new DailyQuestionResponse.AfterEndTimeInfo();
            afterEndTime.setCorrectAnswer(question.getCorrectAnswer());
            afterEndTime.setExplanation(question.getExplanation());

            // İstatistikler
            DailyQuestionResponse.AnswerStatistics statistics = new DailyQuestionResponse.AnswerStatistics();
            statistics.setTotalAnswers(answerRepository.countAnswersByQuestionId(question.getId()));

            // Answer distribution'ı manuel olarak oluştur
            Map<OptionLetter, Long> distribution = new HashMap<>();
            List<Object[]> distributionData = answerRepository.getAnswerDistribution(question.getId());
            for (Object[] row : distributionData) {
                OptionLetter option = (OptionLetter) row[0];
                Long count = (Long) row[1];
                distribution.put(option, count);
            }
            statistics.setDistribution(distribution);
            afterEndTime.setStatistics(statistics);

            response.setAfterEndTime(afterEndTime);
        }

        return response;
    }

    /**
     * Admin için özel response - Zaman kontrolü yapmadan tüm bilgileri gösterir
     */
    private DailyQuestionResponse toDailyQuestionResponseForAdmin(DailyQuestion question) {
        DailyQuestionResponse response = new DailyQuestionResponse();
        response.setId(question.getId());

        // Görsel URL'i oluştur
        String imageUrl = question.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("/api/files/")) {
            response.setImageUrl("/api/files/" + imageUrl);
        } else {
            response.setImageUrl(imageUrl);
        }

        response.setAnswerNumber(question.getAnswerNumber());
        response.setVideoUrl(question.getVideoUrl());
        response.setDisplayDate(question.getDisplayDate());
        response.setStartTime(question.getStartTime());
        response.setEndTime(question.getEndTime());
        response.setActive(question.isActive());

        // Admin için her zaman doğru cevap, açıklama ve istatistikleri göster
        DailyQuestionResponse.AfterEndTimeInfo afterEndTime = new DailyQuestionResponse.AfterEndTimeInfo();
        afterEndTime.setCorrectAnswer(question.getCorrectAnswer());
        afterEndTime.setExplanation(question.getExplanation());

        // İstatistikler
        DailyQuestionResponse.AnswerStatistics statistics = new DailyQuestionResponse.AnswerStatistics();
        statistics.setTotalAnswers(answerRepository.countAnswersByQuestionId(question.getId()));

        // Answer distribution
        Map<OptionLetter, Long> distribution = new HashMap<>();
        List<Object[]> distributionData = answerRepository.getAnswerDistribution(question.getId());
        for (Object[] row : distributionData) {
            OptionLetter option = (OptionLetter) row[0];
            Long count = (Long) row[1];
            distribution.put(option, count);
        }
        statistics.setDistribution(distribution);
        afterEndTime.setStatistics(statistics);

        response.setAfterEndTime(afterEndTime);

        return response;
    }
}
