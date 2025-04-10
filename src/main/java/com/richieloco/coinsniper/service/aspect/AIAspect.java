package com.richieloco.coinsniper.service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AIAspect {

    private static final Logger logger = LoggerFactory.getLogger(AIAspect.class);

    @Around("execution(* com.crypto.trader.service.AiRiskManagementService.*(..))")
    public Object logAndEnhanceRiskManagement(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("AI Risk Management called: " + joinPoint.getSignature().getName());
        Object result = joinPoint.proceed();
        logger.info("AI Risk assessment result: " + result);
        return result;
    }
}
