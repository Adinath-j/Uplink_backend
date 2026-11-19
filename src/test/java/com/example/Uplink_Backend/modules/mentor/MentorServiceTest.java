package com.example.Uplink_Backend.modules.mentor;

import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.mentor.dto.BookingResponseDto;
import com.example.Uplink_Backend.modules.mentor.dto.CreateBookingRequest;
import com.example.Uplink_Backend.modules.mentor.dto.MentorProfileDto;
import com.example.Uplink_Backend.modules.mentor.entity.MentorBooking;
import com.example.Uplink_Backend.modules.mentor.entity.MentorProfile;
import com.example.Uplink_Backend.modules.mentor.repository.MentorBookingRepository;
import com.example.Uplink_Backend.modules.mentor.repository.MentorProfileRepository;
import com.example.Uplink_Backend.modules.mentor.service.MentorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MentorServiceTest {

    @Mock
    private MentorProfileRepository mentorRepository;

    @Mock
    private MentorBookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MentorService mentorService;

    private User sampleStudent;
    private MentorProfile sampleMentor;
    private UUID studentId;
    private UUID mentorId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        mentorId = UUID.randomUUID();

        sampleStudent = new User();
        sampleStudent.setId(studentId);
        sampleStudent.setFullName("Aakash Nair");

        sampleMentor = new MentorProfile("Dr. Arvind Sharma", "Principal Architect", "Google Cloud",
                "https://example.com/avatar.png", "Bio", "Engineering, System Design",
                "Kubernetes, Java", 14, "Mon, Wed");
        sampleMentor.setId(mentorId);
    }

    @Test
    void testGetMentors() {
        when(mentorRepository.searchMentors(any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleMentor)));

        Page<MentorProfileDto> page = mentorService.getMentors("Engineering", null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("Dr. Arvind Sharma", page.getContent().get(0).getName());
        assertEquals("Google Cloud", page.getContent().get(0).getOrganization());
    }

    @Test
    void testRequestBooking() {
        when(userRepository.findById(studentId)).thenReturn(Optional.of(sampleStudent));
        when(mentorRepository.findById(mentorId)).thenReturn(Optional.of(sampleMentor));

        MentorBooking booking = new MentorBooking(sampleMentor, sampleStudent, "Dr. Arvind Sharma",
                "Principal Architect, Google Cloud", "Friday 4 PM", "System Design Mock", "Please review my distributed cache design");
        booking.setId(UUID.randomUUID());

        when(bookingRepository.save(any(MentorBooking.class))).thenReturn(booking);

        CreateBookingRequest req = new CreateBookingRequest("Friday 4 PM", "System Design Mock", "Please review my distributed cache design");
        BookingResponseDto res = mentorService.requestBooking(studentId, mentorId, req);

        assertNotNull(res);
        assertEquals("Dr. Arvind Sharma", res.getMentorName());
        assertEquals("REQUESTED", res.getStatus());
        verify(bookingRepository, times(1)).save(any(MentorBooking.class));
    }
}

