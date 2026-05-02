package br.com.geozone.service;

import br.com.geozone.dto.GeoDTO;
import br.com.geozone.model.ConsultaHistorico;
import br.com.geozone.model.Usuario;
import br.com.geozone.repository.ConsultaHistoricoRepository;
import br.com.geozone.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private final WebClient webClient;
    private final UsuarioRepository usuarioRepository;
    private final ConsultaHistoricoRepository historicoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${geozone.overpass.url}")
    private String overpassUrl;

    @Value("${geozone.nominatim.url}")
    private String nominatimUrl;

    // ══════════════════════════════════════════
    // BUSCAR PARCELA POR COORDENADA
    // ══════════════════════════════════════════
    public GeoDTO.ParcelaResponse buscarParcela(Double lat, Double lon, String emailUsuario) {
        String query = String.format("""
            [out:json][timeout:15];
            (
              way["landuse"](around:40,%f,%f);
              way["building"](around:40,%f,%f);
              way["natural"](around:40,%f,%f);
              way["leisure"](around:40,%f,%f);
            );
            out body geom;
            """, lat, lon, lat, lon, lat, lon, lat, lon);

        try {
            String response = webClient.post()
                    .uri(overpassUrl)
                    .bodyValue("data=" + query)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode elements = root.path("elements");

            GeoDTO.ParcelaResponse parcela;
            if (elements.isArray() && elements.size() > 0) {
                parcela = parseParcela(elements.get(0), lat, lon);
            } else {
                parcela = parcelaVazia(lat, lon);
            }

            // Enriquecer com Nominatim
            enriquecerComNominatim(parcela, lat, lon);

            // Registrar histórico
            if (emailUsuario != null) {
                registrarHistorico(emailUsuario, lat, lon, parcela);
            }

            return parcela;

        } catch (Exception e) {
            log.error("Erro ao buscar parcela OSM [{},{}]: {}", lat, lon, e.getMessage());
            GeoDTO.ParcelaResponse fallback = parcelaVazia(lat, lon);
            enriquecerComNominatim(fallback, lat, lon);
            return fallback;
        }
    }

    // ══════════════════════════════════════════
    // BUSCAR ENDEREÇO (NOMINATIM SEARCH)
    // ══════════════════════════════════════════
    @Cacheable(value = "buscaEndereco", key = "#q")
    public List<GeoDTO.SearchResult> buscarEndereco(String q) {
        try {
            String url = nominatimUrl + "/search?format=json&q="
                    + java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8)
                    + "&limit=6&addressdetails=1&countrycodes=br";

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode results = objectMapper.readTree(response);
            List<GeoDTO.SearchResult> lista = new ArrayList<>();

            for (JsonNode r : results) {
                String displayName = r.path("display_name").asText();
                String[] partes = displayName.split(",");
                lista.add(GeoDTO.SearchResult.builder()
                        .nome(partes.length > 0 ? partes[0].trim() : displayName)
                        .enderecoCurto(partes.length > 1 ? partes[1].trim() : "")
                        .enderecoCompleto(displayName)
                        .latitude(r.path("lat").asDouble())
                        .longitude(r.path("lon").asDouble())
                        .build());
            }
            return lista;

        } catch (Exception e) {
            log.error("Erro na busca Nominatim '{}': {}", q, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ══════════════════════════════════════════
    // GEOCODIFICAÇÃO REVERSA
    // ══════════════════════════════════════════
    @Cacheable(value = "revGeo", key = "#lat + ',' + #lon")
    public GeoDTO.NominatimResult reverseGeocode(Double lat, Double lon) {
        try {
            String url = nominatimUrl + "/reverse?format=json&lat=" + lat + "&lon=" + lon
                    + "&zoom=18&addressdetails=1";

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode addr = root.path("address");

            String road = firstNonEmpty(
                    addr.path("road").asText(""),
                    addr.path("pedestrian").asText(""),
                    addr.path("suburb").asText("")
            );
            String city = firstNonEmpty(
                    addr.path("city").asText(""),
                    addr.path("town").asText(""),
                    addr.path("village").asText(""),
                    addr.path("municipality").asText("")
            );

            return GeoDTO.NominatimResult.builder()
                    .displayName(root.path("display_name").asText())
                    .road(road)
                    .houseNumber(addr.path("house_number").asText(""))
                    .city(city)
                    .state(addr.path("state").asText(""))
                    .postcode(addr.path("postcode").asText(""))
                    .lat(lat)
                    .lon(lon)
                    .build();

        } catch (Exception e) {
            log.error("Erro no reverse geocode [{},{}]: {}", lat, lon, e.getMessage());
            return GeoDTO.NominatimResult.builder().lat(lat).lon(lon).build();
        }
    }

    // ══════════════════════════════════════════
    // CALCULAR MEDIÇÃO (DISTÂNCIA OU ÁREA)
    // ══════════════════════════════════════════
    public GeoDTO.MedicaoResponse calcularMedicao(GeoDTO.MedicaoRequest req) {
        List<GeoDTO.Ponto> pontos = req.getPontos();
        if (pontos == null || pontos.size() < 2) {
            throw new RuntimeException("Mínimo de 2 pontos necessários para medição");
        }

        if ("distancia".equalsIgnoreCase(req.getTipo())) {
            double totalMetros = 0;
            for (int i = 1; i < pontos.size(); i++) {
                totalMetros += haversine(
                        pontos.get(i - 1).getLat(), pontos.get(i - 1).getLng(),
                        pontos.get(i).getLat(), pontos.get(i).getLng()
                );
            }
            String unidade = totalMetros >= 1000 ? "km" : "m";
            double valor = totalMetros >= 1000 ? totalMetros / 1000.0 : totalMetros;
            String formatado = totalMetros >= 1000
                    ? String.format("%.2f km", valor)
                    : String.format("%.1f m", valor);

            return GeoDTO.MedicaoResponse.builder()
                    .tipo("distancia")
                    .valor(valor)
                    .unidade(unidade)
                    .valorFormatado(formatado)
                    .build();

        } else if ("area".equalsIgnoreCase(req.getTipo())) {
            if (pontos.size() < 3) {
                throw new RuntimeException("Mínimo de 3 pontos para calcular área");
            }
            double areaM2 = calcularAreaPoligono(pontos);
            String unidade = areaM2 >= 10000 ? "ha" : "m²";
            double valor = areaM2 >= 10000 ? areaM2 / 10000.0 : areaM2;
            String formatado = areaM2 >= 10000
                    ? String.format("%.3f ha", valor)
                    : String.format("%.1f m²", valor);

            return GeoDTO.MedicaoResponse.builder()
                    .tipo("area")
                    .valor(valor)
                    .unidade(unidade)
                    .valorFormatado(formatado)
                    .build();
        }

        throw new RuntimeException("Tipo de medição inválido. Use 'distancia' ou 'area'");
    }

    // ══════════════════════════════════════════
    // PARSE DA RESPOSTA OSM
    // ══════════════════════════════════════════
    private GeoDTO.ParcelaResponse parseParcela(JsonNode el, double lat, double lon) {
        Map<String, String> tags = new LinkedHashMap<>();
        JsonNode tagsNode = el.path("tags");
        tagsNode.fields().forEachRemaining(e -> tags.put(e.getKey(), e.getValue().asText()));

        String landuse = firstTag(tags, "landuse", "building", "natural", "leisure", "amenity");
        String area = calcularAreaOsm(el);

        List<GeoDTO.ViabilidadeItem> viab = gerarViabilidade(tags, landuse, area);

        return GeoDTO.ParcelaResponse.builder()
                .osmId(el.path("id").asLong())
                .tipo(labelLanduse(landuse))
                .landuse(landuse)
                .endereco(buildEndereco(tags))
                .cidade(firstTag(tags, "addr:city", "addr:municipality"))
                .estado(tags.getOrDefault("addr:state", ""))
                .cep(tags.getOrDefault("addr:postcode", ""))
                .area(area)
                .latitude(lat)
                .longitude(lon)
                .tags(tags)
                .viabilidade(viab)
                .build();
    }

    private GeoDTO.ParcelaResponse parcelaVazia(double lat, double lon) {
        return GeoDTO.ParcelaResponse.builder()
                .osmId(0L)
                .tipo("Não classificado")
                .landuse("")
                .endereco("")
                .cidade("")
                .estado("")
                .cep("")
                .area("—")
                .latitude(lat)
                .longitude(lon)
                .tags(new HashMap<>())
                .viabilidade(Collections.emptyList())
                .build();
    }

    private void enriquecerComNominatim(GeoDTO.ParcelaResponse parcela, double lat, double lon) {
        try {
            GeoDTO.NominatimResult nom = reverseGeocode(lat, lon);
            if (nom == null) return;
            if (isEmpty(parcela.getEndereco())) {
                String end = buildEnderecoNominatim(nom);
                parcela.setEndereco(end);
            }
            if (isEmpty(parcela.getCidade()) && !isEmpty(nom.getCity())) {
                parcela.setCidade(nom.getCity());
            }
            if (isEmpty(parcela.getEstado()) && !isEmpty(nom.getState())) {
                parcela.setEstado(nom.getState());
            }
            if (isEmpty(parcela.getCep()) && !isEmpty(nom.getPostcode())) {
                parcela.setCep(nom.getPostcode());
            }
        } catch (Exception e) {
            log.debug("Enriquecimento Nominatim falhou: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // VIABILIDADE
    // ══════════════════════════════════════════
    private List<GeoDTO.ViabilidadeItem> gerarViabilidade(Map<String, String> tags, String landuse, String area) {
        List<GeoDTO.ViabilidadeItem> lista = new ArrayList<>();

        // Uso do solo
        String statusUso = List.of("residential","commercial","mixed_use").contains(landuse) ? "ok"
                : List.of("industrial","farmland").contains(landuse) ? "warn" : "no";
        lista.add(new GeoDTO.ViabilidadeItem("Uso do Solo", statusUso, labelLanduse(landuse)));

        // Endereçamento
        boolean temEnd = tags.containsKey("addr:street") || tags.containsKey("addr:city");
        lista.add(new GeoDTO.ViabilidadeItem("Endereçamento", temEnd ? "ok" : "warn",
                temEnd ? "Cadastrado no OSM" : "Não encontrado"));

        // Área
        boolean temArea = !area.equals("—") && !area.isEmpty();
        lista.add(new GeoDTO.ViabilidadeItem("Área Calculada", temArea ? "ok" : "warn",
                temArea ? area : "Não calculada"));

        // Edificação existente
        boolean temEdif = tags.containsKey("building");
        lista.add(new GeoDTO.ViabilidadeItem("Edificação", "ok",
                temEdif ? "Com edificação" : "Terreno livre"));

        // Dados OSM
        lista.add(new GeoDTO.ViabilidadeItem("Dados OSM", "ok", "Disponível"));

        return lista;
    }

    // ══════════════════════════════════════════
    // HISTÓRICO
    // ══════════════════════════════════════════
    private void registrarHistorico(String email, double lat, double lon, GeoDTO.ParcelaResponse parcela) {
        try {
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                ConsultaHistorico hist = ConsultaHistorico.builder()
                        .usuario(usuario)
                        .latitude(lat)
                        .longitude(lon)
                        .endereco(parcela.getEndereco())
                        .landuse(parcela.getLanduse())
                        .osmId(parcela.getOsmId())
                        .build();
                historicoRepository.save(hist);
            });
        } catch (Exception e) {
            log.debug("Erro ao salvar histórico: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // MATEMÁTICA GEOGRÁFICA
    // ══════════════════════════════════════════
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calcularAreaPoligono(List<GeoDTO.Ponto> pontos) {
        double area = 0;
        int n = pontos.size();
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += pontos.get(i).getLat() * pontos.get(j).getLng();
            area -= pontos.get(j).getLat() * pontos.get(i).getLng();
        }
        double areaGraus = Math.abs(area) / 2.0;
        double latMedia = pontos.stream().mapToDouble(GeoDTO.Ponto::getLat).average().orElse(0);
        double fatorLat = 111320.0;
        double fatorLon = 111320.0 * Math.cos(Math.toRadians(latMedia));
        return areaGraus * fatorLat * fatorLon;
    }

    private String calcularAreaOsm(JsonNode el) {
        try {
            JsonNode geo = el.path("geometry");
            if (!geo.isArray() || geo.size() < 3) return "—";
            List<GeoDTO.Ponto> pontos = new ArrayList<>();
            geo.forEach(p -> pontos.add(new GeoDTO.Ponto(p.path("lat").asDouble(), p.path("lon").asDouble())));
            double area = calcularAreaPoligono(pontos);
            if (area <= 0) return "—";
            return area >= 10000
                    ? String.format("%.3f ha", area / 10000.0)
                    : String.format("%.0f m²", area);
        } catch (Exception e) {
            return "—";
        }
    }

    // ══════════════════════════════════════════
    // UTILS
    // ══════════════════════════════════════════
    private String labelLanduse(String lu) {
        if (lu == null || lu.isBlank()) return "Não classificado";
        Map<String, String> m = Map.ofEntries(
                Map.entry("residential", "Residencial"),
                Map.entry("commercial", "Comercial"),
                Map.entry("industrial", "Industrial"),
                Map.entry("retail", "Varejo"),
                Map.entry("farmland", "Agrícola"),
                Map.entry("forest", "Floresta"),
                Map.entry("grass", "Área Verde"),
                Map.entry("meadow", "Campo"),
                Map.entry("recreation_ground", "Lazer"),
                Map.entry("cemetery", "Cemitério"),
                Map.entry("construction", "Em Obras"),
                Map.entry("yes", "Edificação"),
                Map.entry("house", "Casa"),
                Map.entry("apartments", "Apartamentos"),
                Map.entry("office", "Escritório"),
                Map.entry("school", "Escola"),
                Map.entry("hospital", "Hospital")
        );
        return m.getOrDefault(lu, capitalize(lu.replace("_", " ")));
    }

    private String buildEndereco(Map<String, String> tags) {
        String road = tags.getOrDefault("addr:street", tags.getOrDefault("name", ""));
        String num = tags.getOrDefault("addr:housenumber", "");
        if (road.isBlank()) return "";
        return num.isBlank() ? road : road + ", " + num;
    }

    private String buildEnderecoNominatim(GeoDTO.NominatimResult nom) {
        String road = nom.getRoad() == null ? "" : nom.getRoad();
        String num = nom.getHouseNumber() == null ? "" : nom.getHouseNumber();
        if (road.isBlank()) return nom.getDisplayName() != null ? nom.getDisplayName().split(",")[0] : "";
        return num.isBlank() ? road : road + ", " + num;
    }

    private String firstTag(Map<String, String> tags, String... keys) {
        for (String k : keys) {
            if (tags.containsKey(k) && !tags.get(k).isBlank()) return tags.get(k);
        }
        return "";
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return "";
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
