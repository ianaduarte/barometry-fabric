package dev.ianaduarte.barometry.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import dev.ianaduarte.barometry.BarometryClient;
import dev.ianaduarte.barometry.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class BarometryCloudRenderer {
	private static final int DATA_BUFFER_SIZE = new Std140SizeCalculator()
		.putFloat().putFloat().putFloat().putFloat() //color
		.putFloat().putFloat()                       //uvOffsets
		.putFloat().putFloat()                       //cameraPos
		.putFloat()                                  //layerOffset
		.putFloat()                                  //height,
		.putInt().putInt()                           //layerWidth, layerCount
		.get();
	private SingleByteTexture coverageTexture, forecastTexture;
	private byte[] coverageData, forecastData;
	private int cloudRadius, cloudDiameter;
	private boolean texturesDirty;
	
	private float cloudOffsetPrev, cloudOffset;
	
	private GpuBuffer vertexBuffer;
	private int indexCount;
	private final RenderSystem.AutoStorageIndexBuffer indexBuffer;
	private final PerspectiveProjectionMatrixBuffer matrixBuffer;
	private final MappableRingBuffer cloudDataBuffer;
	
	private boolean needsRebuild = true;
	private final Minecraft minecraft;
	
	public BarometryCloudRenderer(Minecraft minecraft) {
		this.indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLE_FAN);
		this.matrixBuffer = new PerspectiveProjectionMatrixBuffer("BAROMETRY_CLOUDS");
		this.cloudDataBuffer = new MappableRingBuffer(
			() -> "Barometry CloudData UBO",
			GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, DATA_BUFFER_SIZE
		);
		this.minecraft = minecraft;
	}
	
	public void tick(ClientLevel level) {
		this.cloudOffsetPrev = this.cloudOffset;
		
		if(level.tickRateManager().isFrozen()) return;
		this.cloudOffset += (0.001f) * (MathUtil.sat(level.tickRateManager().tickrate() * 0.05f));
	}
	
	public void markForRebuild() {
		this.needsRebuild = true;
	}
	public void render(int cloudColor, float cloudHeight, Vec3 cameraPos, float partialTicks) {
		this.rebuildMesh();
		if(this.indexCount == 0) return;
		
		GpuBuffer indexBuffer = this.indexBuffer.getBuffer(this.indexCount);
		Pair<GpuTextureView, GpuTextureView> renderBuffers = this.getColorAndDepthBuffers();
		
		this.preRender(cloudColor, cloudHeight, cameraPos, partialTicks);
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(
				RenderSystem.getModelViewMatrix(),
				new Vector4f(),
				new Vector3f(),
				new Matrix4f(),
				0.0F
			);
		try(RenderPass renderPass = RenderSystem
			.getDevice()
			.createCommandEncoder()
			.createRenderPass(
				() -> "Barometry Clouds",
				renderBuffers.getFirst(), OptionalInt.empty(),
				renderBuffers.getSecond(), OptionalDouble.empty()
			)
		) {
			renderPass.setPipeline(BarometryClient.CLOUDS_PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.bindSampler("CloudCoverage", this.coverageTexture.getTextureView());
			renderPass.bindSampler("CloudForecast", this.forecastTexture.getTextureView());
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);
			renderPass.setUniform("CloudInfo", this.cloudDataBuffer.currentBuffer());
			renderPass.setIndexBuffer(indexBuffer, this.indexBuffer.type());
			renderPass.setVertexBuffer(0, this.vertexBuffer);
			renderPass.drawIndexed(0, 0, this.indexCount, this.cloudDiameter * this.cloudDiameter);
		}
		this.postRender();
	}
	private void preRender(int packedColor, float cloudHeight, Vec3 cameraPos, float partialTicks) {
		Vector4f cloudColor = new Vector4f(ARGB.redFloat(packedColor), ARGB.greenFloat(packedColor), ARGB.blueFloat(packedColor), 0.8f);
		float normCloudHeight = (float)(cloudHeight - cameraPos.y);
		float uvOffsetX = (float)(MathUtil.mod(cameraPos.x, 16) * 0.125 - 1);
		float uvOffsetY = (float)(MathUtil.mod(cameraPos.z, 16) * 0.125 - 1);
		float noiseOffset = Mth.lerp(MathUtil.sat(partialTicks), this.cloudOffsetPrev, this.cloudOffset);
		
		Matrix4f projMatrix = ((ProjectionGetter)this.minecraft.gameRenderer).calculateExtendedFarplaneMatrix(1e4f, partialTicks);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(this.matrixBuffer.getBuffer(projMatrix), ProjectionType.PERSPECTIVE);
		
		this.setCloudInfo(cloudColor, uvOffsetX, uvOffsetY, (float)cameraPos.x / 16, (float)cameraPos.z / 16, noiseOffset, normCloudHeight, this.cloudDiameter, 1);
		this.setTextures(cameraPos, partialTicks);
	}
	private void postRender() {
		this.cloudDataBuffer.rotate();
		RenderSystem.restoreProjectionMatrix();
	}
	private Pair<GpuTextureView, GpuTextureView> getColorAndDepthBuffers() {
		RenderTarget mainTarget = this.minecraft.getMainRenderTarget();
		RenderTarget cloudsTarget = this.minecraft.levelRenderer.getCloudsTarget();
		
		return (cloudsTarget != null)
			? Pair.of(cloudsTarget.getColorTextureView(), cloudsTarget.getDepthTextureView())
			: Pair.of(mainTarget  .getColorTextureView(), mainTarget  .getDepthTextureView());
	}
	
	private void setCloudInfo(Vector4f color, float uvX, float uvY, float camX, float camZ, float noiseOffset, float height, int layerDiameter, int layerCount) {
		try(GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.cloudDataBuffer.currentBuffer(), false, true)) {
			Std140Builder.intoBuffer(mappedView.data())
				.putFloat(color.x).putFloat(color.y).putFloat(color.z).putFloat(color.w)
				.putFloat(uvX).putFloat(uvY)
				.putFloat(camX).putFloat(camZ)
				.putFloat(noiseOffset)
				.putFloat(height)
				.putInt(layerDiameter + 2)
				.putInt(layerCount)
			;
		}
	}
	private void setTextures(Vec3 cameraPos, float partialTicks) {
		if(!this.texturesDirty) return;
		this.texturesDirty = false;
		
		Player player = this.minecraft.player;
		if(player == null) return;
		
		//int playerChunkX = (int)Math.floor(Mth.lerp(partialTicks, player.xo, player.getX()) / 16);
		//int playerChunkZ = (int)Math.floor(Mth.lerp(partialTicks, player.zo, player.getZ()) / 16);
		//
		//int paddedDiameter = this.cloudDiameter + 2;
		//byte[] bytes = new byte[paddedDiameter * paddedDiameter];
		//
		//for(int i = 0; i < bytes.length; i++) {
		//	int x = (i / paddedDiameter);
		//	int z = (i % paddedDiameter);
		//	int xx = x + playerChunkX;
		//	int zz = z + playerChunkZ;
		//
		//	if((x == 0 || x == paddedDiameter - 1) || (z == 0 || z == paddedDiameter - 1)) {
		//		bytes[i] = (byte)127;
		//		continue;
		//	}
		//
		//	boolean even = (xx + zz) % 2 == 0;
		//	if(even) bytes[i] = (byte)255;
		//}
		this.coverageTexture.writeValues(this.coverageData);
		this.forecastTexture.writeValues(this.forecastData);
	}
	private void resize(int newRadius) {
		if(this.cloudRadius == newRadius) return;
		
		this.cloudRadius = newRadius;
		this.cloudDiameter = newRadius * 2 + 1;
		
		int paddedDiameter = this.cloudDiameter + 2;
		if(this.coverageTexture != null) this.coverageTexture.close();
		if(this.forecastTexture != null) this.forecastTexture.close();
		
		this.coverageData = new byte[paddedDiameter * paddedDiameter];
		this.forecastData = new byte[paddedDiameter * paddedDiameter];
		this.coverageTexture = new SingleByteTexture(paddedDiameter, false);
		this.forecastTexture = new SingleByteTexture(paddedDiameter, false);
	}
	private void rebuildMesh() {
		if(!this.needsRebuild) return;
		this.cloudRadius = -1;
		this.resize(this.minecraft.options.cloudRange().get());
		
		try(MeshData meshData = buildCloudSlice(Tesselator.getInstance())) {
			if(this.vertexBuffer != null && this.vertexBuffer.size() >= meshData.vertexBuffer().remaining()) {
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
		this.needsRebuild = false;
	}
	
	private static MeshData buildCloudSlice(Tesselator tesselator) {
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);
		
		builder.addVertex( 0.0f, 0.0f,  0.0f).setUv(0.5f, 0.5f);
		
		builder.addVertex(-0.5f, 0.0f, -0.5f).setUv(0.0f, 0.0f);
		builder.addVertex( 0.5f, 0.0f, -0.5f).setUv(1.0f, 0.0f);
		builder.addVertex( 0.5f, 0.0f,  0.5f).setUv(1.0f, 1.0f);
		builder.addVertex(-0.5f, 0.0f,  0.5f).setUv(0.0f, 1.0f);
		builder.addVertex(-0.5f, 0.0f, -0.5f).setUv(0.0f, 0.0f);
		
		return builder.build();
	}
	
	public void updateTextures(int sampleRadius, byte[] coverage, byte[] forecast) {
		this.resize(sampleRadius - 1);
		this.texturesDirty = true;
		System.arraycopy(coverage, 0, this.coverageData, 0, coverage.length);
		System.arraycopy(forecast, 0, this.forecastData, 0, forecast.length);
	}
}

