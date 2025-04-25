package com.richieloco.coinsniper.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T readFromFile(String fileName, Class<T> valueType) {
        logger.info("Reading JSON from classpath resource: {}", fileName);

        try (InputStream inputStream = getResourceAsStream(fileName)) {
            if (inputStream == null) {
                logger.error("Resource not found: {}", fileName);
                return null;
            }
            T result = mapper.readValue(inputStream, valueType);
            logger.info("Successfully parsed JSON into {}", valueType.getSimpleName());
            return result;
        } catch (IOException e) {
            logger.error("Failed to read JSON from resource: {}", fileName, e);
            return null;
        }
    }

    private static InputStream getResourceAsStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }
}
