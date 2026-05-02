package br.com.geozone.service;

import br.com.geozone.dto.AdminLoginRequest;
import br.com.geozone.dto.AuthResponse;
import br.com.geozone.dto.LoginRequest;
import br.com.geozone.dto.RegisterRequest;
import br.com.geozone.model.Usuario;
import br.com.geozone.repository.UsuarioRepository;
import br.com.geozone.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${geozone.admin.email}")
    private String adminEmail;

    @Value("${geozone.admin.password}")
    private String adminPassword;

    @Value("${geozone.admin.twofa}")
    private String adminTwoFa;

    // Controle de tentativas de login admin
    private final ConcurrentHashMap<String, AtomicInteger> tentativas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> bloqueios = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════
    // CADASTRO
    // ══════════════════════════════════════════
    @Transactional
    public AuthResponse cadastrar(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado: " + req.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .nome(req.getNome())
                .email(req.getEmail().toLowerCase().trim())
                .senha(passwordEncoder.encode(req.getSenha()))
                .plano(Usuario.Plano.GRATUITO)
                .role(Usuario.Role.USER)
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Novo usuário cadastrado: {} ({})", salvo.getNome(), salvo.getEmail());

        UserDetails ud = userDetailsService.loadUserByUsername(salvo.getEmail());
        String token = jwtUtil.gerarToken(ud);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(salvo.getId())
                .nome(salvo.getNome())
                .email(salvo.getEmail())
                .plano(salvo.getPlano())
                .role(salvo.getRole())
                .isAdmin(false)
                .build();
    }

    // ══════════════════════════════════════════
    // LOGIN USUÁRIO
    // ══════════════════════════════════════════
    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getEmail().toLowerCase().trim(),
                            req.getSenha()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("E-mail ou senha incorretos");
        }

        Usuario usuario = usuarioRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserDetails ud = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtUtil.gerarToken(ud);

        log.info("Login realizado: {}", usuario.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .plano(usuario.getPlano())
                .role(usuario.getRole())
                .isAdmin(false)
                .build();
    }

    // ══════════════════════════════════════════
    // LOGIN ADMIN COM 2FA
    // ══════════════════════════════════════════
    public AuthResponse loginAdmin(AdminLoginRequest req) {
        String ip = req.getEmail(); // chave por email para controle

        // Verificar bloqueio
        Long bloqueadoAte = bloqueios.get(ip);
        if (bloqueadoAte != null && System.currentTimeMillis() < bloqueadoAte) {
            long restante = (bloqueadoAte - System.currentTimeMillis()) / 1000;
            throw new RuntimeException("Conta bloqueada. Tente novamente em " + restante + "s");
        }

        boolean emailOk = req.getEmail().equalsIgnoreCase(adminEmail);
        boolean senhaOk = req.getSenha().equals(adminPassword);
        boolean tokenOk = req.getToken2fa().equals(adminTwoFa);

        if (!emailOk || !senhaOk || !tokenOk) {
            AtomicInteger count = tentativas.computeIfAbsent(ip, k -> new AtomicInteger(0));
            int tentativa = count.incrementAndGet();
            if (tentativa >= 3) {
                bloqueios.put(ip, System.currentTimeMillis() + 60_000L);
                tentativas.remove(ip);
                log.warn("Admin bloqueado após 3 tentativas: {}", req.getEmail());
                throw new RuntimeException("3 tentativas falhas. Bloqueado por 60 segundos.");
            }
            int restantes = 3 - tentativa;
            throw new RuntimeException("Credenciais inválidas. " + restantes + " tentativa(s) restante(s).");
        }

        // Sucesso — resetar tentativas
        tentativas.remove(ip);
        bloqueios.remove(ip);

        // Garantir que admin existe no banco ou criar admin fictício para JWT
        Usuario admin = usuarioRepository.findByEmail(adminEmail).orElseGet(() -> {
            Usuario a = Usuario.builder()
                    .nome("Administrador")
                    .email(adminEmail)
                    .senha(passwordEncoder.encode(adminPassword))
                    .plano(Usuario.Plano.ENTERPRISE)
                    .role(Usuario.Role.ADMIN)
                    .ativo(true)
                    .build();
            return usuarioRepository.save(a);
        });

        String token = jwtUtil.gerarTokenAdmin(admin.getEmail());
        log.info("Login ADMIN realizado: {}", admin.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(admin.getId())
                .nome(admin.getNome())
                .email(admin.getEmail())
                .plano(admin.getPlano())
                .role(admin.getRole())
                .isAdmin(true)
                .build();
    }

    // ══════════════════════════════════════════
    // UPGRADE DE PLANO
    // ══════════════════════════════════════════
    @Transactional
    public AuthResponse upgradePlano(String email, Usuario.Plano novoPlano) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuario.setPlano(novoPlano);
        usuarioRepository.save(usuario);
        log.info("Plano atualizado para {} — usuário: {}", novoPlano, email);

        UserDetails ud = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.gerarToken(ud);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .plano(usuario.getPlano())
                .role(usuario.getRole())
                .isAdmin(false)
                .build();
    }
}
