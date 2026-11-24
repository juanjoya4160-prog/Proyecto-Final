package co.cards4play.model;
import java.time.Instant;
public final class Purchase {
  private final String id,clientId,productId; private final Instant date;
  public Purchase(String id,String clientId,String productId,Instant date){this.id=id;this.clientId=clientId;this.productId=productId;this.date=date;}
  public String getId(){return id;} public String getClientId(){return clientId;} public String getProductId(){return productId;} public Instant getDate(){return date;}
}
