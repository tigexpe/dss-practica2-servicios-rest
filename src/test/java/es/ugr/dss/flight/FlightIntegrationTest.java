package es.ugr.dss.flight;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class FlightIntegrationTest {

 @Autowired TestRestTemplate http;

 private ResponseEntity<TravelResult> book(String emp, String dest, String start, String end) {
  return http.postForEntity("http://localhost:8080/api/bookings",
      new TravelRequest(emp, dest, start, end), TravelResult.class);
 }

 // ---------- Pruebas iniciales, ahora reforzadas con aerolínea y precio exactos ----------

 @Test void authorizedGetsCheapestVueling() {
  var r = book("EMP001", "AMS", "2026-11-10", "2026-11-14");
  assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
  assertThat(r.getBody().status()).isEqualTo("QUOTED");
  assertThat(r.getBody().selectedAirline()).isEqualTo("VUELING");
  assertThat(r.getBody().totalPrice()).isEqualByComparingTo(new BigDecimal("275"));
 }

 @Test void unauthorizedIsRejected() {
  var r = book("EMP999", "AMS", "2026-11-10", "2026-11-14");
  assertThat(r.getBody().status()).isEqualTo("REJECTED");
  assertThat(r.getBody().selectedAirline()).isNull();   // no se consultó a ninguna aerolínea
  assertThat(r.getBody().totalPrice()).isNull();
 }

 @Test void invalidDatesRejected() {
  var r = book("EMP001", "AMS", "2026-11-14", "2026-11-10"); // regreso antes de salida
  assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
 }

 @Test void noOffers() {
  var r = book("EMP001", "NONE", "2026-11-10", "2026-11-14");
  assertThat(r.getBody().status()).isEqualTo("NO_OFFER");
  assertThat(r.getBody().selectedAirline()).isNull();
  assertThat(r.getBody().totalPrice()).isNull();
 }

 // ---------- Pruebas nuevas ----------

 // BUSINESS: Iberia (250+50)*2=600, Vueling (250+25)*2=550 -> gana Vueling.
 @Test void businessClassChoosesCheapest() {
  var r = book("EMP002", "AMS", "2026-11-10", "2026-11-14");
  assertThat(r.getBody().status()).isEqualTo("QUOTED");
  assertThat(r.getBody().selectedAirline()).isEqualTo("VUELING");
  assertThat(r.getBody().totalPrice()).isEqualByComparingTo(new BigDecimal("550"));
 }

 // FAIL: Vueling devuelve 503 (falla), Iberia sigue -> gana Iberia (120+50)*1=170.
 @Test void vuelingOutageKeepsIberia() {
  var r = book("EMP001", "FAIL", "2026-11-10", "2026-11-14");
  assertThat(r.getBody().status()).isEqualTo("QUOTED");
  assertThat(r.getBody().selectedAirline()).isEqualTo("IBERIA");
  assertThat(r.getBody().totalPrice()).isEqualByComparingTo(new BigDecimal("170"));
 }
}