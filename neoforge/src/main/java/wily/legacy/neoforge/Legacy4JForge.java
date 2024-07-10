package wily.legacy.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.event.EventNetworkChannel;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@Mod(Legacy4J.MOD_ID)
@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {
    public static IEventBus MOD_EVENT_BUS;
    public Legacy4JForge(IEventBus bus) {
        MOD_EVENT_BUS = bus;
        Legacy4J.init();
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister() {
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, ResourceLocation type, Function<FriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> apply) {
                EventNetworkChannel NETWORK = NetworkRegistry.ChannelBuilder.named(type).networkProtocolVersion(()->"1").serverAcceptedVersions(s-> !s.equals(NetworkRegistry.ABSENT.version()) && Integer.parseInt(s) >= 1).clientAcceptedVersions(s->!s.equals(NetworkRegistry.ABSENT.version()) && Integer.parseInt(s) >= 1).eventNetworkChannel();
                if (client || FMLEnvironment.dist.isClient()) NETWORK.addListener(p->{
                    if (p.getPayload() != null) apply.apply(codec.apply(p.getPayload()), p.getSource().getDirection().getReceptionSide().isClient() ? Legacy4JClient.SECURE_EXECUTOR : Legacy4J.SECURE_EXECUTOR, () -> p.getSource().getDirection().getReceptionSide().isClient() ? Legacy4JForgeClient.getClientPlayer() : p.getSource().getSender());
                    p.getSource().setPacketHandled(true);
                });
            }
        });
        NeoForge.EVENT_BUS.addListener(TickEvent.ServerTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4J.preServerTick(e.getServer());
        });
        if (FMLEnvironment.dist == Dist.CLIENT) Legacy4JForgeClient.init();

    }
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        Legacy4J.setup();
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event)
    {
        Legacy4J.registerBuiltInPacks((path, name, displayName, position, defaultEnabled) -> {
            Path resourcePath = ModList.get().getModFileById(Legacy4J.MOD_ID).getFile().findResource(path);
            for (PackType type : PackType.values()){
                if (event.getPackType() != type || !Files.isDirectory(resourcePath.resolve(type.getDirectory()))) continue;
                Pack pack = Pack.readMetaAndCreate(Legacy4J.MOD_ID + ":" + name, displayName,false, new PathPackResources.PathResourcesSupplier(resourcePath,true), type, position, PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}
