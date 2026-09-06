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
    private static final long CACHE=300000L; private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Gson GSON=new Gson(); private static final Map<String,Double> NPC=new HashMap<>(),BAZAAR=new HashMap<>(),LBIN=new HashMap<>(); private static final AtomicBoolean REFRESHING=new AtomicBoolean(); private static volatile long last;
    private ItemPriceResolver(){}
    public static double value(String id){if(id==null||id.isBlank())return 0;refresh();Double n=NPC.get(id);if(n!=null&&n>0)return n;Double b=BAZAAR.get(id);if(b!=null&&b>0)return b;Double l=LBIN.get(id);return l==null?0:l;}
    private static void refresh(){long now=System.currentTimeMillis();if(now-last<CACHE||!REFRESHING.compareAndSet(false,true))return;CompletableFuture<HttpResponse<String>> a=get(ITEMS_URL),b=get(BAZAAR_URL),c=get(LBIN_URL);CompletableFuture.allOf(a,b,c).thenRun(()->{try{Map<String,Double> n=parseNpc(a.join()),bz=parseBazaar(b.join()),lb=parseBin(c.join());synchronized(NPC){NPC.clear();NPC.putAll(n);}synchronized(BAZAAR){BAZAAR.clear();BAZAAR.putAll(bz);}synchronized(LBIN){LBIN.clear();LBIN.putAll(lb);}last=System.currentTimeMillis();}catch(Throwable e){System.err.println("[TastyFish] Price refresh failed: "+e);}}).whenComplete((v,e)->REFRESHING.set(false));}
    private static CompletableFuture<HttpResponse<String>> get(String url){return CLIENT.sendAsync(HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Accept","application/json").GET().build(),HttpResponse.BodyHandlers.ofString()).thenApply(r->{if(r.statusCode()<200||r.statusCode()>=300)throw new IllegalStateException("HTTP "+r.statusCode());return r;});}
    private static Map<String,Double> parseNpc(HttpResponse<String> r){Map<String,Double> o=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("items"))return o;for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;JsonObject i=e.getAsJsonObject();try{double p=i.get("npc_sell_price").getAsDouble();if(p>0)o.put(i.get("id").getAsString(),p);}catch(Exception ignored){}}return o;}
    private static Map<String,Double> parseBazaar(HttpResponse<String> r){Map<String,Double> o=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("products"))return o;for(var e:root.getAsJsonObject("products").entrySet()){try{double p=e.getValue().getAsJsonObject().getAsJsonObject("quick_status").get("buyPrice").getAsDouble();if(p>0)o.put(e.getKey(),p);}catch(Exception ignored){}}return o;}
    private static Map<String,Double> parseBin(HttpResponse<String> r){Map<String,Double> o=new HashMap<>();JsonElement root=JsonParser.parseString(r.body());if(!root.isJsonObject())return o;for(var e:root.getAsJsonObject().entrySet()){try{double p=e.getValue().getAsDouble();if(p>0)o.put(e.getKey(),p);}catch(Exception ignored){}}return o;}
}
