package co.cards4play.model;
public abstract class User {
  private final String id, username, password; private final Role role;
  protected User(String id,String username,String password,Role role){this.id=id;this.username=username;this.password=password;this.role=role;}
  public String getId(){return id;} public String getUsername(){return username;} public String getPassword(){return password;} public Role getRole(){return role;}
}
