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

        public void storePage(Player player, Object... o) {
                getStore(player).add(o);
        }

        public void storePage(Player player, List<Object> lines) {
                storePage(player, lines.toArray(new Object[0]));
        }

        public Object[] getPage(Player player) {
                Store store = players.get(player);
                if (store == null) return null;
                return store.get();
        }

        public void clearPages(Player player) {
                players.remove(player);
        }
}

class Store {
        private LinkedList<Object[]> pages = new LinkedList<>();

        public Store() {}

        public void add(Object[] page) {
                pages.addLast(page);
        }

        public Object[] get() {
                if (pages.isEmpty()) return null;
                return pages.removeFirst();
        }
}
