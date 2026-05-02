# GeoZone API

Backend da GeoZone em Java (Spring Boot). Consulta parcelas via OpenStreetMap, busca endereços, calcula áreas/distâncias e tem sistema de favoritos com planos.

## Como rodar

Precisa ter Java 21 e Maven instalado.

```
mvn spring-boot:run
```

Roda na porta 8080 por padrão. Usa H2 em memória pra dev, não precisa instalar banco.

## Endpoints principais

- POST /api/auth/cadastrar - cria conta
- POST /api/auth/login - login normal
- POST /api/auth/admin/login - login admin (precisa 2FA)
- GET /api/geo/parcela?lat=&lon= - busca parcela na coordenada
- GET /api/geo/buscar?q= - busca endereço (Nominatim)
- GET /api/geo/reverse?lat=&lon= - geocodificação reversa
- POST /api/geo/medir - calcula distância ou área
- GET /api/favoritos - lista favoritos do usuário
- POST /api/favoritos - salva favorito
- DELETE /api/favoritos/{id} - remove favorito
- GET /api/admin/stats - estatísticas (só admin)
- GET /api/admin/usuarios - lista usuários (só admin)

## Banco

Dev: H2 em memória (já configurado, console em /h2-console)

Produção: trocar pra PostgreSQL no application.properties

## Credenciais dev

Admin: admin@geozone.com.br / GeoZone@Admin2025 / 2FA: 492817

Trocar antes de subir pra produção.

## Testes

```
mvn test
```
