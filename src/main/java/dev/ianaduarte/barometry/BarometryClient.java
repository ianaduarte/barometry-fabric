package dev.ianaduarte.barometry;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.RenderPipelines;

public class BarometryClient implements ClientModInitializer {
	public static final RenderPipeline CLOUDS_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withVertexShader(Barometry.getLocation("core/rendertype_bclouds"))
			.withFragmentShader(Barometry.getLocation("core/rendertype_bclouds"))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_FAN)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withCull(false)
			.withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
			.withSampler("CloudCoverage")
			.withSampler("CloudForecast")
			.withLocation(Barometry.getLocation("pipeline/bclouds"))
			.build()
	);
	
	@Override
	public void onInitializeClient() {
	}
}
