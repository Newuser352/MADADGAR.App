package com.example.madadgarapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for managing a list of favourited item IDs using {@link SharedPreferences}.
 *
 * Keep it simple for demo purposes – favourited items are stored locally on the current device.
 * Later, this can be swapped out for a remote solution (e.g. Supabase profile column) if the
 * project needs multi-device synchronisation.
 */
public final class FavoriteManager {

    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorite_item_ids";

    private FavoriteManager() {
        // no-op
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * @return {@code true} if the given item is marked as favourite on this device.
     */
    public static boolean isFavorite(Context ctx, String itemId) {
        if (itemId == null) return false;
        return prefs(ctx).getStringSet(KEY_FAVORITES, new HashSet<>()).contains(itemId);
    }

    /**
     * Mark or un-mark an item as favourite.
     *
     * @param favourite {@code true} to mark as favourite, {@code false} to remove.
     */
    public static void setFavorite(Context ctx, String itemId, boolean favourite) {
        if (itemId == null) return;
        SharedPreferences sp = prefs(ctx);
        Set<String> favs = new HashSet<>(sp.getStringSet(KEY_FAVORITES, new HashSet<>()));
        if (favourite) {
            favs.add(itemId);
        } else {
            favs.remove(itemId);
        }
        sp.edit().putStringSet(KEY_FAVORITES, favs).apply();
    }

    /**
     * Convenience toggler.
     *
     * @return the new favourite state after toggling (i.e. {@code true} if now favourited).
     */
    public static boolean toggleFavorite(Context ctx, String itemId) {
        boolean newState = !isFavorite(ctx, itemId);
        setFavorite(ctx, itemId, newState);
        return newState;
    }

    /**
     * Retrieve the set of favourited IDs.
     */
    public static Set<String> getFavorites(Context ctx) {
        return new HashSet<>(prefs(ctx).getStringSet(KEY_FAVORITES, new HashSet<>()));
    }
}
