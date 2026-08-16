package ai.repository;

import ai.dto.EmailRequest;

public interface EmailService {

    void send(EmailRequest request);
}
