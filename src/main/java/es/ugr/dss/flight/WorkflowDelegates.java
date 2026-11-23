package es.ugr.dss.flight;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
@Component("checkEmployee") class CheckEmployee implements JavaDelegate {
 private final HttpPartners partners; CheckEmployee(HttpPartners partners){this.partners=partners;}
 @Override public void execute(DelegateExecution e){
  try {var info=partners.employee((String)e.getVariable("employeeId"));e.setVariable("authorized",info.authorized());e.setVariable("travelClass",info.travelClass());}
  catch(Exception ex){e.setVariable("authorized",false);e.setVariable("travelClass","NONE");}
 }
}
@Component("rejectEmployee") class RejectEmployee implements JavaDelegate {
 @Override public void execute(DelegateExecution e){e.setVariable("status","REJECTED");e.setVariable("detail","Employee not authorized or unknown");}
}
@Component("quoteIberia") class QuoteIberia implements JavaDelegate {
 private final HttpPartners partners;QuoteIberia(HttpPartners partners){this.partners=partners;}
 @Override public void execute(DelegateExecution e){
  var r=new TravelRequest((String)e.getVariable("employeeId"),(String)e.getVariable("destination"),(String)e.getVariable("departureDate"),(String)e.getVariable("returnDate"));
  try {var q=partners.quote("IBERIA",r,(String)e.getVariable("travelClass"));
   e.setVariable("status",q.available()?"QUOTED":"NO_OFFER");e.setVariable("selectedAirline",q.available()?q.airline():null);e.setVariable("totalPrice",q.price());e.setVariable("detail","Starter: Iberia only; implement parallel selection in Practice 2");
  }catch(Exception ex){e.setVariable("status","NO_OFFER");e.setVariable("detail","Iberia unavailable");}
 }
}
