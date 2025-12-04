package ace.actually.almanacbook;

import com.nettakrim.spyglass_astronomy.*;
import com.nettakrim.spyglass_astronomy.commands.admin_subcommands.ConstellationsCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Base64;

@Environment(EnvType.CLIENT)
public class AlmanacClient implements ClientModInitializer {
    public static final Identifier ASTRA_UPDATE_CLIENT_PACKET = new Identifier("almanacbook","astra_update_client_packet");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ASTRA_UPDATE_CLIENT_PACKET,((client, handler, buf, responseSender) ->
        {
            final NbtCompound compound = buf.readNbt();
            client.execute(()->
            {
                final NbtList astra = (NbtList) compound.get("astra");
                for (int i = 0; i < astra.size(); i++) {
                    if(astra.getString(i).contains("constellations"))
                    {
                        //"/sga:admin constellations add constellationData constellationName,
                        final String[] v = astra.getString(i).split(" ");
                        final Constellation constellation = SpaceDataManager.decodeConstellation((Base64.Decoder)null, v[4], v[3]);
                        ConstellationsCommand.addConstellation(constellation,true,true);
                    }
                    if(astra.getString(i).contains("star"))
                    {

                        //"/sga:admin rename star starIndex starName;
                        final String[] v = astra.getString(i).split(" ");
                        final Star star = SpyglassAstronomyClient.stars.get(Integer.parseInt(v[3]));
                        final String name = v[4];
                        if (star.isUnnamed()) {
                            SpyglassAstronomyClient.say("commands.name.star", new Object[] { name });
                        } else {
                            SpyglassAstronomyClient.say("commands.name.star.rename", new Object[] { star.name,name });
                        }

                        star.name=name;
                        star.select();
                        SpaceDataManager.makeChange();
                    }
                    if(astra.getString(i).contains("planet"))
                    {
                        //"/sga:admin rename planet  orbitingBodyIndex orbitingBodyName;
                        final String[] v = astra.getString(i).split(" ");
                        final OrbitingBody orbitingBody = SpyglassAstronomyClient.orbitingBodies.get(Integer.parseInt(v[3]));
                        final String name2 = v[4];
                        if (orbitingBody.isUnnamed()) {
                            SpyglassAstronomyClient.say("commands.name." + (orbitingBody.isPlanet ? "planet" : "comet"), new Object[] { name2 });
                        } else {
                            SpyglassAstronomyClient.say("commands.name." + (orbitingBody.isPlanet ? "planet" : "comet") + ".rename", new Object[] { orbitingBody.name, name2 });
                        }
                        orbitingBody.name = name2;
                        orbitingBody.select();
                        SpaceDataManager.makeChange();
                    }

                }
                client.player.sendMessage(Text.translatable("almanac.learnt"));
            });
        }));
    }
}
