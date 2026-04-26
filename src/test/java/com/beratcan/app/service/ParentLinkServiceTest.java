package com.beratcan.app.service;

import com.beratcan.app.dto.response.StudentSummaryDto;
import com.beratcan.app.entity.ParentProfile;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.repository.ParentProfileRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentLinkServiceTest {

    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;

    @InjectMocks private ParentLinkService service;

    private ParentProfile parent;
    private StudentProfile student;

    @BeforeEach
    void setUp() {
        User parentUser = new User();
        parentUser.setId(10L);
        parent = new ParentProfile();
        parent.setId(1L);
        parent.setUser(parentUser);
        parent.setChildren(new HashSet<>());

        User studentUser = new User();
        studentUser.setId(20L);
        studentUser.setFirstName("Ali");
        studentUser.setLastName("Yılmaz");
        studentUser.setUsername("aliy");
        student = new StudentProfile();
        student.setId(2L);
        student.setUser(studentUser);
        student.setGradeLevel(GradeLevel.G11);
        student.setParentInviteCode("ABC123");
    }

    @Test
    void getChildren_returnsLinkedStudentSummaries() {
        parent.getChildren().add(student);
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));

        List<StudentSummaryDto> result = service.getChildren(10L);

        assertEquals(1, result.size());
        StudentSummaryDto dto = result.get(0);
        assertEquals(2L, dto.getStudentProfileId());
        assertEquals("Ali", dto.getFirstName());
        assertEquals("Yılmaz", dto.getLastName());
        assertEquals("aliy", dto.getUsername());
        assertEquals("G11", dto.getGradeLevel());
    }

    @Test
    void linkChildByCode_validCode_addsChildToParent() {
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));
        when(studentProfileRepository.findByParentInviteCode("ABC123")).thenReturn(Optional.of(student));

        StudentSummaryDto result = service.linkChildByCode(10L, "ABC123");

        assertEquals(2L, result.getStudentProfileId());
        assertTrue(parent.getChildren().contains(student));
    }

    @Test
    void linkChildByCode_isCaseInsensitiveAndTrimsWhitespace() {
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));
        when(studentProfileRepository.findByParentInviteCode("ABC123")).thenReturn(Optional.of(student));

        service.linkChildByCode(10L, "  abc123  ");

        verify(studentProfileRepository).findByParentInviteCode("ABC123");
    }

    @Test
    void linkChildByCode_secondLinkSameChild_isIdempotent() {
        parent.getChildren().add(student);
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));
        when(studentProfileRepository.findByParentInviteCode("ABC123")).thenReturn(Optional.of(student));

        StudentSummaryDto result = service.linkChildByCode(10L, "ABC123");

        assertEquals(2L, result.getStudentProfileId());
        assertEquals(1, parent.getChildren().size()); // Set: duplicate yok
    }

    @Test
    void linkChildByCode_invalidCode_throwsResourceNotFound() {
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));
        when(studentProfileRepository.findByParentInviteCode("XXXXXX")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.linkChildByCode(10L, "XXXXXX"));
    }

    @Test
    void linkChildByCode_blankCode_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.linkChildByCode(10L, "   "));
        assertThrows(BadRequestException.class,
                () -> service.linkChildByCode(10L, null));
    }

    @Test
    void linkChildByCode_parentProfileMissing_throwsBadRequest() {
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.linkChildByCode(10L, "ABC123"));
    }

    @Test
    void unlinkChild_existingChild_removesFromParent() {
        parent.getChildren().add(student);
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));

        service.unlinkChild(10L, 2L);

        assertTrue(parent.getChildren().isEmpty());
    }

    @Test
    void unlinkChild_notLinked_throwsResourceNotFound() {
        when(parentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(parent));

        assertThrows(ResourceNotFoundException.class,
                () -> service.unlinkChild(10L, 999L));
    }
}
