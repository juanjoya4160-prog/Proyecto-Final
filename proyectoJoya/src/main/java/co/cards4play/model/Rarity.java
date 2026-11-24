package co.cards4play.model;
public enum Rarity {
  N(50,0), UC(25,1), R(15,2), SR(7,3), UR(3,4);
  private final int probability, rank;
  Rarity(int probability,int rank){this.probability=probability;this.rank=rank;}
  public int probability(){return probability;} public int rank(){return rank;}
}
