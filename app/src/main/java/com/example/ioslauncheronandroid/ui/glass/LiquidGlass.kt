package com.example.ioslauncheronandroid.ui.glass

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ioslauncheronandroid.data.LauncherSettings

const val BackgroundScale = 1.08f

const val ParallaxDp = 14f

private const val LIQUID_SRC = """
uniform shader image;
uniform float2 size;
uniform float2 origin;
uniform float2 center;
uniform float2 tshift;
uniform float2 invK;
uniform float scaleS;
uniform float radius;
uniform float refr;
uniform float lift;
uniform float2 light;
uniform float screenH;
uniform float dimTop;
uniform float dimBottom;

float sdBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

float3 bg(float2 fc) {
    float2 p = fc + origin;
    float2 b = (((p - tshift - center) / scaleS) + center) * invK;
    float dim = mix(dimTop, dimBottom, clamp(p.y / screenH, 0.0, 1.0));
    return image.eval(b).rgb * (1.0 - dim);
}

half4 main(float2 fc) {
    float2 hs = size * 0.5;
    float r = min(radius, min(hs.x, hs.y));
    float2 p = fc - hs;
    float d = sdBox(p, hs, r);
    float shape = 1.0 - smoothstep(-1.0, 0.75, d);
    if (shape <= 0.0) return half4(0.0);

    float2 gx = float2(1.0, 0.0);
    float2 gy = float2(0.0, 1.0);
    float2 n = float2(sdBox(p + gx, hs, r) - sdBox(p - gx, hs, r),
                      sdBox(p + gy, hs, r) - sdBox(p - gy, hs, r));
    n = n / max(length(n), 0.0001);

    float m = min(hs.x, hs.y);
    float depth = clamp(-d / (m * 0.85), 0.0, 1.0);
    float edge = pow(1.0 - depth, 2.4);
    float2 off = -n * edge * refr;

    float3 col;
    col.r = bg(fc + off * 1.10).r;
    col.g = bg(fc + off).g;
    col.b = bg(fc + off * 0.90).b;

    float lum = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(lum), col, 1.12);
    col = (col - 0.5) * 1.06 + 0.5;
    col = mix(col, float3(1.0), lift);

    float rim = smoothstep(0.0, 1.2, -d) * (1.0 - smoothstep(1.2, 4.0, -d));
    float2 L = normalize(float2(-0.55, -0.85) + light * 0.7);
    float facing = max(dot(n, L), 0.0);
    float back = max(dot(n, -L), 0.0);
    col += rim * (0.85 * pow(facing, 3.0) + 0.30 * pow(back, 4.0));

    float band = (1.0 - smoothstep(0.0, 14.0, -d));
    col += band * (0.10 * pow(facing, 2.0) - 0.07 * pow(back, 2.0));
    col += (1.0 - smoothstep(0.0, 0.5, fc.y / size.y)) * 0.035;

    return half4(half3(clamp(col, 0.0, 1.0)) * half(shape), half(shape));
}
"""

@RequiresApi(33)
private class LiquidShader(bitmap: android.graphics.Bitmap) {
    val shader = RuntimeShader(LIQUID_SRC)
    val brush = ShaderBrush(shader)

    init {
        val bs = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        bs.filterMode = BitmapShader.FILTER_MODE_LINEAR
        shader.setInputShader("image", bs)
    }
}

fun Modifier.liquidGlass(
    radius: Dp? = null,
    elevation: Int = 10,
    lift: Float? = null,

    tick: (() -> Float)? = null,
): Modifier = composed {
    val backdrop = LocalBackdrop.current
    val soft = backdrop?.soft
    val shape: Shape = if (radius == null) RoundedCornerShape(percent = 50) else RoundedCornerShape(radius)
    val liquid = remember(soft) {
        if (Build.VERSION.SDK_INT >= 33 && soft != null) {
            runCatching { LiquidShader(soft.asAndroidBitmap()) }.getOrNull()
        } else null
    }
    if (liquid == null || backdrop == null || soft == null) {
        return@composed this.glass(shape, elevation = elevation)
    }
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val density = LocalDensity.current
    this
        .onGloballyPositioned { coords[0] = it }
        .shadow(elevation.dp, shape, clip = false, ambientColor = Color.Black.copy(0.22f), spotColor = Color.Black.copy(0.22f))
        .clip(shape)
        .drawBehind {
            if (Build.VERSION.SDK_INT < 33) return@drawBehind
            val sh = liquid.shader
            tick?.invoke()
            val screen = backdrop.size
            val at = coords[0]
            if (screen.width == 0 || at == null || !at.isAttached) return@drawBehind
            val origin = at.positionInWindow()
            val amp = ParallaxDp * density.density
            sh.setFloatUniform("size", size.width, size.height)
            sh.setFloatUniform("origin", origin.x, origin.y)
            sh.setFloatUniform("center", screen.width / 2f, screen.height / 2f)
            sh.setFloatUniform("tshift", -Tilt.dx * amp, -Tilt.dy * amp)
            sh.setFloatUniform("invK", soft.width / screen.width.toFloat(), soft.height / screen.height.toFloat())
            sh.setFloatUniform("scaleS", BackgroundScale)
            sh.setFloatUniform("radius", if (radius == null) 100000f else radius.toPx())
            sh.setFloatUniform("refr", 20f * density.density)
            sh.setFloatUniform("lift", lift ?: (LauncherSettings.glassAlpha * 0.45f))
            sh.setFloatUniform("light", Tilt.dx, Tilt.dy)
            sh.setFloatUniform("screenH", screen.height.toFloat())
            sh.setFloatUniform("dimTop", LauncherSettings.dim * 0.6f)
            sh.setFloatUniform("dimBottom", LauncherSettings.dim * 1.4f)
            drawRect(liquid.brush)
        }
        .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
}
