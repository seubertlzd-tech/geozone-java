# GeoZone v2.0 — Backend Java Spring Boot

API REST completa para análise de propriedades e parcelas com OpenStreetMap.

---

## 🚀 Como Rodar

### Pré-requisitos
- Java 21+
- Maven 3.9+

### 1. Instalar dependências e rodar
```bash
cd geozone-java
mvn spring-boot:run
```

### 2. Compilar JAR executável
```bash
mvn clean package -DskipTests
java -jar target/geozone-api-2.0.0.jar
```

### 3. Rodar os testes
```bash
mvn test
```

---

## 🌐 Endpoints da API

### Públicos (sem autenticação)
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/info` | Status e informações da API |
| GET | `/api/geo/buscar?q=` | Busca endereços (Nominatim) |
| POST | `/api/auth/cadastrar` | Cadastrar novo usuário |
| POST | `/api/auth/login` | Login de usuário |
| POST | `/api/auth/admin/login` | Login de administrador (com 2FA) |

### Autenticados (Bearer Token)
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/geo/parcela?lat=&lon=` | Dados da parcela OSM |
| GET | `/api/geo/reverse?lat=&lon=` | Geocodificação reversa |
| POST | `/api/geo/medir` | Calcular distância ou área |
| GET | `/api/favoritos` | Listar favoritos |
| POST | `/api/favoritos` | Salvar favorito |
| DELETE | `/api/favoritos/{id}` | Remover favorito |
| PUT | `/api/auth/upgrade?plano=PRO` | Fazer upgrade de plano |
| GET | `/api/auth/me` | Dados do usuário logado |

### Admin (Bearer Token Admin)
| Método | URL | Descrição |
|--------|-----|-----------|
| GET | `/api/admin/stats` | Estatísticas do painel |
| GET | `/api/admin/usuarios` | Listar todos usuários |
| PUT | `/api/admin/usuarios/{id}/plano?plano=PRO` | Alterar plano |
| PUT | `/api/admin/usuarios/{id}/toggle` | Ativar/desativar |
| DELETE | `/api/admin/usuarios/{id}` | Remover usuário |

---

## 🔐 Autenticação

### 1. Cadastrar
```bash
curl -X POST http://localhost:8080/api/auth/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","email":"joao@email.com","senha":"senha1234"}'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@email.com","senha":"senha1234"}'
```

### 3. Usar o token
```bash
curl http://localhost:8080/api/favoritos \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

### 4. Login Admin (2FA)
```bash
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@geozone.com.br","senha":"GeoZone@Admin2025","token2fa":"492817"}'
```

---

## 📍 Exemplos de Uso

### Buscar parcela por coordenada
```bash
curl "http://localhost:8080/api/geo/parcela?lat=-23.3045&lon=-51.1696" \
  -H "Authorization: Bearer TOKEN"
```

### Buscar endereço
```bash
curl "http://localhost:8080/api/geo/buscar?q=Avenida+Paulista+São+Paulo"
```

### Calcular distância entre pontos
```bash
curl -X POST http://localhost:8080/api/geo/medir \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "tipo": "distancia",
    "pontos": [
      {"lat": -23.3045, "lng": -51.1696},
      {"lat": -23.3100, "lng": -51.1750}
    ]
  }'
```

### Calcular área de polígono
```bash
curl -X POST http://localhost:8080/api/geo/medir \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "tipo": "area",
    "pontos": [
      {"lat": -23.300, "lng": -51.165},
      {"lat": -23.301, "lng": -51.170},
      {"lat": -23.305, "lng": -51.168},
      {"lat": -23.304, "lng": -51.163}
    ]
  }'
```

### Salvar favorito
```bash
curl -X POST http://localhost:8080/api/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "endereco": "Av. Brasil, 1000",
    "cidade": "Londrina",
    "estado": "PR",
    "tipo": "Residencial",
    "area": "450 m²",
    "latitude": -23.3045,
    "longitude": -51.1696
  }'
```

---

## 🗄️ Banco de Dados

### Desenvolvimento (H2 — já configurado, sem instalar nada)
- Console web: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:geozonedb`
- Usuário: `sa` | Senha: (em branco)

### Produção (PostgreSQL)
Edite `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/geozonedb
spring.datasource.username=postgres
spring.datasource.password=sua_senha
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

## 🏗️ Estrutura do Projeto

```
geozone-java/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/geozone/
    │   │   ├── GeozoneApplication.java       ← Ponto de entrada
    │   │   ├── controller/
    │   │   │   ├── AuthController.java       ← Login, cadastro
    │   │   │   ├── GeoController.java        ← Parcelas, busca, medição
    │   │   │   ├── FavoritoController.java   ← CRUD favoritos
    │   │   │   ├── AdminController.java      ← Painel admin
    │   │   │   └── InfoController.java       ← Status API
    │   │   ├── service/
    │   │   │   ├── AuthService.java          ← Auth + JWT + 2FA
    │   │   │   ├── GeoService.java           ← Overpass + Nominatim
    │   │   │   ├── FavoritoService.java      ← Lógica de favoritos
    │   │   │   └── AdminService.java         ← Painel administrativo
    │   │   ├── model/
    │   │   │   ├── Usuario.java              ← Entidade JPA
    │   │   │   ├── Favorito.java             ← Entidade JPA
    │   │   │   └── ConsultaHistorico.java    ← Entidade JPA
    │   │   ├── repository/
    │   │   │   ├── UsuarioRepository.java
    │   │   │   ├── FavoritoRepository.java
    │   │   │   └── ConsultaHistoricoRepository.java
    │   │   ├── dto/
    │   │   │   ├── AuthResponse.java
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── AdminLoginRequest.java
    │   │   │   ├── FavoritoDTO.java
    │   │   │   ├── GeoDTO.java
    │   │   │   ├── ApiResponse.java
    │   │   │   └── AdminStatsDTO.java
    │   │   ├── security/
    │   │   │   ├── JwtUtil.java              ← Geração/validação JWT
    │   │   │   ├── JwtAuthFilter.java        ← Filtro de autenticação
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java       ← Spring Security + CORS
    │   │   │   └── WebClientConfig.java      ← WebClient HTTP
    │   │   └── exception/
    │   │       └── GeozoneExceptions.java    ← Exceções + Handler global
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/br/com/geozone/
            └── AuthControllerTest.java
```

---

## 🔧 Tecnologias Utilizadas

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.2.0 | Framework principal |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Persistência de dados |
| JWT (jjwt) | 0.11.5 | Tokens de autenticação |
| WebFlux WebClient | 6.x | Chamadas HTTP (Overpass, Nominatim) |
| H2 Database | — | Banco em memória (dev) |
| PostgreSQL | 15+ | Banco em produção |
| Lombok | — | Redução de boilerplate |
| Maven | 3.9 | Gerenciador de dependências |

---

## 📝 Credenciais Padrão (Dev)

| Tipo | Email | Senha | 2FA |
|------|-------|-------|-----|
| Admin | admin@geozone.com.br | GeoZone@Admin2025 | 492817 |

> **⚠️ Importante:** Altere todas as credenciais em `application.properties` antes de ir para produção!

---

## 📡 APIs Externas Utilizadas (Gratuitas)

- **OpenStreetMap Overpass API** — Dados de parcelas e edificações
- **Nominatim** — Geocodificação e busca de endereços
- Ambas são gratuitas e não requerem chave de API

---

*GeoZone v2.0 — Informação que Constrói Decisões*
