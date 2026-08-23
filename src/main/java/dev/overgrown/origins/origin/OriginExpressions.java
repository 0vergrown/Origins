package dev.overgrown.origins.origin;

import dev.overgrown.apoli.data.expr.ExprParser;
import dev.overgrown.apoli.data.expr.ExprPeer;
import dev.overgrown.apoli.data.expr.ExprVars;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class OriginExpressions {

    private OriginExpressions() {}

    public static void register() {
        ExprVars.register("origin_count", (e, c, l, v) -> {
            Player player = OriginView.playerOf(e);
            return player == null ? 0 : OriginView.layerCount(player);
        });
        ExprVars.register("is_swapped", (e, c, l, v) -> {
            Player player = OriginView.playerOf(e);
            return player != null && !OriginView.swaps(player).isEmpty() ? 1 : 0;
        });

        ExprParser.registerIdFunction("has_origin", (id, args, name, at) ->
            new ExprVars.ResolvedVar((e, c, l, v) -> holds(e, id), false));
        ExprParser.registerIdFunction("target_has_origin", (id, args, name, at) ->
            new ExprVars.ResolvedVar((e, c, l, v) -> {
                Entity peer = ExprPeer.target();
                return holds(peer != null ? peer : e, id);
            }, false, true));
        ExprParser.registerIdFunction("in_origin_pool", (id, args, name, at) ->
            new ExprVars.ResolvedVar((e, c, l, v) -> {
                Player player = OriginView.playerOf(e);
                return player != null && OriginView.inPool(player, id) ? 1 : 0;
            }, false));
        ExprParser.registerIdFunction("has_origin_on", (id, args, name, at) ->
            new ExprVars.ResolvedVar((e, c, l, v) -> {
                Player player = OriginView.playerOf(e);
                return player != null && OriginView.activeOn(player, id) != null ? 1 : 0;
            }, false));
        ExprParser.registerIdFunction("origin_impact", (id, args, name, at) ->
            new ExprVars.ResolvedVar((e, c, l, v) -> {
                Player player = OriginView.playerOf(e);
                return player == null ? 0 : OriginView.impactOn(player, id);
            }, false));
    }

    private static double holds(@Nullable Entity entity, net.minecraft.resources.ResourceLocation originId) {
        Player player = OriginView.playerOf(entity);
        return player != null && OriginView.holds(player, originId) ? 1 : 0;
    }
}
