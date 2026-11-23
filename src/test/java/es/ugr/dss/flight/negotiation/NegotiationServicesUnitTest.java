package es.ugr.dss.flight.negotiation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias puras de las reglas de comprador y vendedor.
 * No arrancan Spring ni Flowable: instancian directamente los beans.
 */
class NegotiationServicesUnitTest {
	
	@Test void buyerAcceptsWithinBudgetAndThreshold() {
		var buyer = new Buyer();
		// P0 = 10000  =>  umbral del comprador = 70% = 7000
		assertThat(buyer.accepts(7000, 10000, 7000)).isTrue(); // caso justo en el límite (guión)
		assertThat(buyer.accepts(7000, 10000, 6999)).isFalse(); // presupuesto insuficiente (guión)
		assertThat(buyer.accepts(7001, 10000, 10000)).isFalse(); // supera el 70% de P0 aunque haya presupuesto
	}
	
	@Test void sellerDropsTenPercentButNotBelowPmin() {
		var seller = new Seller();
		assertThat(seller.nextOffer(10000, 6000)).isEqualTo(9000L); // -10%
		assertThat(seller.nextOffer(6561, 6000)).isEqualTo(6000L); // 5904 está debajo de Pmin -> cap en 6000
		assertThat(seller.nextOffer(6000, 6000)).isEqualTo(6000L); // ya es Pmin -> estancamiento
	}
}
