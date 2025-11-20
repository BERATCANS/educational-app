package com.beratcan.app.service;

import com.beratcan.app.dto.request.DailyQuestionAnswerRequest;
import com.beratcan.app.dto.request.CreateDailyQuestionRequest;
import com.beratcan.app.dto.response.DailyQuestionResponse;
import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.entity.DailyQuestionAnswer;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.FileStorageException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.DailyQuestionRepository;
import com.beratcan.app.repository.DailyQuestionAnswerRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyQuestionService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DailyQuestionRepository dailyQuestionRepository;
    private final DailyQuestionAnswerRepository answerRepository;
    private final UserRepository userRepository;

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
        // Resmi kaydet
        String filename = System.currentTimeMillis() + "_" + request.getImage().getOriginalFilename();
        Path filePath = Paths.get(uploadDir, filename);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, request.getImage().getBytes());
        } catch (IOException e) {
            throw new FileStorageException("Günlük soru görseli kaydedilemedi.", e);
        }
        String imageUrl = "/api/files/" + filename;

        // Yeni soru oluştur
        DailyQuestion question = new DailyQuestion();
        question.setImageUrl(imageUrl);
        question.setAnswerNumber(request.getAnswerNumber());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setVideoUrl(request.getVideoUrl());
        question.setExplanation(request.getExplanation());
        question.setDisplayDate(request.getDisplayDate());
        question.setStartTime(request.getStartTime());
        question.setEndTime(request.getEndTime());
        question.setActive(false); // Başlangıçta pasif, scheduler aktifleştirecek

        return dailyQuestionRepository.save(question);
    }

    private DailyQuestionResponse toDailyQuestionResponse(DailyQuestion question, Long userId) {
        DailyQuestionResponse response = new DailyQuestionResponse();
        response.setId(question.getId());
        response.setImageUrl(question.getImageUrl());
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
}
