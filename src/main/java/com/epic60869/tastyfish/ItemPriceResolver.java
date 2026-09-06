package com.epic60869.tastyfish;

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
    private static final String LOWEST_BINS_URL="https://lb.tricked.dev/lowestbins.json";
    private static final long CACHE_TIME_MILLIS=5*60*1000L;
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Map<String,Double> NPC_PRICES=new HashMap<>(),BAZAAR_PRICES=new HashMap<>(),LOWEST_BIN_PRICES=new HashMap<>();
    private static final Map<String,String> NAME_TO_ID=new HashMap<>(),ALIASES=new HashMap<>();
    private static final AtomicBoolean REFRESHING=new AtomicBoolean(false);
    private static volatile long lastRefresh=0L;
    private static volatile CompletableFuture<Void> refreshFuture=CompletableFuture.completedFuture(null);
    static{
        alias("dedication iv","ENCHANTMENT_DEDICATION_4"); alias("dedication 4","ENCHANTMENT_DEDICATION_4");
        alias("cultivating x","ENCHANTMENT_CULTIVATING_10"); alias("cultivating 10","ENCHANTMENT_CULTIVATING_10");
        alias("turbo wheat v","ENCHANTMENT_TURBO_WHEAT_5"); alias("turbo carrot v","ENCHANTMENT_TURBO_CARROT_5");
        alias("turbo potato v","ENCHANTMENT_TURBO_POTATO_5"); alias("turbo pumpkin v","ENCHANTMENT_TURBO_PUMPKIN_5");
        alias("turbo melon v","ENCHANTMENT_TURBO_MELON_5"); alias("turbo cocoa v","ENCHANTMENT_TURBO_COCOA_5");
        alias("turbo cactus v","ENCHANTMENT_TURBO_CACTUS_5"); alias("turbo mushrooms v","ENCHANTMENT_TURBO_MUSHROOMS_5");
        alias("turbo cane v","ENCHANTMENT_TURBO_CANE_5"); alias("turbo warts v","ENCHANTMENT_TURBO_WARTS_5");
        alias("pesterminator i","ENCHANTMENT_PESTERMINATOR_1"); alias("sunset i","ENCHANTMENT_SUNSET_1");
        alias("synthesis","SYNTHESIS_GARDEN_CHIP"); alias("synthesis chip","SYNTHESIS_GARDEN_CHIP");
        alias("evergreen","EVERGREEN_GARDEN_CHIP"); alias("evergreen chip","EVERGREEN_GARDEN_CHIP");
        alias("quickdraw","QUICKDRAW_GARDEN_CHIP"); alias("quickdraw chip","QUICKDRAW_GARDEN_CHIP");
        alias("hypercharge","HYPERCHARGE_GARDEN_CHIP"); alias("hypercharge chip","HYPERCHARGE_GARDEN_CHIP");
        alias("vermin vaporizer","VERMIN_VAPORIZER_GARDEN_CHIP"); alias("vermin vaporizer chip","VERMIN_VAPORIZER_GARDEN_CHIP");
        alias("rare finder","RAREFINDER_CHIP"); alias("rare finder chip","RAREFINDER_CHIP"); alias("rareﬁnder chip","RAREFINDER_CHIP");
        alias("cropshot","CROPSHOT_GARDEN_CHIP"); alias("cropshot chip","CROPSHOT_GARDEN_CHIP");
        alias("sowledge","SOWLEDGE_GARDEN_CHIP"); alias("sowledge chip","SOWLEDGE_GARDEN_CHIP");
        alias("mechamind","MECHAMIND_GARDEN_CHIP"); alias("mechamind chip","MECHAMIND_GARDEN_CHIP");
        alias("overdrive","OVERDRIVE_GARDEN_CHIP"); alias("overdrive chip","OVERDRIVE_GARDEN_CHIP");
    }
    private ItemPriceResolver(){}
    private static void alias(String n,String id){ALIASES.put(normalize(n),id);}
    public static void warmup(){ensureRefresh();}
    public static double value(String id){if(id==null||id.isBlank())return 0.0;Double b=BAZAAR_PRICES.get(id);if(b!=null&&b>0)return b;Double l=LOWEST_BIN_PRICES.get(id);if(l!=null&&l>0)return l;Double n=NPC_PRICES.get(id);return n==null?0.0:n;}
    public static double valueByName(String name){if(name==null||name.isBlank())return 0.0;ensureRefresh();String n=normalize(name),id=ALIASES.get(n);if(id==null)id=NAME_TO_ID.get(n);if(id==null)id=generatedId(n);return id==null?0.0:value(id);}
    public static CompletableFuture<Double> valueByNameAsync(String name){if(name==null||name.isBlank())return CompletableFuture.completedFuture(0.0);ensureRefresh();return refreshFuture.handle((ignored,error)->valueByName(name));}
    private static String normalize(String n){return n.toLowerCase(Locale.ROOT).replace('ﬁ','f').replaceAll("[^a-z0-9 ]","").replaceAll("\\s+"," ").trim();}
    private static String generatedId(String n){return switch(n){case "wheat"->"WHEAT";case "carrot"->"CARROT_ITEM";case "potato"->"POTATO_ITEM";case "pumpkin"->"PUMPKIN";case "melon"->"MELON";case "cactus"->"CACTUS";case "sugar cane"->"SUGAR_CANE";case "nether wart"->"NETHER_STALK";case "cocoa beans"->"INK_SACK:3";case "mushroom"->"MUSHROOM_COLLECTION";default->null;};}
    private static void ensureRefresh(){long now=System.currentTimeMillis();if(now-lastRefresh<CACHE_TIME_MILLIS||!REFRESHING.compareAndSet(false,true))return;CompletableFuture<HttpResponse<String>> items=get(ITEMS_URL),bazaar=get(BAZAAR_URL),bins=get(LOWEST_BINS_URL);refreshFuture=CompletableFuture.allOf(items,bazaar,bins).thenRun(()->{try{Map<String,Double> npc=parseNpc(items.join()),market=parseBazaar(bazaar.join()),low=parseBins(bins.join());Map<String,String> names=parseNames(items.join());synchronized(NPC_PRICES){NPC_PRICES.clear();NPC_PRICES.putAll(npc);}synchronized(BAZAAR_PRICES){BAZAAR_PRICES.clear();BAZAAR_PRICES.putAll(market);}synchronized(LOWEST_BIN_PRICES){LOWEST_BIN_PRICES.clear();LOWEST_BIN_PRICES.putAll(low);}synchronized(NAME_TO_ID){NAME_TO_ID.clear();NAME_TO_ID.putAll(names);}lastRefresh=System.currentTimeMillis();}catch(Throwable e){System.err.println("[TastyFish] Failed to parse item prices: "+rootMessage(e));}}).exceptionally(e->{System.err.println("[TastyFish] Failed to refresh item prices: "+rootMessage(e));return null;}).whenComplete((x,e)->REFRESHING.set(false));}
    private static CompletableFuture<HttpResponse<String>> get(String url){HttpRequest r=HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Accept","application/json").GET().build();return CLIENT.sendAsync(r,HttpResponse.BodyHandlers.ofString()).thenApply(x->{if(x.statusCode()<200||x.statusCode()>=300)throw new IllegalStateException("HTTP "+x.statusCode());return x;});}
    private static Map<String,Double> parseNpc(HttpResponse<String> r){Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("items")||!root.get("items").isJsonArray())return out;for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;JsonObject i=e.getAsJsonObject();if(!i.has("id")||!i.has("npc_sell_price"))continue;try{double p=i.get("npc_sell_price").getAsDouble();if(p>0)out.put(i.get("id").getAsString(),p);}catch(Exception ignored){}}return out;}
    private static Map<String,String> parseNames(HttpResponse<String> r){Map<String,String> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("items")||!root.get("items").isJsonArray())return out;for(JsonElement e:root.getAsJsonArray("items")){if(!e.isJsonObject())continue;JsonObject i=e.getAsJsonObject();if(i.has("id")&&i.has("name"))out.put(normalize(i.get("name").getAsString()),i.get("id").getAsString());}return out;}
    private static Map<String,Double> parseBazaar(HttpResponse<String> r){Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();if(!root.has("products")||!root.get("products").isJsonObject())return out;for(Map.Entry<String,JsonElement> e:root.getAsJsonObject("products").entrySet()){if(!e.getValue().isJsonObject())continue;JsonObject p=e.getValue().getAsJsonObject();if(!p.has("quick_status")||!p.get("quick_status").isJsonObject())continue;JsonObject q=p.getAsJsonObject("quick_status");if(!q.has("sellPrice"))continue;try{double v=q.get("sellPrice").getAsDouble();if(v>0)out.put(e.getKey(),v);}catch(Exception ignored){}}return out;}
    private static Map<String,Double> parseBins(HttpResponse<String> r){Map<String,Double> out=new HashMap<>();JsonObject root=JsonParser.parseString(r.body()).getAsJsonObject();for(Map.Entry<String,JsonElement> e:root.entrySet()){try{if(e.getValue().isJsonPrimitive()){double v=e.getValue().getAsDouble();if(v>0)out.put(e.getKey(),v);}}catch(Exception ignored){}}return out;}
    private static String rootMessage(Throwable e){Throwable c=e;while(c.getCause()!=null)c=c.getCause();return c.getMessage()==null?c.toString():c.getMessage();}
}
