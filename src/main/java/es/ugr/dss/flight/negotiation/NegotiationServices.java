package es.ugr.dss.flight.negotiation;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("catalog")
class Catalog {
	record Product(int stock, long p0, long pmin) {}
	
	private final Map<String, Product> products = Map.of(
			//"CAMISA", new Product(10, 10000L, 6000L),
			//"TRAJE", new Product(10, 20000L, 16000L),
			//"ZAPATOS", new Product(10, 8000L, 5000L)
			"CAMISA", new Product(10, 10000L, 6000L),
			"TRAJE", new Product(3, 20000L, 16000L),
			"ZAPATOS", new Product(0, 8000L, 5000L)
	);
	
	/** Devuelve el producto, o null si no está en el catálogo. */
	Product find(String name) {
		return products.get(name);
	}
}

/**
 * Comprador. Acepta el precio si cabe en su presupuesto Y no supera el 70% de P0.
 * Toda la aritmética es entera (céntimos), con división entera como pide el guión.
 */
@Component("buyer")
class Buyer {
	boolean accepts(long price, long p0, long budget) {
		return price <= budget && price <= (70L * p0) / 100L;
	}
}


/**
 * Vendedor. Baja la oferta un 10% (división entera), pero nunca por debajo
 * del mínimo comercial Pmin.
 */
@Component("seller")
class Seller {
	long nextOffer(long current, long pmin) {
		return Math.max((90L * current) / 100L, pmin);
	}
}