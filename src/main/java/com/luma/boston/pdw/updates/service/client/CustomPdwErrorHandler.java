package com.luma.boston.pdw.updates.service.client;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luma.pdw.exception.PdwValidationException;
import com.luma.pdw.model.error.BadRequestResult;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@CustomLog
@Component
public class CustomPdwErrorHandler implements ErrorDecoder {

    private final ErrorDecoder errorDecoder = new Default();

    @SneakyThrows
    @Override
    public Exception decode(String s, Response response) {

        if (response != null && 400 == response.status()) {
            StringBuffer resultMsg = new StringBuffer();
            try (var reader = new BufferedReader(new InputStreamReader(response.body().asInputStream()))){
                var text  = reader.lines().collect(Collectors.joining("\n"));
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                BadRequestResult badRequestResult = mapper.readValue(text, BadRequestResult.class);
                if (badRequestResult.getViolations() != null) {
                    badRequestResult.getViolations().stream().forEach(violation -> resultMsg.append(violation.getMessage() + ", "));
                }
            } catch (Exception ex) {
                log.error("Error parsing 400 result from pdw", ex);
            }

            String validationMsg = (resultMsg.length() > 0) ? resultMsg.substring(0, resultMsg.length() - 2) : null;
//            log.error("Error saving product: {}", validationMsg);
            return new PdwValidationException("Http status 400 (Bad request) returned from pdw", validationMsg);
        }

        return errorDecoder.decode(s, response);
    }
}
