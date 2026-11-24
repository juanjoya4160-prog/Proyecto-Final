package co.cards4play;

import static spark.Spark.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import co.cards4play.model.Rarity;

public class App {
  static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  static final Path FILE = Path.of(System.getenv().getOrDefault("DATA_FILE", "data/cards4play.json"));
  static final ThreadLocal<String> RATE_SOURCE = ThreadLocal.withInitial(() -> "DEFECTO");
  static ObjectNode db;
  static synchronized void save(){ try { Files.createDirectories(FILE.toAbsolutePath().getParent()); JSON.writerWithDefaultPrettyPrinter().writeValue(FILE.toFile(),db); } catch(Exception e){ throw new RuntimeException(e); } }
  static synchronized void load(){
    try { Files.createDirectories(FILE.toAbsolutePath().getParent()); db=Files.exists(FILE)?(ObjectNode)JSON.readTree(FILE.toFile()):JSON.createObjectNode(); }
    catch(Exception e){ throw new RuntimeException("No se pudo leer el archivo de datos",e); }
    for(String k:List.of("users","cards","sealed","accessories","boosters","tournaments","purchases")) if(!db.has(k))db.set(k,JSON.createArrayNode());
    if(db.withArray("users").findValue("role")==null || db.withArray("users").findValuesAsText("role").stream().noneMatch("ADMIN"::equals)) {
      ObjectNode admin=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("username","admin").put("password","admin123").put("role","ADMIN"); db.withArray("users").add(admin); save();
    }
  }
  static ObjectNode obj(){return JSON.createObjectNode();} static ArrayNode arr(String key){return (ArrayNode)db.get(key);}
  static ObjectNode find(String key,String id){for(JsonNode n:arr(key))if(n.path("id").asText().equals(id))return (ObjectNode)n;return null;}
  static ObjectNode byName(String key,String name){for(JsonNode n:arr(key))if(n.path("username").asText().equals(name))return (ObjectNode)n;return null;}
  static ObjectNode body(spark.Request req){try{return (ObjectNode)JSON.readTree(req.body());}catch(Exception e){throw new HttpError(400,"Cuerpo JSON inválido");}}
  static String required(ObjectNode b,String k){String s=b.path(k).asText("").trim();if(s.isEmpty())throw new HttpError(400,"Falta el campo: "+k);return s;}
  static class HttpError extends RuntimeException { int status; HttpError(int s,String m){super(m);status=s;} }
  static ObjectNode user(spark.Request req){String u=req.headers("X-Username"),p=req.headers("X-Password");if(u==null||p==null)throw new HttpError(401,"Autenticación requerida mediante X-Username y X-Password");ObjectNode x=byName("users",u);if(x==null||!x.path("password").asText().equals(p))throw new HttpError(401,"Credenciales inválidas");return x;}
  static ObjectNode admin(spark.Request r){ObjectNode u=user(r);if(!u.path("role").asText().equals("ADMIN"))throw new HttpError(403,"Acción exclusiva del administrador");return u;}
  static ObjectNode client(spark.Request r){ObjectNode u=user(r);if(!u.path("role").asText().equals("CLIENT"))throw new HttpError(403,"Acción exclusiva de clientes");return u;}
  static ObjectNode product(String id,String name,String type,double usd){return obj().put("id",id).put("name",name).put("type",type).put("priceUsd",usd);}
  static ObjectNode visibleBooster(JsonNode b){ObjectNode o=((ObjectNode)b).deepCopy();o.remove("cardIds");if(!o.path("opened").asBoolean())o.remove("cards");return o;}
  static ObjectNode error(String m){return obj().put("error",m);}
  static double rate(){try{HttpRequest req=HttpRequest.newBuilder(URI.create("https://open.er-api.com/v6/latest/USD")).timeout(Duration.ofSeconds(3)).GET().build();String response=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString()).body();double v=JSON.readTree(response).path("rates").path("COP").asDouble(0);if(v>0){RATE_SOURCE.set("API");return v;}}catch(Exception ignored){}double fallback=Double.parseDouble(System.getenv().getOrDefault("TASA_DEFECTO","4000"));RATE_SOURCE.set("DEFECTO");System.out.println("La API no respondió, por lo que se toma un valor por defecto de "+fallback);return fallback;}
  static ObjectNode priced(JsonNode product,double r,String source){ObjectNode o=((ObjectNode)product).deepCopy();o.put("priceCop",Math.round(o.path("priceUsd").asDouble()*r*100.0)/100.0).put("fuenteTasa",source);return o;}
  static ObjectNode newProduct(String name,String type,double price){if(price<0)throw new HttpError(400,"El precio debe ser positivo");return product(UUID.randomUUID().toString(),name,type,price);}
  static void created(spark.Response res){res.status(201);}
  public static void main(String[] args){
    ipAddress("0.0.0.0"); port(Integer.parseInt(System.getenv().getOrDefault("PORT","4567"))); load();
    before((req,res)->{res.type("application/json; charset=utf-8");});
    exception(HttpError.class,(e,req,res)->{res.status(e.status);res.body(error(e.getMessage()).toString());});
    exception(Exception.class,(e,req,res)->{e.printStackTrace();res.status(500);res.body(error("Error interno del servidor").toString());});
    get("/",(q,s)->obj().put("service","Cards4Play API").put("status","ok"));
    post("/auth/login",(q,s)->{ObjectNode b=body(q),u=byName("users",required(b,"username"));if(u==null||!u.path("password").asText().equals(required(b,"password")))throw new HttpError(401,"Credenciales inválidas");return obj().put("id",u.path("id").asText()).put("username",u.path("username").asText()).put("role",u.path("role").asText()).put("tokenType","Basic headers X-Username/X-Password");});
    post("/clients",(q,s)->{admin(q);ObjectNode b=body(q);String name=required(b,"username"),pass=required(b,"password");if(byName("users",name)!=null)throw new HttpError(409,"El usuario ya existe");ObjectNode u=obj().put("id",UUID.randomUUID().toString()).put("username",name).put("password",pass).put("role","CLIENT");arr("users").add(u);save();created(s);return obj().put("id",u.path("id").asText()).put("username",name).put("role","CLIENT");});
    get("/clients",(q,s)->{admin(q);ArrayNode out=JSON.createArrayNode();for(JsonNode u:arr("users"))if(u.path("role").asText().equals("CLIENT"))out.add(obj().put("id",u.path("id").asText()).put("username",u.path("username").asText()));return out;});
    post("/cards",(q,s)->{admin(q);ObjectNode b=body(q);String rarity=required(b,"rarity");try{Rarity.valueOf(rarity);}catch(Exception e){throw new HttpError(400,"Rareza inválida: use N, UC, R, SR o UR");}ObjectNode c=newProduct(required(b,"name"),"CARD",b.path("priceUsd").asDouble(-1));c.put("rarity",rarity);arr("cards").add(c);save();created(s);return c;});
    get("/cards",(q,s)->{user(q);double r=rate();String source=rateSource(r);ArrayNode out=JSON.createArrayNode();for(JsonNode p:arr("cards"))out.add(priced(p,r,source));ObjectNode response=obj().put("tasaCopPorUsd",r).put("fuenteTasa",source);response.set("items",out);return response;});
    post("/products/sealed",(q,s)->addGeneric(q,s,"sealed","SEALED")); post("/products/accessories",(q,s)->addGeneric(q,s,"accessories","ACCESSORY"));
    get("/products",(q,s)->{user(q);double r=rate();ArrayNode all=JSON.createArrayNode();for(String key:List.of("sealed","accessories"))for(JsonNode p:arr(key))all.add(priced(p,r,rateSource(r)));return all;});
    post("/boosters",(q,s)->{admin(q);ArrayNode cards=arr("cards");if(cards.isEmpty())throw new HttpError(409,"No hay cartas registradas para crear el booster");ObjectNode b=obj().put("id",UUID.randomUUID().toString()).put("type","BOOSTER").put("name",body(q).path("name").asText("Booster")).put("priceUsd",body(q).path("priceUsd").asDouble(5)).put("opened",false).put("ownerId","");ArrayNode chosen=JSON.createArrayNode();for(int i=0;i<10;i++){Rarity wanted=drawRarity();List<JsonNode> pool=new ArrayList<>();for(JsonNode c:cards)if(c.path("rarity").asText().equals(wanted.name()))pool.add(c);if(pool.isEmpty()){for(int d=1;d<5&&pool.isEmpty();d++){for(Rarity rr:Rarity.values())if(rr.rank()==wanted.rank()-d)for(JsonNode c:cards)if(c.path("rarity").asText().equals(rr.name()))pool.add(c);}for(int d=1;d<5&&pool.isEmpty();d++){for(Rarity rr:Rarity.values())if(rr.rank()==wanted.rank()+d)for(JsonNode c:cards)if(c.path("rarity").asText().equals(rr.name()))pool.add(c);}}chosen.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())).path("id").asText());}b.set("cardIds",chosen);arr("boosters").add(b);save();created(s);return visibleBooster(b);});
    get("/boosters",(q,s)->{user(q);ArrayNode out=JSON.createArrayNode();for(JsonNode b:arr("boosters"))if(b.path("ownerId").asText().isEmpty())out.add(visibleBooster(b));return out;});
    get("/boosters/:id",(q,s)->{ObjectNode viewer=user(q),b=find("boosters",q.params(":id"));if(b==null)throw new HttpError(404,"Booster inexistente");String owner=b.path("ownerId").asText("");if(!owner.isEmpty()&&!owner.equals(viewer.path("id").asText())&&!viewer.path("role").asText().equals("ADMIN"))throw new HttpError(403,"El booster pertenece a otro cliente");return visibleBooster(b);});
    post("/sealed",(q,s)->addGeneric(q,s,"sealed","SEALED"));
    post("/accessories",(q,s)->addGeneric(q,s,"accessories","ACCESSORY"));
    post("/purchases",(q,s)->{ObjectNode u=client(q),b=body(q);String id=required(b,"productId");ObjectNode bought=find("boosters",id);if(bought!=null){if(!bought.path("ownerId").asText().isEmpty())throw new HttpError(409,"El booster ya fue vendido");bought.put("ownerId",u.path("id").asText());}else {bought=find("sealed",id);if(bought==null)bought=find("accessories",id);if(bought==null)throw new HttpError(404,"Producto inexistente");}ObjectNode p=obj().put("id",UUID.randomUUID().toString()).put("clientId",u.path("id").asText()).put("productId",id).put("date",Instant.now().toString());arr("purchases").add(p);save();created(s);return p;});
    post("/boosters/:id/open",(q,s)->{ObjectNode u=client(q),b=find("boosters",q.params(":id"));if(b==null)throw new HttpError(404,"Booster inexistente");if(!b.path("ownerId").asText().equals(u.path("id").asText()))throw new HttpError(403,"El booster pertenece a otro cliente o no ha sido comprado");if(b.path("opened").asBoolean())throw new HttpError(409,"El booster ya fue abierto");ArrayNode cards=JSON.createArrayNode();for(JsonNode id:b.withArray("cardIds")){ObjectNode c=find("cards",id.asText());if(c!=null)cards.add(c);}b.put("opened",true);b.set("cards",cards);save();return b;});
    get("/purchases",(q,s)->{ObjectNode u=client(q);ArrayNode out=JSON.createArrayNode();for(JsonNode p:arr("purchases"))if(p.path("clientId").asText().equals(u.path("id").asText())){ObjectNode row=((ObjectNode)p).deepCopy();ObjectNode booster=find("boosters",p.path("productId").asText());if(booster!=null){if(booster.path("opened").asBoolean())row.set("producto",booster.deepCopy());else row.set("producto",obj().put("id",booster.path("id").asText()).put("type","BOOSTER").put("opened",false));}else{ObjectNode prod=find("sealed",p.path("productId").asText());if(prod==null)prod=find("accessories",p.path("productId").asText());if(prod!=null)row.set("producto",prod);}out.add(row);}return out;});
    post("/tournaments",(q,s)->{admin(q);ObjectNode b=body(q);int cap=b.path("capacity").asInt(0);if(cap<1)throw new HttpError(400,"capacity debe ser mayor que cero");ObjectNode t=obj().put("id",UUID.randomUUID().toString()).put("name",required(b,"name")).put("date",required(b,"date")).put("capacity",cap);t.set("participants",JSON.createArrayNode());arr("tournaments").add(t);save();created(s);return t;});
    get("/tournaments",(q,s)->{user(q);return arr("tournaments");});
    post("/tournaments/:id/register",(q,s)->{ObjectNode u=client(q),t=find("tournaments",q.params(":id"));if(t==null)throw new HttpError(404,"Torneo inexistente");ArrayNode ps=t.withArray("participants");for(JsonNode p:ps)if(p.asText().equals(u.path("id").asText()))throw new HttpError(409,"El cliente ya está inscrito");if(ps.size()>=t.path("capacity").asInt())throw new HttpError(409,"El torneo no tiene cupos disponibles");ps.add(u.path("id").asText());save();return obj().put("tournamentId",t.path("id").asText()).put("clientId",u.path("id").asText()).put("registered",true);});
    notFound((q,s)->{s.status(404);return error("Ruta inexistente");});
  }
  static String rateSource(double r){return RATE_SOURCE.get();}
  static Rarity drawRarity(){int n=ThreadLocalRandom.current().nextInt(100);int sum=0;for(Rarity r:Rarity.values()){sum+=r.probability();if(n<sum)return r;}return Rarity.N;}
  static Object addGeneric(spark.Request q,spark.Response s,String key,String type){admin(q);ObjectNode b=body(q),p=newProduct(required(b,"name"),type,b.path("priceUsd").asDouble(-1));arr(key).add(p);save();created(s);return p;}
}
