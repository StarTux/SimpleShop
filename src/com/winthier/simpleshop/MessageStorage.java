package com.winthier.simpleshop;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.entity.Player;

public class MessageStorage {
        private final Map<Player, Store> players = new WeakHashMap<Player, Store>();

        private Store getStore(Player player) {
                Store result = players.get(player);
                if (result == null) {
                        result = new Store();
                        players.put(player, result);
                }
                return result;
        }

        public void storePage(Player player, String... lines) {
                getStore(player).add(lines);
        }

        public void storePage(Player player, List<String> lines) {
                storePage(player, lines.toArray(new String[0]));
        }

        public String[] getPage(Player player, String... lines) {
                Store store = players.get(player);
                if (store == null) return null;
                return store.get();
        }

        public void clearPages(Player player) {
                players.remove(player);
        }
}

class Store {
        private LinkedList<String[]> pages = new LinkedList<String[]>();

        public Store() {}

        public void add(String[] page) {
                pages.addLast(page);
        }

        public String[] get() {
                if (pages.isEmpty()) return null;
                return pages.removeFirst();
        }
}
