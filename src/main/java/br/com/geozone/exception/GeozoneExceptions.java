package br.com.geozone.exception;

import br.com.geozone.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// ══════════════════════════════════════════
// Exceções customizadas
// ══════════════════════════════════════════

class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}

class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado: " + id);
    }
}

class FavoritoNaoEncontradoException extends RuntimeException {
    public FavoritoNaoEncontradoException(Long id) {
        super("Favorito não encontrado: " + id);
    }
}

class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}

class Token2FAInvalidoException extends RuntimeException {
    public Token2FAInvalidoException() {
        super("Código 2FA inválido");
    }
}

class LimiteFavoritosException extends RuntimeException {
    public LimiteFavoritosException() {
        super("Limite de favoritos atingido no plano gratuito. Faça upgrade para o Pro.");
    }
}

// ══════════════════════════════════════════
// Handler global de exceções
// ══════════════════════════════════════════
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String campo = ((FieldError) e).getField();
            String msg = e.getDefaultMessage();
            erros.put(campo, msg);
        });
        return ResponseEntity.badRequest().body(ApiResponse.erro("Dados inválidos: " + erros));
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailDuplicado(EmailJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioNotFound(UsuarioNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler(FavoritoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleFavoritoNotFound(FavoritoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Void>> handleCredenciais(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.erro("Credenciais inválidas"));
    }

    @ExceptionHandler(Token2FAInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handle2FA(Token2FAInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler(LimiteFavoritosException.class)
    public ResponseEntity<ApiResponse<Void>> handleLimite(LimiteFavoritosException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAcesso(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.erro("Acesso negado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.erro("Erro interno: " + ex.getMessage()));
    }
}
