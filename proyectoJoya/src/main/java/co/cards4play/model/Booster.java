package co.cards4play.model;
import java.util.*;
public final class Booster extends Product {
  private final List<String> cardIds; private String ownerId; private boolean opened;
  public Booster(String id,String name,double priceUsd,List<String> cardIds){super(id,name,priceUsd);if(cardIds.size()!=10)throw new IllegalArgumentException("El booster debe tener diez cartas");this.cardIds=List.copyOf(cardIds);}
  public List<String> getCardIds(){return cardIds;} public String getOwnerId(){return ownerId;} public boolean isOpened(){return opened;}
  public void purchase(String clientId){if(ownerId!=null)throw new IllegalStateException("Booster vendido");ownerId=clientId;}
  public void open(String clientId){if(!Objects.equals(ownerId,clientId))throw new SecurityException("No es el propietario");if(opened)throw new IllegalStateException("Booster abierto");opened=true;}
}
