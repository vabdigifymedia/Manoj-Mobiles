package com.api.manojmobiles.service.sms;

import com.api.manojmobiles.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsProperties smsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send an SMS asynchronously using TD Digital Solution HTTP Token API.
     * 
     * @param number The destination mobile number.
     * @param template The DLT approved SmsTemplate enum.
     * @param templateArgs The arguments to replace in the template format string.
     */
    @Async
    public void sendSms(String number, SmsTemplate template, Object... templateArgs) {
        if (smsProperties.getAuthKey() == null || smsProperties.getAuthKey().isEmpty()) {
            log.warn("SMS sending skipped: TD Digital Solution auth key is not configured.");
            return;
        }

        try {
            String message = template.formatMessage(templateArgs);
            String url = UriComponentsBuilder.fromUriString(smsProperties.getBaseUrl() + "/http-tokenkeyapi.php")
                    .queryParam("authentic-key", smsProperties.getAuthKey())
                    .queryParam("senderid", smsProperties.getSenderId())
                    .queryParam("route", smsProperties.getDomesticRoute())
                    .queryParam("number", number)
                    .queryParam("message", message)
                    .queryParam("templateid", template.getTemplateId())
                    .build()
                    .toUriString();

            log.info("Sending SMS to number: {} with template: {}", maskPhone(number), template.name());
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS successfully sent to {}. Response: {}", maskPhone(number), response.getBody());
            } else {
                log.error("Failed to send SMS to {}. Status: {}, Response: {}", 
                          maskPhone(number), response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Exception occurred while sending SMS to {}: {}", maskPhone(number), e.getMessage());
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 4);
    }
}
