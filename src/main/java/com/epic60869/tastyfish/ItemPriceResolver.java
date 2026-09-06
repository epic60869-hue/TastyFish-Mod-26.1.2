package com.epic60869.tastyfish;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemPriceResolver {
    private static final String ITEMS_URL="https://api.hypixel.net/v2/resources/skyblock/items", BAZAAR_URL="https://api.hypixel.net/v2/skyblock/bazaar", LBIN_URL="https://moulberry.codes/lowestbin.json";
    private static final long CACHE=300000L;
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<String,Double> NPC=new ConcurrentHashMap<>(),BAZAAR=new ConcurrentHashMap<>(),LBIN=new ConcurrentHashMap<>();
    private static final Map<String,String> NAME_TO_ID=new ConcurrentHashMap<>();
    private static final AtomicBoolean REFRESHING=new AtomicBoolean();
    private static volatile long last;
    private ItemPriceResolver(){}

    public static void warm(){refresh(true);}

    public static double value(String id){
        if(id==null||id.isBlank())return 0;
        refresh(false);
        Double n=NPC.get(id); if(n!=null&&n>0)return n;
        Double b=BAZAAR.get(id); if(b!=null&&b>0)return b;
        Double l=LBIN.get(id); return l==null?0:l;
    }

    public static String idByName(String name){
        if(name==null||name.isBlank())return null;
        refresh(false);
        String clean=cleanName(name);
        String id=NAME_TO_ID.get(clean);
        if(id!=null)return id;
        String upper=clean.toUpperCase(Locale.ROOT).replace(' ','_');
        if(NPC.containsKey(upper)||BAZAAR.containsKey(upper)||LBIN.containsKey(upper))return upper;
        return null;
    }

    public static double valueByName(String name){
        if(name==null||name.isBlank())return 0;
        if(name.equalsIgnoreCase("Seasoning"))return -1;
        String id=idByName(name);
        if(id!=null){double v=value(id);if(v>0)return v;}
        return 0;
    }

    private static String cleanName(String s){
        return s.replaceAll("§[0-9a-fk-or]","").replaceAll("[✪★]+"," ").replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);
    }

    private static void refresh(boolean force){
        long now=System.currentTimeMillis();
        if(!force && now-last<CACHE)return;
        if(!REFRESHING.compareAndSet(false,true))return;
        fetch(ITEMS_URL).whenComplete((r,e)->{
            if(e==null){try{
                Map<String,Double> n=parseNpc(r.body());
                Map<String,String> names=parseNames(r.body());
                NPC.clear();NPC.putAll(n);NAME_TO_ID.putAll(names);
            }catch(Throwable t){System.err.println("[TastyFish] Hypixel item price parse failed: "+t);}}
        });
        fetch(BAZAAR_URL).whenComplete((r,e)->{
            if(e==null){try{BAZAAR.clear();BAZAAR.putAll(parseBazaar(r.body()));}
            catch(Throwable t){System.err.println("[TastyFish] Bazaar price parse failed: "+t);}}
        });
        fetch(LBIN_URL).whenComplete((r,e)->{
            if(e==null){try{LBIN.clear();LBIN.putAll(parseBin(r.body()));}
            catch(Throwable t){System.err.println("[TastyFish] LBIN price parse failed: "+t);}}
        });
        CompletableFuture.allOf(
            fetch(ITEMS_URL).exceptionally(e->null),
            fetch(BAZAAR_URL).exceptionally(e->null),
            fetch(LBIN_URL).exceptionally(e->null)
        ).whenComplete((v,e)->{last=System.currentTimeMillis();REFRESHING.set(false);});
    }

    private static CompletableFuture<HttpResponse<String>> fetch(String url){
        HttpRequest request=HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Accept","application/json").GET().build();
        return CLIENT.sendAsync(request,HttpResponse.BodyHandlers.ofString()).thenApply(r->{
            if(r.statusCode()<200||r.statusCode()>=300)throw new CompletionException(new IllegalStateException("HTTP "+r.statusCode()+" from "+url));
            return r;
        });
    }

    private static Map<String,Double> parseNpc(String body){
        Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(body).getAsJsonObject();
        if(!root.has("items")||!root.get("items").isJsonArray())return out;
        for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;try{JsonObject i=e.getAsJsonObject();double p=i.has("npc_sell_price")&&!i.get("npc_sell_price").isJsonNull()?i.get("npc_sell_price").getAsDouble():0;if(p>0)out.put(i.get("id").getAsString(),p);}catch(Exception ignored){}}
        return out;
    }

    private static Map<String,String> parseNames(String body){
        Map<String,String> out=new HashMap<>();JsonObject root=JsonParser.parseString(body).getAsJsonObject();
        if(!root.has("items")||!root.get("items").isJsonArray())return out;
        for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;try{JsonObject i=e.getAsJsonObject();String id=i.get("id").getAsString();String name=i.get("name").getAsString();out.put(cleanName(name),id);}catch(Exception ignored){}}
        return out;
    }

    private static Map<String,Double> parseBazaar(String body){
        Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(body).getAsJsonObject();
        if(!root.has("products")||!root.get("products").isJsonObject())return out;
        for(var e:root.getAsJsonObject("products").entrySet()){try{JsonObject q=e.getValue().getAsJsonObject().getAsJsonObject("quick_status");double buy=q.has("buyPrice")?q.get("buyPrice").getAsDouble():0;if(buy>0)out.put(e.getKey(),buy);}catch(Exception ignored){}}
        return out;
    }

    private static Map<String,Double> parseBin(String body){
        Map<String,Double> out=new HashMap<>();JsonElement root=JsonParser.parseString(body);if(!root.isJsonObject())return out;
        for(var e:root.getAsJsonObject().entrySet()){try{double p=e.getValue().getAsDouble();if(p>0)out.put(e.getKey(),p);}catch(Exception ignored){}}
        return out;
    }
}
