package sit.int221.mytasksservice.dtos.response.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.mytasksservice.services.CollabService;
import sit.int221.mytasksservice.services.TasksService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AppErrorHandler {
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = ItemNotFoundException.class)
    public Map<String, Object> handleItemNotFoundException(ItemNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("message", ex.getMessage());
        response.put("instance", request.getRequestURI());
        return response;
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = GeneralException.class)
    public static Map<String, Object> handleInternalServerValueError(HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", HttpStatus.INTERNAL_SERVER_ERROR);
        response.put("instance", request.getRequestURI());
        return response;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public Map<String, Object> handleArgumentExceptions(BadRequestException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("message", ex.getMessage());
        return errors;
    }

    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ResponseStatusException.class)
    public Map<String, Object> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("message", ex.getReason());
        response.put("instance", request.getRequestURI());
        return response;
    }

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public Map<String, Object> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("message", ex.getMessage());
        response.put("instance", request.getRequestURI());
        return response;
    }

    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateItemException.class)
    public Map<String, Object> handleConflictException(DuplicateItemException ex, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("message", ex.getMessage());
        response.put("instance", request.getRequestURI());
        return response;
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(EmailSendingException.class)
    public Map<String, Object> handleEmailSendingException(EmailSendingException ex, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", ex.getMessage());
        response.put("instance", request.getRequestURI());
        return response;
    }

    @Autowired
    private TasksService tasksService;

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleValidationExceptions(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String boardId = extractBoardIdFromUri(uri);

        if (boardId != null) {
            try {
                tasksService.checkBoardAccess(boardId);
            } catch (ItemNotFoundException e) {
                Map<String, Object> errors = new LinkedHashMap<>();
                errors.put("timestamp", LocalDateTime.now());
                errors.put("status", HttpStatus.NOT_FOUND.value());
                errors.put("message", "Board not found");
                return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
            } catch (ForbiddenException e) {
                Map<String, Object> errors = new LinkedHashMap<>();
                errors.put("timestamp", LocalDateTime.now());
                errors.put("status", HttpStatus.FORBIDDEN.value());
                errors.put("message", "Access Denied: You do not have permission to access this resource");
                return new ResponseEntity<>(errors, HttpStatus.FORBIDDEN);
            }
        }

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validationEx = (MethodArgumentNotValidException) ex;
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("timestamp", LocalDateTime.now());
            errors.put("status", HttpStatus.BAD_REQUEST.value());
            errors.put("message", "Invalid request body");

            String httpMethod = request.getMethod();
            if ("POST".equalsIgnoreCase(httpMethod)) {
                errors.put("status", HttpStatus.FORBIDDEN.value());
                errors.put("message", "Request body cannot be null or empty for POST requests");
                return new ResponseEntity<>(errors, HttpStatus.FORBIDDEN);
            }

            if (validationEx.getBindingResult().getAllErrors().isEmpty()) {
                errors.put("message", "Request body cannot be null or empty");
                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            }

            List<String> validationErrors = validationEx.getBindingResult().getAllErrors().stream()
                    .map(error -> ((MessageSourceResolvable) error).getDefaultMessage())
                    .collect(Collectors.toList());
            errors.put("errors", validationErrors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        if (ex instanceof HttpMessageNotReadableException) {
            String httpMethod = request.getMethod();
            Map<String, Object> errors = new LinkedHashMap<>();
            errors.put("timestamp", LocalDateTime.now());

            if ("POST".equalsIgnoreCase(httpMethod)) {
                errors.put("status", HttpStatus.BAD_REQUEST.value());
                errors.put("message", "Invalid or missing request body");
                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            } else if ("PUT".equalsIgnoreCase(httpMethod)) {
                errors.put("status", HttpStatus.NOT_FOUND.value());
                errors.put("message", "Invalid or missing request body");
                return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
            } else {
                errors.put("status", HttpStatus.FORBIDDEN.value());
                errors.put("message", "Access Denied: Invalid or missing request body");
                return new ResponseEntity<>(errors, HttpStatus.FORBIDDEN);
            }
        }
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("message", "Invalid or missing request body");
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    private String extractBoardIdFromUri(String uri) {
        String[] segments = uri.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].equals("boards") && (i + 1) < segments.length) {
                return segments[i + 1]; // คืนค่าบอร์ด ID
            }
        }
        return null;
    }

}