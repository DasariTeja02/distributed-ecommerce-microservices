package com.ecommerce.notification;

import com.ecommerce.notification.controller.NotificationController;
import com.ecommerce.notification.event.NotificationDispatchedEvent;
import com.ecommerce.notification.listener.NotificationKafkaListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationKafkaListener notificationKafkaListener;

    @Test
    @DisplayName("GET /api/notifications should return list of dispatched notifications")
    void shouldReturnAllNotifications() throws Exception {
        NotificationDispatchedEvent event = new NotificationDispatchedEvent(
                "evt-123",
                "NotificationDispatchedEvent",
                UUID.randomUUID(),
                "customer1@example.com",
                "EMAIL",
                "SENT",
                Instant.now(),
                "corr-123"
        );
        when(notificationKafkaListener.getDispatchedHistory()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-123"))
                .andExpect(jsonPath("$[0].recipient").value("customer1@example.com"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/notifications/customer/{customerId} should filter by recipient")
    void shouldReturnCustomerNotifications() throws Exception {
        NotificationDispatchedEvent event = new NotificationDispatchedEvent(
                "evt-123",
                "NotificationDispatchedEvent",
                UUID.randomUUID(),
                "cust-001",
                "EMAIL",
                "SENT",
                Instant.now(),
                "corr-123"
        );
        when(notificationKafkaListener.getDispatchedHistory()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/notifications/customer/cust-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipient").value("cust-001"));
    }
}
