package dev.ianaduarte.barometry.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.MathUtil;
import dev.ianaduarte.barometry.ProjectionGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BarometryCloudsRenderer {
	private static final int DATA_BUFFER_SIZE = new Std140SizeCalculator()
		.putVec4()
		.putVec4()
		.putVec2()
		.get();
	private static final int SUBDIVISIONS = 32;
	
	private GpuBuffer vertexBuffer;
	private CloudRenderer.RelativeCameraPos prevRelativeCameraPos;
	private final RenderSystem.AutoStorageIndexBuffer indices;
	
	private final MappableRingBuffer cloudDataBuffer;
	private int indexCount;
	private final PerspectiveProjectionMatrixBuffer matrixBuffer;
	
	public BarometryCloudsRenderer() {
		this.indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		this.cloudDataBuffer = new MappableRingBuffer(
			() -> "Barometry Cloud Data UBO",
			GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, DATA_BUFFER_SIZE
		);
		this.matrixBuffer = new PerspectiveProjectionMatrixBuffer("BAROMETRY_CLOUDS");
	}
	
	public void render(boolean needsRebuild, int cloudColor, float height, double cloudOffset, float forecast, Vec3 cameraPos, float partialTicks) {
		float relativeY = (float)(height - cameraPos.y);
		CloudRenderer.RelativeCameraPos relativeCameraPos;
		
		if(relativeY < 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
		else if(relativeY > 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
		else relativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
		
		//float gameTime = Minecraft.getInstance().level.getGameTime() + partialTicks;
		//float mscale = (float)(Math.sin((gameTime * 1e-3)) * 0.1f);
		//forecast = Barometry.remap(forecast + mscale, -0.2f, 1.2f, 0, 1);
		
		if(needsRebuild || relativeCameraPos != this.prevRelativeCameraPos) {
			this.prevRelativeCameraPos = relativeCameraPos;
			
			try(MeshData meshData = buildClouds(Tesselator.getInstance())) {
				if(this.vertexBuffer != null && this.vertexBuffer.size >= meshData.vertexBuffer().remaining()) {
					CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
					commandEncoder.writeToBuffer(this.vertexBuffer.slice(), meshData.vertexBuffer());
				}
				else {
					if(this.vertexBuffer != null) this.vertexBuffer.close();
					
					this.vertexBuffer = RenderSystem
						.getDevice()
						.createBuffer(() -> "Cloud vertex buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, meshData.vertexBuffer());
				}
				
				this.indexCount = meshData.drawState().indexCount();
			}
		}
		
		if(this.indexCount == 0) return;
		
		Vector4f color = new Vector4f(
			ARGB.redFloat(cloudColor),
			ARGB.greenFloat(cloudColor),
			ARGB.blueFloat(cloudColor),
			0.8f
		);
		float darkness = 1 - (forecast * 0.25f);
		color.mul(darkness, darkness, darkness, 1);
		
		float cloudX = (float)(cameraPos.x / 12);
		float cloudZ = (float)(cameraPos.z / 12);
		float modulatorOffset = (float)((cloudOffset * 0.03) / 256);
		float cloudLayerOffset = (float)((cloudOffset * 0.01) / 256);
		
		Matrix4f extendedFarplane = ((ProjectionGetter)Minecraft.getInstance().gameRenderer).fetchProjectionMatrix(10_000, partialTicks);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(this.matrixBuffer.getBuffer(extendedFarplane), ProjectionType.PERSPECTIVE);
		GpuBuffer indexBuffer = this.indices.getBuffer(this.indexCount);
		RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
		RenderTarget cloudsTarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
		
		GpuTextureView colorBuffer;
		GpuTextureView depthBuffer;
		if(cloudsTarget != null) {
			colorBuffer = cloudsTarget.getColorTextureView();
			depthBuffer = cloudsTarget.getDepthTextureView();
		}
		else {
			colorBuffer = mainTarget.getColorTextureView();
			depthBuffer = mainTarget.getDepthTextureView();
		}
		switch(relativeCameraPos) {
			case INSIDE_CLOUDS, ABOVE_CLOUDS -> {
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY - 4, cloudZ, color, 0, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY    , cloudZ, color, 1, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY + 4, cloudZ, color, 2, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY + 8, cloudZ, color, 3, forecast, cloudLayerOffset, modulatorOffset);
			}
			case BELOW_CLOUDS -> {
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY + 8, cloudZ, color, 3, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY + 4, cloudZ, color, 2, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY    , cloudZ, color, 1, forecast, cloudLayerOffset, modulatorOffset);
				this.drawWithRenderTypeProc(colorBuffer, depthBuffer, indexBuffer, cloudX, relativeY - 4, cloudZ, color, 0, forecast, cloudLayerOffset, modulatorOffset);
			}
		}
		RenderSystem.restoreProjectionMatrix();
	}
	private static MeshData buildClouds(Tesselator tesselator) {
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		
		final int delta = SUBDIVISIONS / 2;
		float uvDelta = 1f / SUBDIVISIONS;
		float coordDelta = 256f / SUBDIVISIONS;
		for (int y = -delta; y < delta; y++) {
			for (int x = -delta; x < delta; x++) {
				float xPos = x * coordDelta;
				float yPos = y * coordDelta;
				
				float uPos = (x + delta) * uvDelta;
				float vPos = (y + delta) * uvDelta;
				
				buildVertex(builder, xPos, yPos, uPos, vPos);
				buildVertex(builder, xPos + coordDelta, yPos, uPos + uvDelta, vPos);
				buildVertex(builder, xPos + coordDelta, yPos + coordDelta, uPos + uvDelta, vPos + uvDelta);
				buildVertex(builder, xPos, yPos + coordDelta, uPos, vPos + uvDelta);
			}
		}
		
		return builder.build();
	}
	private static void buildVertex(BufferBuilder builder, float x, float y, float u, float v) {
		float sg = (float)MathUtil.squareGradient(u, v, 4.1);
		builder.addVertex(x, sg * -24, y).setUv(u, v);
	}
	
	private void drawWithRenderTypeProc(GpuTextureView colorBuffer, GpuTextureView depthBuffer, GpuBuffer indexBuffer, float x, float y, float z, Vector4f color, int layer, float forecast, float cloudOffset, float modOffset) {
		GpuTextureView noiseTexture = Minecraft.getInstance().getTextureManager().getTexture(Barometry.NOISE2_LOCATION).getTextureView();
		GpuTextureView noiseModTexture = Minecraft.getInstance().getTextureManager().getTexture(Barometry.NOISE2_ALT_LOCATION).getTextureView();
		
		this.setCloudInfo(color.x, color.y, color.z, Barometry.scaleAlpha(layer, color.w), x / 256, z / 256, cloudOffset, modOffset, y, Barometry.scaleForecast(layer, forecast));
		
		try(RenderPass renderPass = RenderSystem
			.getDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "Barometry Clouds #" + layer, colorBuffer, OptionalInt.empty(), depthBuffer, OptionalDouble.empty())
		) {
			renderPass.setPipeline(Barometry.BAROMETRY_CLOUDS_PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.bindSampler("Sampler0", noiseTexture);
			renderPass.bindSampler("Sampler1", noiseModTexture);
			renderPass.setUniform("CloudInfo", this.cloudDataBuffer.currentBuffer());
			renderPass.setIndexBuffer(indexBuffer, this.indices.type());
			renderPass.setVertexBuffer(0, this.vertexBuffer);
			renderPass.drawIndexed(0, 0, this.indexCount, 1);
		}
		this.cloudDataBuffer.rotate();
	}
	private void setCloudInfo(float r, float g, float b, float a, float uvX, float uvY, float xOffset, float yOffset, float cloudHeight, float forecast) {
		try(GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.cloudDataBuffer.currentBuffer(), false, true)) {
			Std140Builder.intoBuffer(mappedView.data())
				.putVec4(r, g, b, a)
				.putVec4(uvX, uvY, cloudHeight, forecast)
				.putVec2(xOffset, yOffset);
		}
	}
}