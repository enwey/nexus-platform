package com.nexus.platform.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.DeveloperSupportTicketCreateRequest;
import com.nexus.platform.dto.OpsSupportTicketDto;
import com.nexus.platform.dto.OpsSupportTicketMessageDto;
import com.nexus.platform.dto.OpsSupportTicketStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.SupportTicketMessageCreateRequest;
import com.nexus.platform.entity.OpsSupportTicket;
import com.nexus.platform.entity.OpsSupportTicketMessage;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsSupportTicketMessageRepository;
import com.nexus.platform.repository.OpsSupportTicketRepository;
import com.nexus.platform.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpsSupportTicketService {
    private static final DateTimeFormatter TICKET_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OpsSupportTicketRepository ticketRepository;
    private final OpsSupportTicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public Result<List<OpsSupportTicketDto>> listDeveloperTickets(User currentUser) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER) {
            return Result.error("Developer account required");
        }
        return Result.success(toTicketDtos(ticketRepository.findByDeveloperIdOrderByUpdatedAtDesc(currentUser.getId())));
    }

    @Transactional
    public Result<OpsSupportTicketDto> createDeveloperTicket(
            User currentUser,
            DeveloperSupportTicketCreateRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsSupportTicketDto.class);
        return idempotencyService.execute(
                "DEV_SUPPORT_TICKET_CREATE",
                idempotencyKey,
                request,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createDeveloperTicketInternal(currentUser, request, requestUri)
        );
    }

    private Result<OpsSupportTicketDto> createDeveloperTicketInternal(User currentUser, DeveloperSupportTicketCreateRequest request, String requestUri) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER) {
            return Result.error("Developer account required");
        }
        String ticketType = normalizeTicketType(request == null ? null : request.ticketType());
        String priority = normalizePriority(request == null ? null : request.priority());
        String title = trimToNull(request == null ? null : request.title());
        String content = trimToNull(request == null ? null : request.content());
        String relatedAppId = trimToNull(request == null ? null : request.relatedAppId());
        if (ticketType == null) {
            return Result.error("Invalid ticket type");
        }
        if (priority == null) {
            return Result.error("Invalid priority");
        }
        if (title == null || title.length() < 4 || title.length() > 128) {
            return Result.error("Title must be 4-128 chars");
        }
        if (content == null || content.length() < 8 || content.length() > 2000) {
            return Result.error("Content must be 8-2000 chars");
        }
        if (relatedAppId != null && relatedAppId.length() > 64) {
            return Result.error("Related app id is too long");
        }

        OpsSupportTicket ticket = new OpsSupportTicket();
        ticket.setDeveloperId(currentUser.getId());
        ticket.setTicketNo(generateTicketNo(currentUser.getId()));
        ticket.setTicketType(ticketType);
        ticket.setPriority(priority);
        ticket.setTitle(title);
        ticket.setContent(content);
        ticket.setRelatedAppId(relatedAppId);
        ticket.setTicketStatus("OPEN");
        ticket.setLastReplyAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        OpsSupportTicketMessage message = new OpsSupportTicketMessage();
        message.setTicketId(ticket.getId());
        message.setSenderId(currentUser.getId());
        message.setSenderRole(currentUser.getRole().name());
        message.setMessageType("OPENING");
        message.setContent(content);
        messageRepository.save(message);

        auditLogService.logOpsAudit("SUPPORT_TICKET_CREATE", currentUser, null, "ticket:" + ticket.getTicketNo(), true, title, requestUri);
        return Result.success(toTicketDto(ticket, Map.of(currentUser.getId(), currentUser)));
    }

    public Result<List<OpsSupportTicketMessageDto>> listDeveloperMessages(User currentUser, Long ticketId) {
        OpsSupportTicket ticket = requireDeveloperTicket(currentUser, ticketId);
        if (ticket == null) {
            return Result.error("Ticket not found");
        }
        return Result.success(toMessageDtos(messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)));
    }

    @Transactional
    public Result<OpsSupportTicketMessageDto> createDeveloperMessage(
            User currentUser,
            Long ticketId,
            SupportTicketMessageCreateRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsSupportTicketMessageDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("request", request);
        return idempotencyService.execute(
                "DEV_SUPPORT_TICKET_REPLY",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createDeveloperMessageInternal(currentUser, ticketId, request, requestUri)
        );
    }

    private Result<OpsSupportTicketMessageDto> createDeveloperMessageInternal(
            User currentUser,
            Long ticketId,
            SupportTicketMessageCreateRequest request,
            String requestUri
    ) {
        OpsSupportTicket ticket = requireDeveloperTicket(currentUser, ticketId);
        if (ticket == null) {
            return Result.error("Ticket not found");
        }
        if ("RESOLVED".equals(ticket.getTicketStatus()) || "CLOSED".equals(ticket.getTicketStatus())) {
            return Result.error("Closed ticket cannot receive replies");
        }
        String content = trimToNull(request == null ? null : request.content());
        if (content == null || content.length() < 2 || content.length() > 2000) {
            return Result.error("Reply must be 2-2000 chars");
        }
        OpsSupportTicketMessage message = new OpsSupportTicketMessage();
        message.setTicketId(ticket.getId());
        message.setSenderId(currentUser.getId());
        message.setSenderRole(currentUser.getRole().name());
        message.setMessageType("REPLY");
        message.setContent(content);
        messageRepository.save(message);
        ticket.setLastReplyAt(LocalDateTime.now());
        if ("WAITING_DEVELOPER".equals(ticket.getTicketStatus())) {
            ticket.setTicketStatus("IN_PROGRESS");
        }
        ticketRepository.save(ticket);
        auditLogService.logOpsAudit("SUPPORT_TICKET_REPLY", currentUser, null, "ticket:" + ticket.getTicketNo(), true, "Developer reply", requestUri);
        return Result.success(toMessageDto(message));
    }

    public Result<List<OpsSupportTicketDto>> listOpsTickets() {
        return Result.success(toTicketDtos(ticketRepository.findAll().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .toList()));
    }

    public Result<List<OpsSupportTicketMessageDto>> listOpsMessages(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            return Result.error("Ticket not found");
        }
        return Result.success(toMessageDtos(messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)));
    }

    @Transactional
    public Result<OpsSupportTicketDto> updateOpsTicketStatus(
            Long ticketId,
            OpsSupportTicketStatusRequest request,
            User currentUser,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsSupportTicketDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_SUPPORT_TICKET_STATUS_UPDATE",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> updateOpsTicketStatusInternal(ticketId, request, currentUser, requestUri)
        );
    }

    private Result<OpsSupportTicketDto> updateOpsTicketStatusInternal(Long ticketId, OpsSupportTicketStatusRequest request, User currentUser, String requestUri) {
        OpsSupportTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return Result.error("Ticket not found");
        }
        String status = normalizeTicketStatus(request == null ? null : request.ticketStatus());
        if (status == null) {
            return Result.error("Invalid ticket status");
        }
        Long assigneeAdminId = request == null ? null : request.assigneeAdminId();
        if (assigneeAdminId != null) {
            User assignee = userRepository.findById(assigneeAdminId).orElse(null);
            if (assignee == null || assignee.getRole() != User.UserRole.ADMIN) {
                return Result.error("Invalid assignee admin");
            }
        }
        if (!isAllowedStatusTransition(ticket.getTicketStatus(), status)) {
            return Result.error("Illegal ticket status transition");
        }
        String resolutionSummary = trimToNull(request == null ? null : request.resolutionSummary());
        if (resolutionSummary != null && resolutionSummary.length() > 256) {
            return Result.error("Resolution summary is too long");
        }
        if (("RESOLVED".equals(status) || "CLOSED".equals(status)) && (resolutionSummary == null || resolutionSummary.length() < 2)) {
            return Result.error("Resolution summary must be at least 2 chars");
        }
        ticket.setTicketStatus(status);
        ticket.setAssigneeAdminId(assigneeAdminId);
        ticket.setResolutionSummary(resolutionSummary);
        ticket.setLastReplyAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        auditLogService.logOpsAudit("SUPPORT_TICKET_STATUS_UPDATE", currentUser, null, "ticket:" + ticket.getTicketNo(), true, status, requestUri);
        return Result.success(toTicketDto(ticket, loadUsersByIds(List.of(ticket.getDeveloperId(), assigneeAdminId))));
    }

    @Transactional
    public Result<OpsSupportTicketMessageDto> createOpsMessage(
            Long ticketId,
            SupportTicketMessageCreateRequest request,
            User currentUser,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsSupportTicketMessageDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_SUPPORT_TICKET_REPLY",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createOpsMessageInternal(ticketId, request, currentUser, requestUri)
        );
    }

    private Result<OpsSupportTicketMessageDto> createOpsMessageInternal(
            Long ticketId,
            SupportTicketMessageCreateRequest request,
            User currentUser,
            String requestUri
    ) {
        OpsSupportTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return Result.error("Ticket not found");
        }
        if ("RESOLVED".equals(ticket.getTicketStatus()) || "CLOSED".equals(ticket.getTicketStatus())) {
            return Result.error("Closed ticket cannot receive replies");
        }
        String content = trimToNull(request == null ? null : request.content());
        if (content == null || content.length() < 2 || content.length() > 2000) {
            return Result.error("Reply must be 2-2000 chars");
        }
        OpsSupportTicketMessage message = new OpsSupportTicketMessage();
        message.setTicketId(ticket.getId());
        message.setSenderId(currentUser.getId());
        message.setSenderRole(currentUser.getRole().name());
        message.setMessageType("REPLY");
        message.setContent(content);
        messageRepository.save(message);
        ticket.setLastReplyAt(LocalDateTime.now());
        if ("OPEN".equals(ticket.getTicketStatus()) || "WAITING_DEVELOPER".equals(ticket.getTicketStatus())) {
            ticket.setTicketStatus("IN_PROGRESS");
        }
        ticketRepository.save(ticket);
        auditLogService.logOpsAudit("SUPPORT_TICKET_REPLY", currentUser, null, "ticket:" + ticket.getTicketNo(), true, "Ops reply", requestUri);
        return Result.success(toMessageDto(message));
    }

    private OpsSupportTicket requireDeveloperTicket(User currentUser, Long ticketId) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER || ticketId == null || ticketId <= 0) {
            return null;
        }
        return ticketRepository.findByIdAndDeveloperId(ticketId, currentUser.getId()).orElse(null);
    }

    private List<OpsSupportTicketDto> toTicketDtos(List<OpsSupportTicket> tickets) {
        List<Long> userIds = tickets.stream()
                .flatMap(ticket -> java.util.stream.Stream.of(ticket.getDeveloperId(), ticket.getAssigneeAdminId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> users = loadUsersByIds(userIds);
        return tickets.stream().map(ticket -> toTicketDto(ticket, users)).toList();
    }

    private OpsSupportTicketDto toTicketDto(OpsSupportTicket ticket, Map<Long, User> users) {
        User developer = users.get(ticket.getDeveloperId());
        return new OpsSupportTicketDto(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getDeveloperId(),
                developer == null ? null : developer.getUsername(),
                developer == null ? null : developer.getEmail(),
                ticket.getTicketType(),
                ticket.getPriority(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getTicketStatus(),
                ticket.getRelatedAppId(),
                ticket.getAssigneeAdminId(),
                ticket.getResolutionSummary(),
                ticket.getLastReplyAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private List<OpsSupportTicketMessageDto> toMessageDtos(List<OpsSupportTicketMessage> messages) {
        return messages.stream().map(this::toMessageDto).toList();
    }

    private OpsSupportTicketMessageDto toMessageDto(OpsSupportTicketMessage message) {
        return new OpsSupportTicketMessageDto(
                message.getId(),
                message.getTicketId(),
                message.getSenderId(),
                message.getSenderRole(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private Map<Long, User> loadUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String generateTicketNo(Long developerId) {
        return "TK" + LocalDateTime.now().format(TICKET_NO_FORMATTER) + String.format("%04d", Math.abs((developerId == null ? 0 : developerId.intValue()) % 10000));
    }

    private boolean isAllowedStatusTransition(String current, String target) {
        String normalizedCurrent = normalizeTicketStatus(current);
        return switch (normalizedCurrent == null ? "OPEN" : normalizedCurrent) {
            case "OPEN" -> List.of("IN_PROGRESS", "WAITING_DEVELOPER", "RESOLVED", "CLOSED").contains(target);
            case "IN_PROGRESS" -> List.of("WAITING_DEVELOPER", "RESOLVED", "CLOSED").contains(target);
            case "WAITING_DEVELOPER" -> List.of("IN_PROGRESS", "RESOLVED", "CLOSED").contains(target);
            case "RESOLVED" -> List.of("CLOSED").contains(target);
            case "CLOSED" -> false;
            default -> false;
        };
    }

    private String normalizeTicketType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REVIEW", "RELEASE", "RUNTIME", "BILLING", "GOVERNANCE", "OTHER" -> normalized;
            default -> null;
        };
    }

    private String normalizePriority(String value) {
        if (value == null || value.isBlank()) {
            return "NORMAL";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "NORMAL", "HIGH", "URGENT" -> normalized;
            default -> null;
        };
    }

    private String normalizeTicketStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OPEN", "IN_PROGRESS", "WAITING_DEVELOPER", "RESOLVED", "CLOSED" -> normalized;
            default -> null;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
