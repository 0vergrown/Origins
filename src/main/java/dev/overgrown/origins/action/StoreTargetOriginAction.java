package dev.overgrown.origins.action;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.world.entity.player.Player;

public final class StoreTargetOriginAction implements ActionType<BiEntityCtx, StoreOriginAction.Cfg> {
    @Override
    public MapCodec<StoreOriginAction.Cfg> codec() {
        return StoreOriginAction.CONFIG_CODEC;
    }

    @Override
    public void run(StoreOriginAction.Cfg cfg, BiEntityCtx ctx) {
        if (ctx.level().isClientSide()) return;
        if (!(ctx.actor() instanceof Player holder) || !(ctx.target() instanceof Player source)) return;
        StoreOriginAction.apply(cfg, holder, source);
    }
}
