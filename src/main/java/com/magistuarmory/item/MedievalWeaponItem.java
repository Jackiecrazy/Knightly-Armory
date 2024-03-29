package com.magistuarmory.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.magistuarmory.effects.LacerationEffect;

import java.time.Clock;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class MedievalWeaponItem extends SwordItem implements IHasModelProperty
{

    protected float attackDamage;
    protected float attackSpeed;
    protected float currentAttackDamage;
    private float currentAttackSpeed;
    private float decreasedAttackDamage;
    private float decreasedAttackSpeed;
    private float maxBlockDamage = 0.0f;
    private float weight = 1.0f;

    private boolean canBlock = false;
    private boolean isSilver = false;
    private boolean isFlamebladed = false;

    public int armorPiercing = 0;

    private float reachDistance = 0.0F;
    private int twoHanded;
    private boolean blockingPriority;

    public MedievalWeaponItem(String unlocName, Item.Properties build, IItemTier material, float attackDamage, float materialFactor, float attackSpeed, int armorPiercing, float reachDistance)
    {
        this(unlocName, build, material, attackDamage, materialFactor, attackSpeed);
        this.armorPiercing = armorPiercing;
        this.reachDistance = reachDistance;
    }

    public MedievalWeaponItem(String unlocName, Item.Properties build, IItemTier material, float attackDamageIn, float materialFactor, float attackSpeedIn)
    {
        super(material, 0, 0.0F, build);
        setRegistryName(unlocName);
        this.attackDamage = attackDamageIn + materialFactor * material.getAttackDamageBonus();
        this.attackSpeed = attackSpeedIn;
        this.currentAttackDamage = this.attackDamage;
        this.currentAttackSpeed = this.attackSpeed;
        if (getTier().equals(ModItemTier.SILVER))
        {
            isSilver = true;
        }
    }

    public MedievalWeaponItem setFlamebladed()
    {
        isFlamebladed = true;
        return this;
    }

    public MedievalWeaponItem setTwoHanded(int level)
    {
        twoHanded = level;
        decreasedAttackDamage = 12.0f * attackDamage / (5.0f * level + 10.0f);
        decreasedAttackSpeed = 14.0f * (attackSpeed + 4.0f) / (15.0f * level + 5.0f) - 4.0f;
        return this;
    }

    public MedievalWeaponItem setBlocking(float weight, float maxBlockDamage)
    {
        this.weight = weight;
        this.maxBlockDamage = maxBlockDamage;
        this.canBlock = true;
        return this;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack)
    {
        if (slot == EquipmentSlotType.MAINHAND)
        {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", this.getAttackDamage(), AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", this.getAttackSpeed(), AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void inventoryTick(ItemStack par1ItemStack, World world, Entity entityIn, int par4, boolean par5)
    {
        boolean flag = false;
        if (twoHanded > 0 && entityIn instanceof LivingEntity && !((LivingEntity)entityIn).getOffhandItem().getItem().equals(Items.AIR))
        {
            if (currentAttackDamage != decreasedAttackDamage)
            {
                currentAttackDamage = decreasedAttackDamage;
                flag = true;
            }

            if (currentAttackDamage != decreasedAttackSpeed)
            {
                currentAttackSpeed = decreasedAttackSpeed;
                flag = true;
            }
        }
        else
        {
            if (currentAttackDamage != attackDamage)
            {
                currentAttackDamage = attackDamage;
                flag = true;
            }

            if (currentAttackDamage != attackSpeed)
            {
                currentAttackSpeed = attackSpeed;
                flag = true;
            }
        }

        if (flag)
        {
            if (entityIn instanceof LivingEntity)
            {
                ItemStack itemstack = ((LivingEntity) entityIn).getMainHandItem();

                for (EquipmentSlotType entityequipmentslot : EquipmentSlotType.values())
                {
                    ((LivingEntity) entityIn).getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(entityequipmentslot));
                    ((LivingEntity) entityIn).getAttributes().addTransientAttributeModifiers(itemstack.getAttributeModifiers(entityequipmentslot));
                }
            }
        }

        if (canBlock() && entityIn instanceof LivingEntity)
        {
            blockingPriority = !(((LivingEntity) entityIn).getMainHandItem().getItem() instanceof ShieldItem) && !(((LivingEntity) entityIn).getOffhandItem().getItem() instanceof ShieldItem);
        }

        super.inventoryTick(par1ItemStack, world, entityIn, par4, par5);
    }

    @Override
    public boolean hurtEnemy(ItemStack p_77644_1_, LivingEntity p_77644_2_, LivingEntity p_77644_3_)
    {
        if (this.isSilver && p_77644_2_.getMobType().equals(CreatureAttribute.UNDEAD))
            p_77644_2_.hurt(DamageSource.MAGIC, this.getAttackDamage() + 3.0F);
        if (this.isFlamebladed)
        {
            float damage = CombatRules.getDamageAfterAbsorb(this.attackDamage, (float)p_77644_2_.getArmorValue(), (float)p_77644_2_.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            p_77644_2_.addEffect(new EffectInstance(LacerationEffect.LACERATION.setDamageValue(damage), 300, 1, true, true, true));
        }
        return super.hurtEnemy(p_77644_1_, p_77644_2_, p_77644_3_);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        if (this.isSilver)
            tooltip.add((new TranslationTextComponent("silvertools.hurt")).withStyle(TextFormatting.GREEN));
        if (this.isFlamebladed)
            tooltip.add((new TranslationTextComponent("flamebladed.hurt")).withStyle(TextFormatting.BLUE));
        if (this.armorPiercing != 0)
            tooltip.add(new StringTextComponent(this.armorPiercing + "% ").append(new TranslationTextComponent("armorpiercing")).withStyle(TextFormatting.BLUE));
        if (this.reachDistance != 0.0F)
            tooltip.add(new StringTextComponent("+" + this.reachDistance + " ").append(new TranslationTextComponent("reachdistance")).withStyle(TextFormatting.BLUE));
        if (twoHanded == 1)
            tooltip.add((new TranslationTextComponent("twohandedi")).withStyle(TextFormatting.BLUE));
        else if (twoHanded > 1)
            tooltip.add(new TranslationTextComponent("twohandedii").withStyle(TextFormatting.BLUE));
        if (canBlock())
            tooltip.add(new StringTextComponent(getMaxBlockDamage() + " ").append(new TranslationTextComponent("maxdamageblock")).withStyle(TextFormatting.BLUE));
            tooltip.add(new StringTextComponent(getWeight() + "").append(new TranslationTextComponent("kgweight")).withStyle(TextFormatting.BLUE));
    }

    public float getAttackDamage()
    {
        return currentAttackDamage;
    }

    public float getAttackSpeed()
    {
        return currentAttackSpeed;
    }

    public float getReachDistance()
    {
        return reachDistance + 5.0f;
    }

    @Override
    public ActionResult<ItemStack> use(World p_77659_1_, PlayerEntity p_77659_2_, Hand p_77659_3_)
    {
        if (canBlock() && blockingPriority)
        {
            ItemStack itemstack = p_77659_2_.getItemInHand(p_77659_3_);
            p_77659_2_.startUsingItem(p_77659_3_);

            return ActionResult.consume(itemstack);
        }

        return super.use(p_77659_1_, p_77659_2_, p_77659_3_);
    }

    public int getUseDuration(ItemStack p_77626_1_)
    {
        return canBlock() ? (int) (500 / getWeight()) : 0;
    }

    @Override
    public UseAction getUseAnimation(ItemStack p_77661_1_)
    {
        return (canBlock() && blockingPriority) ? UseAction.BLOCK : super.getUseAnimation(p_77661_1_);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void registerModelProperty()
    {
        if (canBlock())
        {
            ItemModelsProperties.register(this, new ResourceLocation("blocking"), (p_239421_0_, p_239421_1_, p_239421_2_) ->
            {
                return p_239421_2_ != null && p_239421_2_.isUsingItem() && p_239421_2_.getUseItem() == p_239421_0_ ? 1.0F : 0.0F;
            });
        }
    }

    public void onBlocked(ItemStack stack, float damage, PlayerEntity player, DamageSource source)
    {
        if (canBlock())
        {
            float armorPiercingFactor = 1.0f;
            if (source.getEntity() instanceof LivingEntity)
            {
                LivingEntity attacker = (LivingEntity) source.getEntity();
                if (attacker.getMainHandItem().getItem() instanceof MedievalWeaponItem)
                {
                    armorPiercingFactor += ((MedievalWeaponItem) attacker.getMainHandItem().getItem()).armorPiercing / 100.0f;
                }
            }

            if (source.isExplosion())
            {
                player.hurt(DamageSource.GENERIC, damage);

                return;
            }
            else if (!haveBlocked(new Random(), source))
            {
                float damage2 = CombatRules.getDamageAfterAbsorb(damage, (float)player.getArmorValue(), (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
                player.hurt(DamageSource.GENERIC, damage2);
            }
            else if (damage > getMaxBlockDamage())
            {
                stack.hurtAndBreak((int) (armorPiercingFactor * 0.2f * stack.getMaxDamage()), player, (p_220044_0_) ->
                {
                    p_220044_0_.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
                });
                float damage1 = damage - getMaxBlockDamage();
                float damage2 = CombatRules.getDamageAfterAbsorb(damage1, (float)player.getArmorValue(), (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
                player.hurt(DamageSource.GENERIC, damage2);

                return;
            }

            stack.hurtAndBreak((int) (armorPiercingFactor * damage), player, (p_220044_0_) ->
            {
                p_220044_0_.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
            });
        }
    }

    public float getMaxBlockDamage()
    {
        return maxBlockDamage;
    }

    public float getWeight()
    {
        return weight;
    }

    public boolean canBlock()
    {
        return canBlock;
    }

    @Override
    public boolean isShield(ItemStack stack, LivingEntity entity)
    {
        return canBlock();
    }

    boolean haveBlocked(Random rand, DamageSource source)
	{
		return !source.isProjectile() && rand.nextInt(14) > getWeight();
	}
}
