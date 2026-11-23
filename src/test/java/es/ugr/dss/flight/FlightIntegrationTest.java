package es.ugr.dss.flight;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
class FlightIntegrationTest {
 @Autowired TestRestTemplate http;
 private ResponseEntity<TravelResult> book(String emp,String dest,String start,String end){
  return http.postForEntity("http://localhost:8080/api/bookings",new TravelRequest(emp,dest,start,end),TravelResult.class);
 }
 @Test void authorizedGetsQuote(){var r=book("EMP001","AMS","2026-11-10","2026-11-14");assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);assertThat(r.getBody().status()).isEqualTo("QUOTED");assertThat(r.getBody().totalPrice()).isNotNull();}
 @Test void unauthorizedIsRejected(){var r=book("EMP999","AMS","2026-11-10","2026-11-14");assertThat(r.getBody().status()).isEqualTo("REJECTED");}
 @Test void invalidDatesRejected(){var r=book("EMP001","AMS","2026-11-14","2026-11-10");assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);}
 @Test void noOffers(){var r=book("EMP001","NONE","2026-11-10","2026-11-14");assertThat(r.getBody().status()).isEqualTo("NO_OFFER");}
}
