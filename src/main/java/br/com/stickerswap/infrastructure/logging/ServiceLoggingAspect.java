package br.com.stickerswap.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logServiceCall(ProceedingJoinPoint pjp) throws Throwable {
        String primitive = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();

        log.debug("[primitive: {}] enter", primitive);
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.debug("[primitive: {}] completed in {}ms", primitive, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("[primitive: {}] threw {}: {}", primitive, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
