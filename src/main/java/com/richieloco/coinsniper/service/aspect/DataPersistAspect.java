package com.richieloco.coinsniper.service.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataPersistAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataPersistAspect.class);

    @Before("execution(* com.example.repository.*.save(..))")
    public void beforeCreate(JoinPoint joinPoint) {
        logger.info("Creating entity: {}", joinPoint.getArgs()[0]);
    }

    @Before("execution(* com.example.repository.*.find*(..))")
    public void beforeRead(JoinPoint joinPoint) {
        logger.info("Reading entity with args: {}", joinPoint.getArgs());
    }

    @Before("execution(* com.example.repository.*.update(..))")
    public void beforeUpdate(JoinPoint joinPoint) {
        logger.info("Updating entity: {}", joinPoint.getArgs()[0]);
    }

    @Before("execution(* com.example.repository.*.delete*(..))")
    public void beforeDelete(JoinPoint joinPoint) {
        logger.info("Deleting entity with args: {}", joinPoint.getArgs());
    }

    @AfterReturning(pointcut = "execution(* com.example.repository.*.*(..))", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        logger.info("Method {} executed successfully, returned: {}", joinPoint.getSignature().getName(), result);
    }

    @AfterThrowing(pointcut = "execution(* com.example.repository.*.*(..))", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {
        logger.error("Method {} threw an exception: {}", joinPoint.getSignature().getName(), ex.getMessage());
    }
}
