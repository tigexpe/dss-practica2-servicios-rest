package es.ugr.dss.flight;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("checkEmployee") class CheckEmployee implements JavaDelegate {
 private final HttpPartners partners; CheckEmployee(HttpPartners partners){this.partners=partners;}
 @Override public void execute(DelegateExecution e){
  try {
	  var info=partners.employee((String)e.getVariable("employeeId"));
	  e.setVariable("authorized",info.authorized());
	  e.setVariable("travelClass",info.travelClass());
  } catch(Exception ex){
	  e.setVariable("authorized",false);
	  e.setVariable("travelClass","NONE");
  }
 }
}
@Component("rejectEmployee") class RejectEmployee implements JavaDelegate {
 @Override public void execute(DelegateExecution e){e.setVariable("status","REJECTED");e.setVariable("detail","Employee not authorized or unknown");}
}
@Component("quoteIberia") class QuoteIberia implements JavaDelegate {
 private final HttpPartners partners;
 QuoteIberia(HttpPartners partners){this.partners=partners;}
 @Override public void execute(DelegateExecution e){
  var r=new TravelRequest(
		  (String)e.getVariable("employeeId"),
		  (String)e.getVariable("destination"),
		  (String)e.getVariable("departureDate"),
		  (String)e.getVariable("returnDate")
  );
  try {
	  var q=partners.quote("IBERIA",r,(String)e.getVariable("travelClass"));
	  //e.setVariable("status",q.available()?"QUOTED":"NO_OFFER");
	  //e.setVariable("selectedAirline",q.available()?q.airline():null);
	  //e.setVariable("totalPrice",q.price());
	  //e.setVariable("detail","Starter: Iberia only; implement parallel selection in Practice 2");
	  e.setVariable("iberiaAvailable", q.available());
	  e.setVariable("iberiaPrice", q.available() ? q.price() : null);
	  e.setVariable("iberiaFailed", false);
  }catch(Exception ex){
	  //e.setVariable("status","NO_OFFER");
	  //e.setVariable("detail","Iberia unavailable");
	  e.setVariable("iberiaAvailable", false);
	  e.setVariable("iberiaPrice", null);
	  e.setVariable("iberiaFailed", true);
  }
 }
}

@Component("quoteVueling") class QuoteVueling implements JavaDelegate {
	private final HttpPartners partners;
	QuoteVueling(HttpPartners partners) { this.partners = partners; }
	@Override public void execute(DelegateExecution e) {
		var r = new TravelRequest(
				(String) e.getVariable("employeeId"),
				(String) e.getVariable("destination"),
				(String) e.getVariable("departureDate"),
				(String) e.getVariable("returnDate")
		);
		try {
			var q = partners.quote("VUELING", r, (String)e.getVariable("travelClass"));
			e.setVariable("vuelingAvailable", q.available());
			e.setVariable("vuelingPrice", q.available() ? q.price() : null);
			e.setVariable("vuelingFail", false);
		} catch (Exception ex) {
			e.setVariable("vuelingAvailable", false);
			e.setVariable("vuelingPrice", null);
			e.setVariable("vuelingFail", true);
		}
	}
}

@Component("chooseOffer") class ChooseOffer implements JavaDelegate {
	@Override public void execute(DelegateExecution e) {
		boolean iberiaFailed = Boolean.TRUE.equals(e.getVariable("iberiaFailed"));
		boolean vuelingFailed = Boolean.TRUE.equals(e.getVariable("vuelingFailed"));
		BigDecimal iberiaPrice = (BigDecimal) e.getVariable("iberiaPrice");
		BigDecimal vuelingPrice = (BigDecimal) e.getVariable("vuelingPrice");
		
		// La oferta válida si está disponible y tiene precio
		boolean iberiaValid = Boolean.TRUE.equals(e.getVariable("iberiaAvailable")) && iberiaPrice != null;
		boolean vuelingValid = Boolean.TRUE.equals(e.getVariable("vuelingAvailable")) && vuelingPrice != null;
		
		if (iberiaValid && vuelingValid) {
			// si es menor que 0 significa que iberia sale más barata
			if (iberiaPrice.compareTo(vuelingPrice) <= 0) { quoted(e, "IBERIA", iberiaPrice); }
			else quoted(e, "VUELING", vuelingPrice);
		} else if (iberiaValid) {
			quoted(e, "IBERIA", iberiaPrice);
		} else if (vuelingValid) {
			quoted(e, "VUELING", vuelingPrice);
		} else {
			//no hay oferta válida así que se asigna status NO_OFFER
			e.setVariable("status", "NO_OFFER");
			e.setVariable("selectedAirline", null);
			e.setVariable("totalPrice", null);
			e.setVariable("detail","No offer. IBERIA: " + reason(iberiaFailed) + "; VUELING: " + reason(vuelingFailed));
		}
	}
	
	private void quoted(DelegateExecution e, String airline, BigDecimal price) {
		e.setVariable("status", "QUOTED");
		e.setVariable("selectedAirline", airline);
		e.setVariable("totalPrice", price);
		e.setVariable("detail","Cheapest valid offer: " + airline + " at " + price + " EUR");
	}
	
	private String reason(boolean failed) {
		return failed ? "query failed (HTTP error)" : "no availability";
	}
}
