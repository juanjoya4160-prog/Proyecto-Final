package co.cards4play.model;
public final class Card extends Product {
  private final Rarity rarity;
  public Card(String id,String name,double priceUsd,Rarity rarity){super(id,name,priceUsd);this.rarity=rarity;}
  public Rarity getRarity(){return rarity;}
}
