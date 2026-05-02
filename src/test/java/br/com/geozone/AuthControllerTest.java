package br.com.geozone;

import br.com.geozone.dto.LoginRequest;
import br.com.geozone.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GeoZone — Testes de Autenticação")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Cadastro de usuário deve retornar 201 com token")
    void deveCadastrarUsuario() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setNome("João Silva");
        req.setEmail("joao@teste.com");
        req.setSenha("senha1234");

        mockMvc.perform(post("/api/auth/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.token").isNotEmpty())
                .andExpect(jsonPath("$.dados.email").value("joao@teste.com"));
    }

    @Test
    @DisplayName("Cadastro com e-mail inválido deve retornar 400")
    void deveRejeitarEmailInvalido() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setNome("Maria");
        req.setEmail("email-invalido");
        req.setSenha("senha1234");

        mockMvc.perform(post("/api/auth/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login com credenciais válidas deve retornar token")
    void deveLogar() throws Exception {
        // Primeiro cadastra
        RegisterRequest reg = new RegisterRequest();
        reg.setNome("Ana Costa");
        reg.setEmail("ana@teste.com");
        reg.setSenha("senha1234");
        mockMvc.perform(post("/api/auth/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Depois loga
        LoginRequest login = new LoginRequest();
        login.setEmail("ana@teste.com");
        login.setSenha("senha1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.token").isNotEmpty());
    }

    @Test
    @DisplayName("Login admin com token 2FA correto deve retornar token admin")
    void deveLogarAdmin() throws Exception {
        String body = """
            {
              "email": "admin@geozone.com.br",
              "senha": "GeoZone@Admin2025",
              "token2fa": "492817"
            }
            """;

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.isAdmin").value(true))
                .andExpect(jsonPath("$.dados.token").isNotEmpty());
    }

    @Test
    @DisplayName("Info pública deve retornar status online")
    void deveRetornarInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.app").value("GeoZone API"))
                .andExpect(jsonPath("$.dados.status").value("online"));
    }
}
