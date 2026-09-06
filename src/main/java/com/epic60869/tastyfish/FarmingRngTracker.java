package com.epic60869.tastyfish;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FarmingRngTracker {
    private static final FarmingRngTracker INSTANCE = new FarmingRngTracker();
    private static final long SHOW_MILLIS = 5000L;
    private static final Pattern RARE_MESSAGE = Pattern.compile("(?i)^(?:.*?)(?:RARE CROP!|(?:VERY |CRAZY |PRAY TO RNGESUS |RNGESUS INCARNATE )?RARE DROP!?|VERY RARE DROP!?|CRAZY RARE DROP!?|PRAY TO RNGESUS!?|RNGESUS INCARNATE!?)[ ]*(?<item>.+?)\\s*(?:\\(\\+[^)]*\\))?[! ]*$");
    private static final Pattern OLDER_DROP = Pattern.compile("(?i)(?:you (?:found|dropped|got)|you received|drop(?:ped)?[: ])\\s*(?:an? |some )?(?<item>.+?)(?:!|$)");
    private static final Pattern QUANTITY = Pattern.compile("(?i)^(?:x(?<x1>\\d+)\\s+|(?<x2>\\d+)x\\s+)(?<item>.+)$");
    private static final Set<String> DROPS = Set.of(
        "Cornucopia","Carrot Zest","Deepfries","Aggourdian","Cane Knot","Melon Juice","Cactus Flower",
        "Designer Coffee Beans","Feastfungus","Botroot","Salted Sunflower Seeds","Crystalized Moonlight","Floral Gelatin","Helianthus","Seasoning",
        "Cropie","Squash","Fermento","Burrowing Spores","Overgrown Grass","Green Bandana","Dedication IV","Dedication 4","Flowering Bouquet","Rooted Spores","Fruit Bowl","Atmospheric Filter",
        "Beady Eyes","Chirping Stereo","Clipped Wings","Bookworm's Favorite Book","Mantid Claw","Wriggling Larva","Locust Larva","Squeaky Toy","Squeaky Mousemat","Vermin Vaporizer","Vermin Vaporizer Chip",
        "Pesterminator I","Slug Pet","Rat Pet","Synthesis","Synthesis Chip","Evergreen","Evergreen Chip","Quickdraw","Quickdraw Chip","Hypercharge","Hypercharge Chip","Fire in a Bottle","Iridium",
        "Overclocker 3000","Rabbit Hat","Lucky Clover Core","Bulky Stone","Rarefinder Chip","Cropshot Chip","Sowledge Chip","Mechamind Chip","Overdrive Chip","Sunset I",
        "Turbo-Wheat V","Turbo-Carrot V","Turbo-Potato V","Turbo-Pumpkin V","Turbo-Melon V","Turbo-Cocoa V","Turbo-Cactus V","Turbo-Mushrooms V","Turbo-Cane V","Turbo-Warts V","Cultivating X","Cultivating 10");
    private volatile Drop active;
    private FarmingRngTracker(){}
    public static FarmingRngTracker get(){return INSTANCE;}
    public void register(){ItemPriceResolver.warmup();ClientReceiveMessageEvents.CHAT.register((message,signed,sender,params,timestamp)->handle(message));ClientReceiveMessageEvents.GAME.register((message,overlay)->handle(message));}
    public Drop active(){Drop d=active;if(d!=null&&System.currentTimeMillis()>d.shownUntil()){active=null;return null;}return d;}
    private void handle(Component message){if(Minecraft.getInstance().player==null)return;String raw=message.getString().replaceAll("§[0-9a-fk-or]","").trim();Parsed p=parse(raw);if(p==null)return;long until=System.currentTimeMillis()+SHOW_MILLIS;if("Seasoning".equalsIgnoreCase(p.name())){active=new Drop(p.amount(),p.name(),rarity(raw),-1L,until);return;}ItemPriceResolver.valueByNameAsync(p.name()).thenAccept(price->active=new Drop(p.amount(),p.name(),rarity(raw),Math.round(price),until));}
    private Parsed parse(String raw){Matcher m=RARE_MESSAGE.matcher(raw);String item;if(m.find())item=m.group("item").trim();else{String l=raw.toLowerCase(Locale.ROOT);if(!l.contains("rare drop")&&!l.contains("rngesus")&&!l.contains("rare crop"))return null;Matcher o=OLDER_DROP.matcher(raw);if(!o.find())return null;item=o.group("item").trim();}item=item.replaceAll("\\s*\\(\\+[^)]*\\)\\s*$","").replaceAll("!$","").trim();int amount=1;Matcher q=QUANTITY.matcher(item);if(q.matches()){try{amount=Integer.parseInt(q.group("x1")!=null?q.group("x1"):q.group("x2"));}catch(Exception ignored){}item=q.group("item").trim();}for(String known:DROPS)if(item.equalsIgnoreCase(known)||item.toLowerCase(Locale.ROOT).contains(known.toLowerCase(Locale.ROOT)))return new Parsed(amount,known);return null;}
    private static String rarity(String s){String t=s.toLowerCase(Locale.ROOT);if(t.contains("rare crop"))return "RARE CROP";if(t.contains("crazy rare"))return "CRAZY RARE";if(t.contains("rngesus"))return "RNGESUS";return "RARE DROP";}
    public record Drop(int amount,String name,String rarity,long unitPrice,long shownUntil){}
    private record Parsed(int amount,String name){}
}
