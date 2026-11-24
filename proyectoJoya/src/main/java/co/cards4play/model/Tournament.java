package co.cards4play.model;
import java.util.*;
public final class Tournament {
  private final String id,name,date; private final int capacity; private final Set<String> participants=new LinkedHashSet<>();
  public Tournament(String id,String name,String date,int capacity){if(capacity<1)throw new IllegalArgumentException("Cupo inválido");this.id=id;this.name=name;this.date=date;this.capacity=capacity;}
  public void register(String clientId){if(participants.contains(clientId))throw new IllegalStateException("Inscripción duplicada");if(participants.size()>=capacity)throw new IllegalStateException("Torneo lleno");participants.add(clientId);}
  public Set<String> getParticipants(){return Collections.unmodifiableSet(participants);} public String getId(){return id;} public String getName(){return name;} public String getDate(){return date;} public int getCapacity(){return capacity;}
}
