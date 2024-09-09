package qouteall.dimlib.mixin.common;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.dimlib.ducks.IMappedRegistry;

import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MixinMappedRegistry<T> implements IMappedRegistry {
    @Shadow
    @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract @Nullable T byId(int id);

    @Shadow
    @Final
    private ObjectList<Holder.Reference<T>> byId;

    @Shadow
    @Final
    private Object2IntMap<T> toId;

    @Shadow
    @Final
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;

    @Shadow
    @Final
    ResourceKey<? extends Registry<T>> key;

    @Shadow
    @Final
    private Map<T, Holder.Reference<T>> byValue;

    @Shadow
    private boolean frozen;

    @Shadow
    @Final
    private Map<T, Lifecycle> lifecycles;

    @Override
    public boolean dimlib_getIsFrozen() {
        return frozen;
    }

    @Override
    public void dimlib_setIsFrozen(boolean cond) {
        frozen = cond;
    }

    /**
     * See {@link MappedRegistry#register(ResourceKey, Object, Lifecycle)}
     */
    @Override
    public boolean dimlib_forceRemove(ResourceLocation id) {
        LOGGER.debug("[DimLib] Trying to remove {} from {}", id, this.key);

        Holder.Reference<T> holder = byLocation.remove(id);

        if (holder == null) {
            LOGGER.debug("[DimLib] {} not found in {} when trying to remove", id, this.key);
            return false;
        }

        ResourceKey<T> eleKey = holder.key();
        T value = holder.value();

        int intId = toId.getInt(value);

        if (intId == -1) {
            LOGGER.error("[DimLib] missing integer id for {} {}", value, id);
        } else {
            toId.removeInt(value);
            byId.set(intId, null);
        }

        byKey.remove(eleKey);
        byValue.remove(value);
        lifecycles.remove(value);

        return true;
    }

}
