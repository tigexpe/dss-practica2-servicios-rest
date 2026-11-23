package es.ugr.dss.flight.negotiation;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.BpmnError;
import org.springframework.stereotype.Component;

/* =========================================================================
 *  1) VALIDACIÓN DE ENTRADA
 *  presupuesto > 0 y 1 <= maxRondas <= 10. Si no, error BPMN ENTRADA_INVALIDA.
 *  Inicializa rondasEvaluadas=0 cuanto antes (los casos de error deben dar 0).
 * ========================================================================= */
@Component("validateNegotiation")
class ValidateNegotiation implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  e.setVariable("rondasEvaluadas", 0);
  Number budget = (Number) e.getVariable("presupuestoCentimos");
  Number maxR   = (Number) e.getVariable("maxRondas");
  boolean ok = budget != null && budget.longValue() > 0
            && maxR  != null && maxR.intValue() >= 1 && maxR.intValue() <= 10;
  if (!ok) {
   e.setVariable("estado", "ERROR");
   e.setVariable("motivo", "ENTRADA_INVALIDA");
   throw new BpmnError("ENTRADA_INVALIDA");
  }
 }
}

/* =========================================================================
 *  2) CONSULTA DE EXISTENCIAS
 *  Producto desconocido -> PRODUCTO_DESCONOCIDO ; stock 0 -> SIN_EXISTENCIAS.
 *  Si puede continuar, guarda precioInicial (P0) y precioMinimo (Pmin).
 * ========================================================================= */
@Component("checkStock")
class CheckStock implements JavaDelegate {
 private final Catalog catalog;
 CheckStock(Catalog catalog) { this.catalog = catalog; }
 @Override public void execute(DelegateExecution e) {
  Catalog.Product p = catalog.find((String) e.getVariable("producto"));
  if (p == null) {
   e.setVariable("estado", "ERROR");
   e.setVariable("motivo", "PRODUCTO_DESCONOCIDO");
   throw new BpmnError("PRODUCTO_DESCONOCIDO");
  }
  if (p.stock() <= 0) {
   e.setVariable("estado", "ERROR");
   e.setVariable("motivo", "SIN_EXISTENCIAS");
   throw new BpmnError("SIN_EXISTENCIAS");
  }
  e.setVariable("precioInicial", p.p0());
  e.setVariable("precioMinimo",  p.pmin());
 }
}

/* =========================================================================
 *  3) INICIALIZACIÓN:  precioActual = P0 ; ronda = 1
 * ========================================================================= */
@Component("initNegotiation")
class InitNegotiation implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  long p0 = ((Number) e.getVariable("precioInicial")).longValue();
  e.setVariable("precioActual", p0);
  e.setVariable("ronda", 1);
 }
}

/* =========================================================================
 *  4) EVALUAR COMPRADOR:  fija 'aceptable'
 *     (la tarea BPMN que lo use debe tener id="buyer")
 * ========================================================================= */
@Component("evaluateBuyer")
class EvaluateBuyer implements JavaDelegate {
 private final Buyer buyer;
 EvaluateBuyer(Buyer buyer) { this.buyer = buyer; }
 @Override public void execute(DelegateExecution e) {
  long price  = ((Number) e.getVariable("precioActual")).longValue();
  long p0     = ((Number) e.getVariable("precioInicial")).longValue();
  long budget = ((Number) e.getVariable("presupuestoCentimos")).longValue();
  e.setVariable("aceptable", buyer.accepts(price, p0, budget));
 }
}

/* =========================================================================
 *  5) SIGUIENTE OFERTA:  solo CALCULA precioNuevo (no toca precioActual)
 * ========================================================================= */
@Component("nextOffer")
class NextOffer implements JavaDelegate {
 private final Seller seller;
 NextOffer(Seller seller) { this.seller = seller; }
 @Override public void execute(DelegateExecution e) {
  long current = ((Number) e.getVariable("precioActual")).longValue();
  long pmin    = ((Number) e.getVariable("precioMinimo")).longValue();
  e.setVariable("precioNuevo", seller.nextOffer(current, pmin));
 }
}

/* =========================================================================
 *  6) APLICAR OFERTA:  precioActual = precioNuevo ; ronda = ronda + 1
 * ========================================================================= */
@Component("applyOffer")
class ApplyOffer implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  long nuevo = ((Number) e.getVariable("precioNuevo")).longValue();
  int  ronda = ((Number) e.getVariable("ronda")).intValue();
  e.setVariable("precioActual", nuevo);
  e.setVariable("ronda", ronda + 1);
 }
}

/* =========================================================================
 *  7) CONSTRUCTORES DE RESULTADO NORMAL
 *     En salidas normales: ultimoPrecioCentimos = precioActual ; rondasEvaluadas = ronda
 * ========================================================================= */
@Component("buildAgreement")
class BuildAgreement implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  e.setVariable("estado", "ACUERDO");
  e.setVariable("ultimoPrecioCentimos", ((Number) e.getVariable("precioActual")).longValue());
  e.setVariable("rondasEvaluadas",      ((Number) e.getVariable("ronda")).intValue());
  // motivo se deja sin fijar (null) en un acuerdo
 }
}

@Component("buildMaxRondas")
class BuildMaxRondas implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  e.setVariable("estado", "SIN_ACUERDO");
  e.setVariable("ultimoPrecioCentimos", ((Number) e.getVariable("precioActual")).longValue());
  e.setVariable("rondasEvaluadas",      ((Number) e.getVariable("ronda")).intValue());
  e.setVariable("motivo", "MAX_RONDAS");
 }
}

@Component("buildStalled")
class BuildStalled implements JavaDelegate {
 @Override public void execute(DelegateExecution e) {
  e.setVariable("estado", "SIN_ACUERDO");
  e.setVariable("ultimoPrecioCentimos", ((Number) e.getVariable("precioActual")).longValue());
  e.setVariable("rondasEvaluadas",      ((Number) e.getVariable("ronda")).intValue());
  e.setVariable("motivo", "PRECIO_ESTANCADO");
 }
}