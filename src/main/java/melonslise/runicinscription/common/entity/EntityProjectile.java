package melonslise.runicinscription.common.entity;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityProjectile extends Entity // TODO Rename to magic projectile (?)
{
	private static final DataParameter<Integer> OWNER = EntityDataManager.<Integer>createKey(EntityProjectile.class, DataSerializers.VARINT);
	/** UUID of the projectile's caster */
	private UUID ownerUUID;
	/** Speed is measured in blocks/tick or 20 blocks/second. */
	/** Used for removing the entity if it has been alive for too long. */
	private int ticksAlive; // TODO die after ticks

	public EntityProjectile(World worldIn) // TODO SET SIZE
	{
		super(worldIn); // TODO add player's motion 
		this.dataManager.register(OWNER, 0);
	}

	public EntityProjectile(World world, EntityLivingBase caster, double speed)
	{
		super(world);
		this.ownerUUID = caster.getUniqueID();
		this.dataManager.register(OWNER, caster.getEntityId());
		this.setPosition(caster.posX, caster.posY, caster.posZ);
		this.setSpeedFromEntity(caster, speed);
	}

	/**
	 * Not sure what this is used for :confused:
	 */
	@Override
	protected abstract void entityInit();

	/**
	 * Sets the projectile's position in the world. Does the same thing as {@link #setPosition(double, double, double)}, but does not update the bounding box.
	 */
	public void setLocation(double coordinateX, double coordinateY, double coordinateZ)
	{
		this.posX = coordinateX;
		this.posY = coordinateY;
		this.posZ = coordinateZ;
	}

	@Override
	public void onUpdate() // TODO COMBINE CODE WITH COLLISION CHECK (?)
	{
		super.onUpdate();

		// Effects
		if(this.shouldBurn())
		{
			this.setFire(1);
		}

		// Collision
		RayTraceResult result = this.checkCollision();
		if (result != null)
		{
			if (result.typeOfHit == RayTraceResult.Type.BLOCK && this.worldObj.getBlockState(result.getBlockPos()).getBlock() == Blocks.PORTAL)
			{
				this.setPortal(result.getBlockPos());
			}
			else
			{
				//if(!net.minecraftforge.common.ForgeHooks.onThrowableImpact(this, raytraceresult)) // TODO OWN HOOK
				this.onImpact(result);
			}
		}

		// Movement
		this.setLocation(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

		// Water collision
		float multiplier = 1;
		float subtrahend = 0;
		if (this.isInWater())
		{
			if(this.diesInWater())
			{
				this.setDead(); // Longer death + sound
			}
			else
			{
				for (int j = 0; j < 4; ++j)
				{
					this.worldObj.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ, new int[0]);
				}
				multiplier = 0.9F;
				subtrahend = 0.2F;
			}
		}

		this.motionX *= (double) multiplier; // TODO Apply actual physics formulae (probably mg-pgV)
		this.motionZ *= (double) multiplier;
		this.motionY -= (double) subtrahend;
		// TODO set up hasnogravity and gravity velocity
		this.setPosition(this.posX, this.posY, this.posZ); // Not sure why this is needed, but I suspect it has something to do with it's aabb

		// Particle trail
		this.spawnParticleTrail();
	}

	protected RayTraceResult checkCollision()
	{
		Vec3d vector1 = new Vec3d(this.posX, this.posY, this.posZ);
		Vec3d vector2 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
		RayTraceResult result1 = this.worldObj.rayTraceBlocks(vector1, vector2);
		if (result1 != null)
		{
			vector2 = new Vec3d(result1.hitVec.xCoord, result1.hitVec.yCoord, result1.hitVec.zCoord);
		}

		Entity entity1 = null;
		List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().addCoord(this.motionX, this.motionY, this.motionZ).expandXyz(1.0D)); // TODO FIGURE OUT WHY EXPAND BY 1
		double d0 = 0.0D;
		for (int a = 0; a < entities.size(); ++a)
		{
			Entity entity2 = (Entity)entities.get(a);
			if(entity2.canBeCollidedWith() && !entity2.equals(this.getCaster())) // Exclude entities here
			{
				AxisAlignedBB aabb = entity2.getEntityBoundingBox().expandXyz(0.30000001192092896D); // Why expand?
				RayTraceResult result2 = aabb.calculateIntercept(vector1, vector2);
				if (result2 != null)
				{
					double d1 = vector1.squareDistanceTo(result2.hitVec);
					if (d1 < d0 || d0 == 0.0D)
					{
						entity1 = entity2;
						d0 = d1;
					}
				}
			}
		}
		if (entity1 != null)
		{
			result1 = new RayTraceResult(entity1);
		}
		return result1;
	}

	/**
	 * Called when the projectile collides with a block or entity.
	 */
	protected abstract void onImpact(RayTraceResult result);

	/**
	 * Spawns particles around the projectile. Called from {@link #onUpdate()}.
	 */
	protected abstract void spawnParticleTrail();

	/**
	 * Determines if the projectile will die after colliding with water.
	 */
	public abstract boolean diesInWater();

	/**
	 * Determines if the projectile should burn like a fireball.
	 */
	public abstract boolean shouldBurn();

	/**
	 * Sets the projectile's speed on the three axes.
	 */
	public void setSpeed(double speedX, double speedY, double speedZ)
	{
		this.motionX = speedX;
		this.motionY = speedY;
		this.motionZ = speedZ;
	}

	/**
	 * Sets the projectile's speed depending on the caster's facing.
	 */
	public void setSpeedFromEntity(EntityLivingBase caster, double speed)
	{
		Vec3d vector = caster.getLookVec();
		this.setSpeed(vector.xCoord * speed, vector.yCoord * speed, vector.zCoord * speed);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		System.out.println("MELONSLISE - READING " + nbt.getUniqueId("ownerUUID"));
		if(nbt.hasKey("ownerUUID"))
		{
			this.ownerUUID = nbt.getUniqueId("ownerUUID");
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) // TODO
	{
		System.out.println("MELONSLISE - WRITING " + this.ownerUUID);
		nbt.setUniqueId("ownerUUID", this.ownerUUID);
	}

	@Nullable
	public EntityLivingBase getCaster() // TODO
	{
		if(!this.worldObj.isRemote)
		{
			System.out.println("MELONSLISE - GETTING ON SERVER " + this.ownerUUID);
			return this.worldObj.getPlayerEntityByUUID(this.ownerUUID);
		}
		else
		{
			return (EntityLivingBase) this.worldObj.getEntityByID(this.dataManager.get(OWNER).intValue());
		}
	}

	/** *** OLD ***
	@Nullable
	public EntityLivingBase getCaster()
	{
		if (this.caster == null && this.casterName != null && !this.casterName.isEmpty())
		{
			this.caster = this.worldObj.getPlayerEntityByName(this.casterName);

			if (this.caster == null && this.worldObj instanceof WorldServer)
			{
				try
				{
					Entity entity = ((WorldServer)this.worldObj).getEntityFromUuid(UUID.fromString(this.casterName));
					if (entity instanceof EntityLivingBase)
					{
						this.caster = (EntityLivingBase)entity;
					}
				}
				catch (Throwable error)
				{
					this.caster = null;
				}
			}
		}
		return this.caster;
	}
	*/
}