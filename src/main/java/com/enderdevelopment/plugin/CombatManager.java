package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class CombatManager implements Listener {
    private final Main plugin;
    private final Set<UUID> fighters = new HashSet<>();
    private final Set<UUID> blocking = new HashSet<>();
    private final Map<UUID, Kit> kits = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> combo = new HashMap<>();
    private final Map<UUID, Long> lastCombo = new HashMap<>();
    private final Map<UUID, Integer> ultimate = new HashMap<>();
    private final Map<UUID, Long> blockUntil = new HashMap<>();
    private final BukkitTask cleanupTask;

    public CombatManager(Main plugin) {
        this.plugin = plugin;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 1L, 5L);
    }

    public void shutdown() {
        cleanupTask.cancel();
        for (UUID uuid : Set.copyOf(fighters)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) leave(player, false);
        }
    }

    public void join(Player player) {
        fighters.add(player.getUniqueId());
        kits.putIfAbsent(player.getUniqueId(), Kit.EMBER);
        ultimate.putIfAbsent(player.getUniqueId(), 0);
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(Component.text("You entered the arena. Choose a kit with /arena kit.", NamedTextColor.GREEN));
        showStatus(player);
    }

    public void leave(Player player, boolean message) {
        UUID uuid = player.getUniqueId();
        fighters.remove(uuid);
        blocking.remove(uuid);
        combo.remove(uuid);
        if (message) player.sendMessage(Component.text("You left the arena.", NamedTextColor.YELLOW));
    }

    public void selectKit(Player player, String value) {
        Kit kit = Kit.from(value);
        if (kit == null) {
            player.sendMessage(Component.text("Available kits: ember, tide, stone.", NamedTextColor.RED));
            return;
        }
        kits.put(player.getUniqueId(), kit);
        player.sendMessage(Component.text("Kit selected: " + kit.displayName, kit.color));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!fighters.contains(player.getUniqueId())) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            basicAttack(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            ability(player, player.getInventory().getHeldItemSlot());
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (fighters.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            ultimateAttack(event.getPlayer());
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && fighters.contains(event.getPlayer().getUniqueId())) dash(event.getPlayer());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        if (fighters.contains(event.getPlayer().getUniqueId())) showStatus(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { leave(event.getPlayer(), false); }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && fighters.contains(player.getUniqueId()) && blocking.contains(player.getUniqueId())) {
            event.setDamage(event.getDamage() * 0.25);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 4, .2, .2, .2, .05);
        }
    }

    private void basicAttack(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - lastCombo.getOrDefault(uuid, 0L) > 900) combo.put(uuid, 0);
        int step = combo.getOrDefault(uuid, 0) + 1;
        combo.put(uuid, step > 4 ? 1 : step);
        lastCombo.put(uuid, now);
        double damage = step == 4 ? 5.5 : 3.0;
        hitCone(player, damage, 2.8, step == 4 ? 0.8 : 0.45);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 0.9f + step * .08f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(player.getLocation().getDirection().multiply(1.2)).add(0, 1, 0), 1);
        gainUltimate(player, 4);
    }

    private void ability(Player player, int slot) {
        Kit kit = kits.getOrDefault(player.getUniqueId(), Kit.EMBER);
        int ability = slot % 3;
        if (ability == 0) castBurst(player, kit);
        else if (ability == 1) castLunge(player, kit);
        else castGuardBreak(player, kit);
    }

    private void castBurst(Player player, Kit kit) {
        if (!ready(player, "burst", 4500)) return;
        hitCone(player, kit == Kit.STONE ? 7 : 5, 4.2, .7);
        player.getWorld().spawnParticle(kit.particle, player.getLocation().add(0, 1, 0), 28, 1.2, .5, 1.2, .12);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, .6f, 1.5f);
        gainUltimate(player, 8);
    }

    private void castLunge(Player player, Kit kit) {
        if (!ready(player, "lunge", 6000)) return;
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(1.35).setY(.18));
        Bukkit.getScheduler().runTaskLater(plugin, () -> hitCone(player, kit == Kit.TIDE ? 8 : 6, 2.5, .55), 6L);
        player.getWorld().spawnParticle(kit.particle, player.getLocation().add(0, 1, 0), 16, .4, .4, .4, .08);
    }

    private void castGuardBreak(Player player, Kit kit) {
        if (!ready(player, "break", 7500)) return;
        for (Entity entity : player.getNearbyEntities(3.4, 2.2, 3.4)) {
            if (entity instanceof LivingEntity target && entity != player) {
                deal(player, target, kit == Kit.STONE ? 9 : 6);
                target.setVelocity(player.getLocation().getDirection().multiply(.8).setY(.35));
            }
        }
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 1, .5, 1, .15);
    }

    private void dash(Player player) {
        if (!ready(player, "dash", 1800)) return;
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.7).setY(.2));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, .25, .2, .25, .05);
    }

    private void ultimateAttack(Player player) {
        UUID uuid = player.getUniqueId();
        if (ultimate.getOrDefault(uuid, 0) < 100) {
            player.sendActionBar(Component.text("Ultimate charge: " + ultimate.getOrDefault(uuid, 0) + "%", NamedTextColor.GRAY));
            return;
        }
        ultimate.put(uuid, 0);
        for (Entity entity : player.getNearbyEntities(6, 3, 6)) {
            if (entity instanceof LivingEntity target && entity != player) {
                deal(player, target, 16);
                Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector());
                if (knockback.lengthSquared() > 0) target.setVelocity(knockback.normalize().multiply(1.4).setY(.7));
            }
        }
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 100, 3, 1, 3, .1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, .45f, 1.8f);
        player.sendMessage(Component.text("Ultimate released!", NamedTextColor.GOLD));
    }

    private void hitCone(Player player, double damage, double range, double width) {
        Vector forward = player.getLocation().getDirection().normalize();
        for (Entity entity : player.getNearbyEntities(range, 2.2, range)) {
            if (entity instanceof LivingEntity target && entity != player) {
                Vector offset = target.getLocation().add(0, 1, 0).toVector().subtract(player.getLocation().add(0, 1, 0).toVector());
                if (offset.lengthSquared() > 0 && offset.length() <= range && forward.dot(offset.normalize()) > width) deal(player, target, damage);
            }
        }
    }

    private void deal(Player attacker, LivingEntity target, double damage) {
        target.damage(damage, attacker);
        gainUltimate(attacker, 3);
    }

    private boolean ready(Player player, String name, long duration) {
        String key = player.getUniqueId() + ":" + name;
        long now = System.currentTimeMillis();
        long available = cooldowns.getOrDefault(key, 0L);
        if (available > now) {
            player.sendActionBar(Component.text(name + " ready in " + ((available - now) / 1000 + 1) + "s", NamedTextColor.RED));
            return false;
        }
        cooldowns.put(key, now + duration);
        return true;
    }

    private void gainUltimate(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        ultimate.put(uuid, Math.min(100, ultimate.getOrDefault(uuid, 0) + amount));
    }

    private void showStatus(Player player) {
        player.sendActionBar(Component.text("Kit: " + kits.getOrDefault(player.getUniqueId(), Kit.EMBER).displayName + " | Ultimate: " + ultimate.getOrDefault(player.getUniqueId(), 0) + "%", NamedTextColor.AQUA));
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        blocking.removeIf(uuid -> blockUntil.getOrDefault(uuid, 0L) < now);
        for (UUID uuid : fighters) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) showStatus(player);
        }
    }

    private enum Kit {
        EMBER("Ember", NamedTextColor.RED, Particle.FLAME), TIDE("Tide", NamedTextColor.BLUE, Particle.SPLASH), STONE("Stone", NamedTextColor.GRAY, Particle.BLOCK);
        private final String displayName;
        private final NamedTextColor color;
        private final Particle particle;
        Kit(String displayName, NamedTextColor color, Particle particle) { this.displayName = displayName; this.color = color; this.particle = particle; }
        private static Kit from(String value) { for (Kit kit : values()) if (kit.displayName.equalsIgnoreCase(value)) return kit; return null; }
    }
}
