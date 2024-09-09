package qouteall.dimlib;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.dimlib.mixin.client.IClientPacketListener;

public class DimLibNetworking {
    public static final Logger LOGGER = LoggerFactory.getLogger(DimLibNetworking.class);
    
    public static final ResourceLocation DIM_SYNC_ID = new ResourceLocation("dimlib:dim_sync");

    public static class DimSyncPacket {
        private final CompoundTag dimIdToTypeIdTag;

        public DimSyncPacket(CompoundTag dimIdToTypeIdTag) {
            this.dimIdToTypeIdTag = dimIdToTypeIdTag;
        }

        public static DimSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag compoundTag = buf.readNbt();
            return new DimSyncPacket(compoundTag);
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(dimIdToTypeIdTag);
        }
        
        public static DimSyncPacket createPacket(MinecraftServer server) {
            RegistryAccess registryManager = server.registryAccess();
            Registry<DimensionType> dimensionTypes = registryManager.registryOrThrow(Registries.DIMENSION_TYPE);

            CompoundTag dimIdToDimTypeId = new CompoundTag();
            for (ServerLevel world : server.getAllLevels()) {
                ResourceKey<Level> dimId = world.dimension();
                
                DimensionType dimType = world.dimensionType();
                ResourceLocation dimTypeId = dimensionTypes.getKey(dimType);
                
                if (dimTypeId == null) {
                    LOGGER.error("Cannot find dimension type for {}", dimId.location());
                    LOGGER.error(
                        "Registered dimension types {}", dimensionTypes.keySet()
                    );
                    dimTypeId = BuiltinDimensionTypes.OVERWORLD.location();
                }
                
                dimIdToDimTypeId.putString(
                    dimId.location().toString(),
                    dimTypeId.toString()
                );
            }
            
            return new DimSyncPacket(dimIdToDimTypeId);
        }
        
        public ImmutableMap<ResourceKey<Level>, ResourceKey<DimensionType>> toMap() {
            CompoundTag tag = dimIdToTypeIdTag;

            ImmutableMap.Builder<ResourceKey<Level>, ResourceKey<DimensionType>> builder =
                new ImmutableMap.Builder<>();
            
            for (String key : tag.getAllKeys()) {
                ResourceKey<Level> dimId = ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(key)
                );
                String dimTypeId = tag.getString(key);
                ResourceKey<DimensionType> dimType = ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    new ResourceLocation(dimTypeId)
                );
                builder.put(dimId, dimType);
            }
            
            return builder.build();
        }
        
        @Environment(EnvType.CLIENT)
        public void handle(Minecraft client) {
            Validate.isTrue(
                client.isSameThread(),
                "Not running in client thread"
            );
            
            LOGGER.info(
                "Client received dimension info\n{}",
                String.join("\n", dimIdToTypeIdTag.getAllKeys())
            );
            
            var dimIdToDimType = this.toMap();
            ClientDimensionInfo.accept(dimIdToDimType);
            ((IClientPacketListener) client.getConnection()).ip_setLevels(dimIdToDimType.keySet());

            DimensionAPI.CLIENT_DIMENSION_UPDATE_EVENT.invoker().run(
                ClientDimensionInfo.getDimensionIds()
            );
        }
    }
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(DIM_SYNC_ID, (server, player, handler, buf, responseSender) -> {
            DimSyncPacket packet = DimSyncPacket.read(buf);
            server.execute(() -> {
                // Handle packet on server side if needed
            });
        });
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(DIM_SYNC_ID, (client, handler, buf, responseSender) -> {
            DimSyncPacket packet = DimSyncPacket.read(buf);
            client.execute(() -> packet.handle(client));
        });
    }

    public static void sendDimSyncPacket(MinecraftServer server) {
        DimSyncPacket packet = DimSyncPacket.createPacket(server);
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        server.getPlayerList().getPlayers().forEach(player ->
            ServerPlayNetworking.send(player, DIM_SYNC_ID, buf)
        );
    }
}
