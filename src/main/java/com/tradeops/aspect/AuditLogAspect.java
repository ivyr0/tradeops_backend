package com.tradeops.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tradeops.annotation.Auditable;
import com.tradeops.model.entity.AuditLog;
import com.tradeops.repo.AuditLogRepo;
import com.tradeops.service.audit.AuditIdentityResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepo auditLogRepo;
    private final AuditIdentityResolver identityResolver;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            AuditIdentityResolver.AuditIdentity identity = identityResolver.getCurrentIdentity();

            Long entityId = extractEntityId(result);
            String diffJson = objectMapper.writeValueAsString(result);

            AuditLog auditLog = new AuditLog();
            auditLog.setActorType(identity.actorType());
            auditLog.setActorId(identity.actorId());
            auditLog.setAction(auditable.action());
            auditLog.setEntityType(auditable.entityType());
            auditLog.setEntityId(entityId);
            auditLog.setDiffJson(diffJson);

            auditLogRepo.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to save audit log for action: {}", auditable.action(), e);
        }
    }

    private Long extractEntityId(Object entity) {
        if (entity == null)
            return null;
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return id != null ? Long.valueOf(id.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
