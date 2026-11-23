package es.ugr.dss.flight;
import org.junit.jupiter.api.Test;
import org.flowable.engine.delegate.DelegateExecution;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria PURA de la lógica de selección de ChooseOffer.
 * No arranca Spring ni Flowable: simula el DelegateExecution con un mapa en memoria
 * (un DelegateExecution de mentira) para poder inyectar ofertas sintéticas.
 */
class ChooseOfferUnitTest {

 /** Crea un DelegateExecution simulado cuyo get/setVariable usan un mapa. */
 private DelegateExecution execution(Map<String, Object> store) {
  DelegateExecution e = mock(DelegateExecution.class);
  when(e.getVariable(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
  doAnswer(inv -> { store.put(inv.getArgument(0), inv.getArgument(1)); return null; })
      .when(e).setVariable(anyString(), any());
  return e;
 }

 @Test void empateEligeIberia() {
  // Precios idénticos fabricados a mano (no tocamos las tarifas oficiales).
  var store = new HashMap<String, Object>();
  store.put("iberiaAvailable", true);  store.put("iberiaPrice", new BigDecimal("200")); store.put("iberiaFailed", false);
  store.put("vuelingAvailable", true); store.put("vuelingPrice", new BigDecimal("200")); store.put("vuelingFailed", false);

  new ChooseOffer().execute(execution(store));

  assertThat(store.get("status")).isEqualTo("QUOTED");
  assertThat(store.get("selectedAirline")).isEqualTo("IBERIA");          // empate -> IBERIA
  assertThat((BigDecimal) store.get("totalPrice")).isEqualByComparingTo(new BigDecimal("200"));
 }

 @Test void sinOfertasDaNoOffer() {
  var store = new HashMap<String, Object>();
  store.put("iberiaAvailable", false);  store.put("iberiaPrice", null);  store.put("iberiaFailed", false);
  store.put("vuelingAvailable", false); store.put("vuelingPrice", null); store.put("vuelingFailed", false);

  new ChooseOffer().execute(execution(store));

  assertThat(store.get("status")).isEqualTo("NO_OFFER");
  assertThat(store.get("selectedAirline")).isNull();
  assertThat(store.get("totalPrice")).isNull();
 }
}