package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemPriceResolver {
    private static final String ITEMS_URL="https://api.hypixel.net/v2/resources/skyblock/items";
    private static final String BAZAAR_URL="https://api.hypixel.net/v2/skyblock/bazaar";
    private static final long CACHE_TIME_MILLIS=5*60*1000L;
    private static final Gson GSON=new Gson();
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<String,Double> NPC_PRICES=new HashMap<>(),BAZAAR_PRICES=new HashMap<>();
    private static final Map<String,String> NAME_TO_ID=new HashMap<>();
    private static final AtomicBoolean REFRESHING=new AtomicBoolean(false);
    private static volatile long lastRefresh=0L;
    private ItemPriceResolver(){}
    public static double value(String itemId){if(itemId==null||itemId.isBlank())return 0.0;refreshIfNeeded();Double n=NPC_PRICES.get(itemId);if(n!=null)return n;Double b=BAZAAR_PRICES.get(itemId);return b==null?0.0:b;}
    public static double valueByName(String name){if(name==null||name.isBlank())return 0.0;refreshIfNeeded();String id=NAME_TO_ID.get(normalize(name));if(id!=null)return value(id);String fallback=legacyId(normalize(name));return fallback==null?0.0:value(fallback);}
    private static String normalize(String n){return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]","").replaceAll("\\s+"," ").trim();}
    private static String legacyId(String n){return switch(n){case "wheat"->"WHEAT";case "carrot"->"CARROT_ITEM";case "potato"->"POTATO_ITEM";case "pumpkin"->"PUMPKIN";case "melon"->"MELON";case "cactus"->"CACTUS";case "sugar cane"->"SUGAR_CANE";case "nether wart"->"NETHER_STALK";case "cocoa beans"->"INK_SACK:3";default->null;};}
    private static void refreshIfNeeded(){long now=System.currentTimeMillis();if(now-lastRefresh<CACHE_TIME_MILLIS||!REFRESHING.compareAndSet(false,true))return;CompletableFuture<HttpResponse<String>> a=get(ITEMS_URL),b=get(BAZAAR_URL);a.thenCombine(b,(ir,br)->{Map<String,Double> npc=parseNpc(ir),baz=parseBazaar(br);Map<String,String> names=parseNames(ir);synchronized(NPC_PRICES){NPC_PRICES.clear();NPC_PRICES.putAll(npc);}synchronized(BAZAAR_PRICES){BAZAAR_PRICES.clear();BAZAAR_PRICES.putAll(baz);}synchronized(NAME_TO_ID){NAME_TO_ID.clear();NAME_TO_ID.putAll(names);}lastRefresh=System.currentTimeMillis();return null;}).exceptionally(e->{System.err.println("[TastyFish] Price refresh failed: "+e);return null;}).whenComplete((x,e)->REFRESHING.set(false));}
    private static CompletableFuture<HttpResponse<String>> get(String url){HttpRequest r=HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Accept","application/json").GET().build();return CLIENT.sendAsync(r,HttpResponse.BodyHandlers.ofString()).thenApply(x->{if(x.statusCode()<200||x.statusCode()>=300)throw new IllegalStateException("HTTP "+x.statusCode());return x;});}
    private static Map<String,Double> parseNpc(HttpResponse<String> r){Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("items"))return out;for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;JsonObject i=e.getAsJsonObject();if(i.has("id")&&i.has("npc_sell_price"))try{double p=i.get("npc_sell_price").getAsDouble();if(p>0)out.put(i.get("id").getAsString(),p);}catch(Exception ignored){}}return out;}
    private static Map<String,String> parseNames(HttpResponse<String> r){Map<String,String> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("items"))return out;for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;JsonObject i=e.getAsJsonObject();if(i.has("id")&&i.has("name"))out.put(normalize(i.get("name").getAsString()),i.get("id").getAsString());}return out;}
    private static Map<String,Double> parseBazaar(HttpResponse<String> r){Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("products"))return out;for(Map.Entry<String,JsonElement> e:root.getAsJsonObject("products").entrySet()){if(!e.getValue().isJsonObject())continue;JsonObject p=e.getValue().getAsJsonObject();if(!p.has("quick_status"))continue;JsonObject q=p.getAsJsonObject("quick_status");if(!q.has("buyPrice"))continue;try{double v=q.get("buyPrice").getAsDouble();if(v>0)out.put(e.getKey(),v);}catch(Exception ignored){}}return out;}
}
