package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.dto.response.ActiveClassroomMembershipDto;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.*;
import com.beratcan.app.exception.ActiveClassroomMembershipException;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final SuperAdminProfileRepository superAdminProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final ClassroomService classroomService;

    public static final Pattern PHONE_PATTERN = Pattern.compile(AppConstants.PHONE_NUMBER_PATTERN);

    public UserService(UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            AdminProfileRepository adminProfileRepository,
            ParentProfileRepository parentProfileRepository,
            SuperAdminProfileRepository superAdminProfileRepository,
            BCryptPasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            InviteCodeGenerator inviteCodeGenerator,
            @Lazy ClassroomService classroomService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.superAdminProfileRepository = superAdminProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.classroomService = classroomService;
    }

    @Transactional
    public User registerUser(UserRequest dto) {
        if (dto == null) {
            throw new BadRequestException("User request cannot be null");
        }

        log.info("User registration started: username={}, role={}",
                dto.getUsername(), dto.getRole());

        // Temel alan kontrolleri
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email boş olamaz");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Kullanıcı adı boş olamaz");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Şifre boş olamaz");
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            throw new BadRequestException("Telefon numarası boş olamaz");
        }

        // Email format doğrulama ve normalize et
        String email = dto.getEmail().trim().toLowerCase();
        if (!isValidEmailFormat(email)) {
            log.warn("Invalid email format: username={}", dto.getUsername());
            throw new BadRequestException("Geçersiz email formatı. Lütfen geçerli bir email adresi giriniz.");
        }

        // Telefon doğrulama
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            log.warn("Invalid phone number format: username={}, phone={}", dto.getUsername(), dto.getPhoneNumber());
            throw new BadRequestException("Geçersiz telefon numarası formatı.");
        }

        // Benzersizlik kontrolleri
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed - email already exists: username={}", dto.getUsername());
            throw new ConflictException("Bu email zaten kayıtlı.");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            log.warn("Registration failed - phone number already exists: phone={}", dto.getPhoneNumber());
            throw new ConflictException("Bu telefon numarası zaten kayıtlı.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.warn("Registration failed - username already exists: username={}", dto.getUsername());
            throw new ConflictException("Bu kullanıcı adı zaten kayıtlı.");
        }

        // User oluştur
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(email); // Lowercase ve trim edilmiş email
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // şifreyi hashle
        Role role;
        try {
            role = Role.valueOf(dto.getRole().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Geçersiz rol.");
        }
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            throw new BadRequestException("Admin/SuperAdmin hesabı bu endpoint üzerinden oluşturulamaz.");
        }
        user.setRole(role);

        user.setGender(dto.getGender() != null ? Gender.valueOf(dto.getGender()) : Gender.DIGER);
        user.setCity(dto.getCity() != null ? City.valueOf(dto.getCity()) : City.Diğer);

        // Email doğrulama - başlangıçta false
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}, username={}, role={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        // Role'e göre profil oluştur
        createProfileForRole(savedUser, dto);

        // Email doğrulama email'i gönder
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
            log.info("Verification email sent to new user: userId={}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email during registration: userId={}",
                    savedUser.getId(), e);
        }

        return savedUser;
    }

    /**
     * Kullanıcı kimlik bilgilerini doğrular (email/username + password)
     * 
     * @param request Login request (identifier, password)
     * @return User entity (doğrulama başarılıysa)
     * @throws BadRequestException   Şifre yanlışsa
     * @throws UserNotFoundException Kullanıcı bulunamazsa
     */
    public User validateUserCredentials(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Login request cannot be null");
        }
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new BadRequestException("Identifier (username or email) cannot be null or empty");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password cannot be null or empty");
        }

        log.info("Login attempt: identifier={}", request.getIdentifier());

        Optional<User> userOpt;

        if (request.getIdentifier().contains("@")) {
            userOpt = userRepository.findByEmail(request.getIdentifier());
        } else {
            userOpt = userRepository.findByUsername(request.getIdentifier());
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("Login failed - user not found: identifier={}", request.getIdentifier());
            return new UserNotFoundException("Kullanıcı bulunamadı.");
        });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password: userId={}, username={}", user.getId(), user.getUsername());
            throw new BadRequestException("Geçersiz şifre.");
        }

        log.info("Login successful: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());

        // Login başarılı → User döndür (token controller'da oluşturulacak)
        return user;
    }

    // Kullanıcı bilgilerini getir
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Kullanıcının kendi hesabını sil
     * Cascade ilişkiler sayesinde tüm ilişkili veriler (testResults, cart,
     * purchases, bookReviews) otomatik silinir
     * ManyToMany ilişkiler (ownedBooks, flaggedQuestions) join table'dan otomatik
     * temizlenir
     * 
     * @param userId Silinecek kullanıcının ID'si
     * @throws UserNotFoundException Kullanıcı bulunamazsa
     */
    @Transactional
    public void deleteOwnAccount(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        log.info("User account deletion started: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Account deletion failed - user not found: userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        // TEACHER ise aktif sınıf üyeliklerini kontrol et — varsa silme engellenir
        if (user.getRole() == Role.TEACHER) {
            List<ActiveClassroomMembershipDto> activeMemberships =
                    classroomService.findActiveMembershipsForTeacher(userId);
            if (!activeMemberships.isEmpty()) {
                log.warn("Teacher account deletion blocked - active classrooms: userId={}, count={}",
                        userId, activeMemberships.size());
                throw new ActiveClassroomMembershipException(
                        "Hesabınızı silmek için önce aktif sınıflarınızdan ayrılın veya kapatın",
                        activeMemberships);
            }
        }

        // Kullanıcıyı fiziki olarak silmiyoruz (Soft Delete devrede)
        // Sadece kişisel verilerini (GDPR uyumu için) anonimleştiriyoruz.
        String randomSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        user.setUsername("deleted_" + randomSuffix);
        user.setEmail("deleted_" + java.util.UUID.randomUUID().toString() + "@kitapp.local");
        user.setPhoneNumber("del_" + randomSuffix);
        user.setFirstName("Deleted");
        user.setLastName("User");
        
        // delete() metodu entitiy üzerindeki @SQLDelete sayesinde "UPDATE is_deleted=true" yapacak.
        userRepository.delete(user);

        log.info("User account soft-deleted and anonymized successfully: userId={}", userId);
    }

    /**
     * SuperAdmin tarafından Admin veya SuperAdmin hesabı oluşturma.
     * Sadece ADMIN ve SUPER_ADMIN rolleri bu metotla oluşturulabilir.
     */
    @Transactional
    public User createUserBySuperAdmin(UserRequest dto) {
        if (dto == null) {
            throw new BadRequestException("User request cannot be null");
        }

        Role role;
        try {
            role = Role.valueOf(dto.getRole().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Geçersiz rol.");
        }

        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            throw new BadRequestException("Bu endpoint sadece ADMIN ve SUPER_ADMIN hesapları oluşturmak içindir.");
        }

        log.info("SuperAdmin creating user: username={}, role={}", dto.getUsername(), role);

        // Temel alan kontrolleri
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email boş olamaz");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Kullanıcı adı boş olamaz");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Şifre boş olamaz");
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            throw new BadRequestException("Telefon numarası boş olamaz");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (!isValidEmailFormat(email)) {
            throw new BadRequestException("Geçersiz email formatı.");
        }
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw new BadRequestException("Geçersiz telefon numarası formatı.");
        }

        // Benzersizlik kontrolleri
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Bu email zaten kayıtlı.");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new ConflictException("Bu telefon numarası zaten kayıtlı.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ConflictException("Bu kullanıcı adı zaten kayıtlı.");
        }

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(email);
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(role);
        user.setGender(dto.getGender() != null ? Gender.valueOf(dto.getGender()) : Gender.DIGER);
        user.setCity(dto.getCity() != null ? City.valueOf(dto.getCity()) : City.Diğer);
        user.setEmailVerified(true); // Admin/SuperAdmin hesapları doğrulanmış olarak oluşturulur

        User savedUser = userRepository.save(user);

        // Profil oluştur
        createProfileForRole(savedUser, dto);

        log.info("User created by SuperAdmin: userId={}, username={}, role={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        return savedUser;
    }

    /**
     * Role'e göre ilgili profil tablosuna kayıt oluştur
     */
    private void createProfileForRole(User user, UserRequest dto) {
        switch (user.getRole()) {
            case STUDENT -> {
                StudentProfile profile = new StudentProfile();
                profile.setUser(user);
                profile.setParentInviteCode(
                        inviteCodeGenerator.generateUnique(studentProfileRepository::existsByParentInviteCode));

                // ExamType (opsiyonel)
                if (dto.getExamType() != null) {
                    try {
                        profile.setExamType(ExamType.valueOf(dto.getExamType().toUpperCase()));
                    } catch (Exception e) {
                        throw new BadRequestException("Geçersiz sınav türü.");
                    }

                    // GradeLevel (sınav seçildiyse ekle)
                    if (dto.getGradeLevel() != null) {
                        try {
                            profile.setGradeLevel(GradeLevel.valueOf(dto.getGradeLevel().toUpperCase()));
                        } catch (Exception e) {
                            throw new BadRequestException("Geçersiz sınıf seviyesi.");
                        }
                    }

                    // YKS ise fieldType ve schoolType zorunlu
                    if ("YKS".equalsIgnoreCase(dto.getExamType())) {
                        if (dto.getFieldType() == null || dto.getSchoolType() == null) {
                            throw new BadRequestException("YKS için alan ve lise türü zorunludur.");
                        }
                        try {
                            profile.setFieldType(FieldType.valueOf(dto.getFieldType().toUpperCase()));
                        } catch (Exception e) {
                            throw new BadRequestException("Geçersiz alan türü.");
                        }
                        try {
                            profile.setSchoolType(SchoolType.valueOf(dto.getSchoolType()));
                        } catch (Exception e) {
                            throw new BadRequestException("Geçersiz lise türü.");
                        }
                    }
                }

                studentProfileRepository.save(profile);
                log.info("StudentProfile created for userId={}", user.getId());
            }
            case TEACHER -> {
                TeacherProfile profile = new TeacherProfile();
                profile.setUser(user);
                teacherProfileRepository.save(profile);
                log.info("TeacherProfile created for userId={}", user.getId());
            }
            case PARENT -> {
                ParentProfile profile = new ParentProfile();
                profile.setUser(user);
                parentProfileRepository.save(profile);
                log.info("ParentProfile created for userId={}", user.getId());
            }
            case ADMIN -> {
                AdminProfile profile = new AdminProfile();
                profile.setUser(user);
                adminProfileRepository.save(profile);
                log.info("AdminProfile created for userId={}", user.getId());
            }
            case SUPER_ADMIN -> {
                SuperAdminProfile profile = new SuperAdminProfile();
                profile.setUser(user);
                superAdminProfileRepository.save(profile);
                log.info("SuperAdminProfile created for userId={}", user.getId());
            }
        }
    }

    /**
     * Email formatını doğrula
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Temel format kontrolü
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return false;
        }
        // Email çok uzun olmamalı
        if (email.length() > 100) {
            return false;
        }
        // @ işaretinden önce ve sonra karakter olmalı
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }
        // Domain kısmında en az bir nokta olmalı
        String domain = email.substring(atIndex + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }
        return true;
    }
}
