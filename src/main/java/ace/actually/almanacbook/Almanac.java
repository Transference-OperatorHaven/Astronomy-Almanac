package ace.actually.almanacbook;

import ace.actually.almanacbook.items.AlmanacItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Almanac implements ModInitializer {

    public static final Logger LOGGER;
    public static final Identifier ASTRA_PACKET;
    public static final AlmanacItem ALMANAC_ITEM;
    public static final Identifier ASTRA_UPDATE_CLIENT_PACKET;
    public static RecipeSerializer<AlmanacCloningRecipe> ALMANAC_CLONING;

	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registries.ITEM,new Identifier("almanacbook","almanac"),ALMANAC_ITEM);
		LOGGER.info("It's time to map some stars");

		//fun fact: to actually make cloning work you also need to create a (recipe).json. see data/almanac/recipes/almanac_cloning.json
		Registry.register(Registries.RECIPE_SERIALIZER,new Identifier("almanacbook","almanac_cloning"),ALMANAC_CLONING);

		ServerPlayNetworking.registerGlobalReceiver(ASTRA_PACKET,((server, player, handler, buf, responseSender) ->
		{
			final NbtCompound newAstra = buf.readNbt();
			server.execute(()->
			{
				NbtCompound compound;
				if(player.getMainHandStack().hasNbt())
				{
					compound = player.getMainHandStack().getNbt();
					compound.put("astra",newAstra.get("astra"));
				}
				else
				{
					compound = newAstra;
				}

				if(compound.contains("version"))
				{
					compound.putInt("version",compound.getInt("version")+1);
				}
				else
				{
					compound.putInt("version",1);
				}

				if(compound.contains("authors"))
				{
					final NbtList authors = (NbtList) compound.get("authors");
					boolean canAdd = true;
                    int i = 0;
                    while (i < authors.size()) {
                        if (authors.getString(i).equals(player.getName().getString())) {
                            canAdd = false;
                            break;
                        }
                        else {
                            ++i;
                        }
                    }
					if(canAdd)
					{
						authors.add(NbtString.of(player.getName().getString()));
						compound.put("authors",(NbtElement)authors);
					}

				}
				else
				{
					NbtList authors2 = new NbtList();
					authors2.add(NbtString.of(player.getName().getString()));
					compound.put("authors",(NbtElement)authors2);
				}

                player.getMainHandStack().setNbt(compound);
				player.sendMessage(Text.translatable("almanac.wrote"));
			});
		}));


	}
    static{
        LOGGER = LoggerFactory.getLogger("almanacbook");
        ASTRA_PACKET = new  Identifier("almanacbook","astra_packet");
        ALMANAC_ITEM = new AlmanacItem(new Item.Settings());
        ASTRA_UPDATE_CLIENT_PACKET = new Identifier("almanacbook","astra_update_client_packet");
        Almanac.ALMANAC_CLONING = (RecipeSerializer<AlmanacCloningRecipe>)new SpecialRecipeSerializer<>(AlmanacCloningRecipe::new);
    }
}