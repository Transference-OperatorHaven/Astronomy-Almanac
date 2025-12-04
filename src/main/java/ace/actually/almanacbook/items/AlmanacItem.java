package ace.actually.almanacbook.items;

import ace.actually.almanacbook.Almanac;
import ace.actually.almanacbook.AlmanacClient;
import com.nettakrim.spyglass_astronomy.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.List;

public class AlmanacItem extends Item {
    public AlmanacItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if(!world.isClient && !user.isSneaking())
        {
            NbtCompound compound = user.getStackInHand(hand).getNbt();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(compound);
            ServerPlayNetworking.send((ServerPlayerEntity) user, AlmanacClient.ASTRA_UPDATE_CLIENT_PACKET,buf);
        }
        return super.use(world, user, hand);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient && context.getPlayer().isSneaking() && context.getWorld().getBlockState(context.getBlockPos()).isOf(Blocks.CARTOGRAPHY_TABLE))
        {
            NbtCompound compound = new NbtCompound();
            NbtList astra = new NbtList();

            for(final Star star: SpyglassAstronomyClient.stars)
            {
                if(!star.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(star))));
                }
            }

            for(final OrbitingBody body: SpyglassAstronomyClient.orbitingBodies)
            {
                if(!body.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(body))));
                }
            }

            for(final Constellation constellation: SpyglassAstronomyClient.constellations)
            {
                if(!constellation.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(constellation))));
                }
            }
            compound.put("astra",astra);
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(compound);
            ClientPlayNetworking.send(Almanac.ASTRA_PACKET,buf);

        }
        return super.useOnBlock(context);
    }

    private static String share(final Constellation constellation) {
        return "sga:c_"+(SpaceDataManager.encodeConstellation((Base64.Encoder)null, constellation).replace(" | ", "|"))+"|";

    }

    private static String share(final Star star) {
        final String starName = (star.isUnnamed() ? "Unnamed" : star.name);
        return "sga:s_"+starName+"|"+ star.index +"|";

    }

    private static String share(final OrbitingBody orbitingBody) {
        final String orbitingBodyName = (orbitingBody.isUnnamed() ? "Unnamed" : orbitingBody.name);
        return "sga:p_"+orbitingBodyName+"|"+ SpyglassAstronomyClient.orbitingBodies.indexOf(orbitingBody) +"|";

    }

    private static String reprocessor(String message)
    {
        final int sgaIndex = message.indexOf("sga:");
        if (sgaIndex == -1) return null;

        String data = message.substring(sgaIndex+4);
        final int firstIndex = data.indexOf("|");
        if (firstIndex == -1) return null;
        final int secondIndex = data.indexOf("|", firstIndex+1);
        data = data.substring(0, secondIndex == -1 ? firstIndex : secondIndex);
        if (data.charAt(1) != '_') return null;



        switch (data.charAt(0)) {
            case 'c' -> {
                //constellation shared with sga:c_Name|AAAA|
                final String constellationName = data.substring(2, firstIndex);
                final String constellationData = data.substring(firstIndex + 1, secondIndex);
                return "/sga:admin constellations add " + constellationData + " " + constellationName;

            }
            case 's' -> {
                //star shared with sga:s_Name|index|
                final String starName = data.substring(2, firstIndex);
                final int starIndex;
                int index = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                try {
                    starIndex = index;
                } catch (final Exception e) {
                    break;
                }
                return "/sga:admin rename star " + starIndex + " " + starName;

            }
            case 'p' -> {
                //planets shared with sga:p_Name|index|
                final String orbitingBodyName = data.substring(2, firstIndex);
                final int orbitingBodyIndex;
                int index = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                try {
                    orbitingBodyIndex = index;
                } catch (final Exception e) {
                    break;
                }
                if (orbitingBodyIndex >= SpyglassAstronomyClient.orbitingBodies.size()) break;


                return "/sga:admin rename planet " + orbitingBodyIndex + " " + orbitingBodyName;

            }
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, final World world, final List<Text> tooltip, final TooltipContext context) {
        super.appendTooltip(stack, world, (List)tooltip, context);
        if(stack.hasNbt())
        {
            tooltip.add(Text.of("Version "+stack.getNbt().getInt("version")));
            final NbtList authors = (NbtList) stack.getNbt().get("authors");

            final StringBuilder v = new StringBuilder("Authors: ");
            for (int i = 0; i < authors.size(); i++) {
                v.append(authors.getString(i)).append(", ");
            }
            final String pcomma = v.toString();
            tooltip.add(Text.of(pcomma.substring(0,pcomma.length()-2)));

        }
    }
}
