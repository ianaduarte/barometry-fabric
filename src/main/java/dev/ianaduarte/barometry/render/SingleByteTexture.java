package dev.ianaduarte.barometry.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;
import java.util.UUID;

public class SingleByteTexture extends AbstractTexture {
	private final int size;
	
	public SingleByteTexture(int size, boolean integer) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		UUID uuid = UUID.randomUUID();
		
		this.size = size;
		this.texture = gpuDevice.createTexture(() -> "forecast#" + uuid, GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, integer? TextureFormat.RED8I : TextureFormat.RED8, size, size, 1, 1);
		this.textureView = gpuDevice.createTextureView(this.texture);
		
		this.setFilter(true, true);
		this.setClamp(true);
		gpuDevice.createCommandEncoder().clearColorTexture(this.texture, 0);
	}
	
	public void writeValues(byte[] values) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		ByteBuffer bb = ByteBuffer.allocateDirect(values.length);
		bb.put(values);
		bb.flip();
		gpuDevice.createCommandEncoder().writeToTexture(this.texture, bb, NativeImage.Format.LUMINANCE, 0, 0, 0, 0, this.size, this.size);
	}
}
