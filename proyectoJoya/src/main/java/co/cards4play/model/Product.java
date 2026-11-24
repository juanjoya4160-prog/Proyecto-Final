package co.cards4play.model;
public abstract class Product {
  private final String id,name; private final double priceUsd;
  protected Product(String id,String name,double priceUsd){if(priceUsd<0)throw new IllegalArgumentException("Precio negativo");this.id=id;this.name=name;this.priceUsd=priceUsd;}
  public String getId(){return id;} public String getName(){return name;} public double getPriceUsd(){return priceUsd;}
}
