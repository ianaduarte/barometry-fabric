package dev.ianaduarte.barometry.render;

import org.joml.Matrix4f;

public interface ProjectionGetter {
	Matrix4f calculateExtendedFarplaneMatrix(float farPlane, float partialTicks);
}
