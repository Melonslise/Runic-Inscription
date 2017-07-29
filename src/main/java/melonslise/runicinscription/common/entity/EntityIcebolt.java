package melonslise.runicinscription.common.entity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityIcebolt extends EntityFireball
{
	private int q = -1;
	private int w = -1;
	private int e = -1;
	private Block r;
	private boolean inGround;
	private int ticksAlive;
	private int ticksInAir;

	public EntityIcebolt(World world)
	{
		super(world);
		this.setSize(0.3125F, 0.3125F);
	}

	public EntityIcebolt(World world, double x, double y, double z, double j, double k, double l)
	{
		super(world, x, y, z, j, k, l);
		this.setSize(0.3125F, 0.3125F);
	}

	public EntityIcebolt(World world, EntityLivingBase entity, double x, double y, double z)
	{
		super(world, entity, x, y, z);
		this.setSize(0.3125F, 0.3125F);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float x)
	{
		return false;
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	@Override
	protected void onImpact(MovingObjectPosition position)
	{
		if(!this.worldObj.isRemote)
		{
			if (position.entityHit != null)
			{
				position.entityHit.attackEntityFrom(DamageSource.magic, 3.0F);

				((EntityLivingBase) position.entityHit).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2 * 20, 5));
			}
			else
			{
				switch (position.sideHit)
				{
				case 0:
					break;
				case 1:
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
				}

			}

			this.setDead();
		}

		for (int a = 0; a < 10; ++a)
		{
			this.worldObj.spawnParticle("snowballpoof", this.posX, this.posY, this.posZ, -this.motionX * this.rand.nextGaussian() * 0.15D, -0.2D, -this.motionZ * this.rand.nextGaussian() * 0.15D);
		}
	}

	@Override
	public void onUpdate()
	{
		if (!this.worldObj.isRemote && (((this.shootingEntity != null) && this.shootingEntity.isDead) || !this.worldObj.blockExists((int)this.posX, (int)this.posY, (int)this.posZ)))
		{
			this.setDead();
		}
		else
		{
			if (this.inGround)
			{
				if (this.worldObj.getBlock(this.q, this.w, this.e) == this.r)
				{
					++this.ticksAlive;

					if (this.ticksAlive == 600)
					{
						this.setDead();
					}

					return;
				}

				this.inGround = false;
				this.motionX *= this.rand.nextFloat() * 0.2F;
				this.motionY *= this.rand.nextFloat() * 0.2F;
				this.motionZ *= this.rand.nextFloat() * 0.2F;
				this.ticksAlive = 0;
				this.ticksInAir = 0;
			}
			else
			{
				++this.ticksInAir;
			}

			Vec3 vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			Vec3 vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec3, vec31);
			vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			if (movingobjectposition != null)
			{
				vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
			}

			Entity entity = null;
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double d0 = 0.0D;

			for (int i = 0; i < list.size(); ++i)
			{
				Entity entity1 = (Entity)list.get(i);

				if (entity1.canBeCollidedWith() && (!entity1.isEntityEqual(this.shootingEntity) || (this.ticksInAir >= 25)))
				{
					float f = 0.3F;
					AxisAlignedBB axisalignedbb = entity1.boundingBox.expand(f, f, f);
					MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec3, vec31);

					if (movingobjectposition1 != null)
					{
						double d1 = vec3.distanceTo(movingobjectposition1.hitVec);

						if ((d1 < d0) || (d0 == 0.0D))
						{
							entity = entity1;
							d0 = d1;
						}
					}
				}
			}

			if (entity != null)
			{
				movingobjectposition = new MovingObjectPosition(entity);
			}

			if (movingobjectposition != null)
			{
				this.onImpact(movingobjectposition);
			}

			this.posX += this.motionX;
			this.posY += this.motionY;
			this.posZ += this.motionZ;
			float f1 = MathHelper.sqrt_double((this.motionX * this.motionX) + (this.motionZ * this.motionZ));
			this.rotationYaw = (float)((Math.atan2(this.motionZ, this.motionX) * 180.0D) / Math.PI) + 90.0F;

			for (this.rotationPitch = (float)((Math.atan2(f1, this.motionY) * 180.0D) / Math.PI) - 90.0F; (this.rotationPitch - this.prevRotationPitch) < -180.0F; this.prevRotationPitch -= 360.0F)
			{
				;
			}

			while ((this.rotationPitch - this.prevRotationPitch) >= 180.0F)
			{
				this.prevRotationPitch += 360.0F;
			}

			while ((this.rotationYaw - this.prevRotationYaw) < -180.0F)
			{
				this.prevRotationYaw -= 360.0F;
			}

			while ((this.rotationYaw - this.prevRotationYaw) >= 180.0F)
			{
				this.prevRotationYaw += 360.0F;
			}

			this.rotationPitch = this.prevRotationPitch + ((this.rotationPitch - this.prevRotationPitch) * 0.2F);
			this.rotationYaw = this.prevRotationYaw + ((this.rotationYaw - this.prevRotationYaw) * 0.2F);
			float f2 = this.getMotionFactor();

			if (this.isInWater())
			{
				for (int j = 0; j < 4; ++j)
				{
					float f3 = 0.25F;
					this.worldObj.spawnParticle("bubble", this.posX - (this.motionX * f3), this.posY - (this.motionY * f3), this.posZ - (this.motionZ * f3), this.motionX, this.motionY, this.motionZ);
				}

				f2 = 0.8F;
			}

			this.motionX += this.accelerationX;
			this.motionY += this.accelerationY;
			this.motionZ += this.accelerationZ;
			this.motionX *= f2;
			this.motionY *= f2;
			this.motionZ *= f2;
			this.worldObj.spawnParticle("splash", this.posX, this.posY + 0.5D, this.posZ, 0.0D, 0.0D, 0.0D);
			this.setPosition(this.posX, this.posY, this.posZ);
		}
	}
}
