package de.smpcore.gui;

import de.smpcore.economy.EconomyManager;
import de.smpcore.shop.ShopManager;
import de.smpcore.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shop im Look des HugoSMP-Shops: 3 Reihen, Kategorien mittig, kein Füll-Glas. */
public final class ShopMenus {
    private ShopMenus() {}

    public static void openMain(Player p) { new Main(p).open(); }

    public static final class Main extends Menu {
        public Main(Player p) { super(p, 27, "Shop"); }

        @Override
        protected void build() {
            int next = 11;
            Set<Integer> used = new HashSet<>();
            for (ShopManager.Category c : plugin.shop().categories()) {
                int slot = c.slot();
                if (slot < 0 || slot >= 27 || used.contains(slot)) {
                    while (used.contains(next)) next++;
                    slot = next;
                }
                used.add(slot);
                if (c.locked()) {
                    set(slot, Items.make(c.icon(), c.name()));
                } else {
                    set(slot, Items.make(c.icon(), c.name(), "<gray>" + c.items().size() + " Items", "", "<yellow>Klicken zum Öffnen"),
                            e -> go(new Cat(viewer, c)));
                }
            }
        }
    }

    public static final class Cat extends PagedMenu {
        private final ShopManager.Category cat;

        public Cat(Player p, ShopManager.Category cat) {
            super(p, "Shop");
            this.cat = cat;
        }

        @Override protected boolean filler() { return false; }

        @Override
        protected List<Entry> entries() {
            List<Entry> out = new ArrayList<>();
            for (ShopManager.Item it : cat.items()) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                if (it.buy() > 0) lore.add("<gray>Kaufen: <green>" + EconomyManager.fmt(it.buy()));
                if (it.sell() > 0) lore.add("<gray>Verkaufen: <gold>" + EconomyManager.fmt(it.sell()));
                lore.add("");
                lore.add("<yellow>Klicken zum Auswählen");
                out.add(new Entry(Items.withLore(new ItemStack(it.material()), lore), e -> go(new Detail(viewer, cat, it))));
            }
            return out;
        }

        @Override
        protected void controls() {
            set(49, Items.make(Material.ARROW, "<red>Zurück"), e -> go(new Main(viewer)));
            set(47, Items.make(Material.GOLD_INGOT, "<gold>Dein Geld", "<white>" + EconomyManager.fmt(plugin.economy().balance(viewer.getUniqueId()))));
        }
    }

    public static final class Detail extends Menu {
        private final ShopManager.Category cat;
        private final ShopManager.Item item;

        public Detail(Player p, ShopManager.Category cat, ShopManager.Item item) {
            super(p, 27, "Shop");
            this.cat = cat;
            this.item = item;
        }

        @Override
        protected void build() {
            Material m = item.material();
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (item.buy() > 0) lore.add("<gray>Kaufpreis: <green>" + EconomyManager.fmt(item.buy()) + " <dark_gray>/ Stück");
            if (item.sell() > 0) lore.add("<gray>Verkaufspreis: <gold>" + EconomyManager.fmt(item.sell()) + " <dark_gray>/ Stück");
            lore.add("");
            lore.add("<gray>Dein Geld: <white>" + EconomyManager.fmt(plugin.economy().balance(viewer.getUniqueId())));
            set(13, Items.withLore(new ItemStack(m), lore));

            int[] amounts = {1, 16, 64};
            for (int i = 0; i < 3; i++) {
                final int amt = amounts[i];
                if (item.buy() > 0) {
                    set(10 + i, Items.make(Material.LIME_STAINED_GLASS_PANE, amt, "<green><bold>Kaufen <white>" + amt + "x",
                                    List.of("<gray>Preis: <green>" + EconomyManager.fmt(item.buy() * amt))),
                            e -> { plugin.shop().buy(viewer, item, amt); refresh(); });
                }
                if (item.sell() > 0) {
                    set(14 + i, Items.make(Material.RED_STAINED_GLASS_PANE, amt, "<red><bold>Verkaufen <white>" + amt + "x",
                                    List.of("<gray>Erlös: <gold>" + EconomyManager.fmt(item.sell() * amt))),
                            e -> { plugin.shop().sellItem(viewer, m, item.sell(), amt); refresh(); });
                }
            }
            if (item.sell() > 0) {
                set(22, Items.make(Material.GOLD_BLOCK, "<gold><bold>Alles verkaufen", "<gray>Verkauft alle " + Items.pretty(m) + " aus deinem Inventar"),
                        e -> { plugin.shop().sellItem(viewer, m, item.sell(), Integer.MAX_VALUE); refresh(); });
            }
            set(18, Items.make(Material.ARROW, "<red>Zurück"), e -> go(new Cat(viewer, cat)));
        }
    }
}
