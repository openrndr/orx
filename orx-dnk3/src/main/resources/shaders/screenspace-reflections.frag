#version 330
// --- varyings ---
in vec2 v_texCoord0;

// --- G buffer ---
uniform sampler2D colors;
uniform sampler2D projDepth;
uniform sampler2D normals;

// --- transforms ---
uniform mat4 projection;
uniform mat4 projectionMatrixInverse;

// --- output ---
layout(location = 0) out vec4 o_color;


// --- parameters ---
uniform float jitterOriginGain;
uniform int iterationLimit;
uniform float distanceLimit;
uniform float gain;
uniform float borderWidth;

float distanceSquared(vec2 a, vec2 b) {
    vec2 d = b-a;
    return dot(d,d);
}

#pragma import org.openrndr.extra.shaderphrases.phrases.Depth.projectionToViewCoordinate;
#pragma import org.openrndr.extra.shaderphrases.phrases.Depth.projectionToViewDepth;

#pragma import org.openrndr.extra.noise.phrases.NoisePhrasesKt.phraseHash22;


// this is from http://casual-effects.blogspot.nl/2014/08/screen-space-ray-tracing.html

void swap(inout float a, inout float b) {
    float temp = a;
    a = b;
    b = temp;
}


bool traceScreenSpaceRay1
   (vec3          csOrigin,
    vec3         csDirection,
    mat4x4          projectToPixelMatrix,
    sampler2D       csZBuffer,
    vec2            csZBufferSize,
    float           csZThickness,
    float           nearPlaneZ,
    float			stride,
    float           jitterFraction,
    float           maxSteps,
    in float        maxRayTraceDistance,
    out vec2      hitPixel,
	out vec3		csHitPoint,
    out vec3 csHitNormal
//    ,out vec3      debugColor
    ) {
    vec3 debugColor = vec3(0);
    // Clip ray to a near plane in 3D (doesn't have to be *the* near plane, although that would be a good idea)
    float rayLength = ((csOrigin.z + csDirection.z * maxRayTraceDistance) > nearPlaneZ) ?
                        (nearPlaneZ - csOrigin.z) / csDirection.z :
                        maxRayTraceDistance;
	vec3 csEndPoint = csDirection * rayLength + csOrigin;

    // Project into screen space
    vec4 H0 = projectToPixelMatrix * vec4(csOrigin, 1.0);
    vec4 H1 = projectToPixelMatrix * vec4(csEndPoint, 1.0);

    // There are a lot of divisions by w that can be turned into multiplications
    // at some minor precision loss...and we need to interpolate these 1/w values
    // anyway.
    //
    // Because the caller was required to clip to the near plane,
    // this homogeneous division (projecting from 4D to 2D) is guaranteed
    // to succeed.
    float k0 = 1.0 / H0.w;
    float k1 = 1.0 / H1.w;

    // Switch the original points to values that interpolate linearly in 2D
    vec3 Q0 = csOrigin * k0;
    vec3 Q1 = csEndPoint * k1;

	// Screen-space endpoints
    vec2 P0 = H0.xy * k0;
    vec2 P1 = H1.xy * k1;

    // [Optional clipping to frustum sides here]

    // Initialize to off screen
    hitPixel = vec2(-1.0, -1.0);

    // If the line is degenerate, make it cover at least one pixel
    // to avoid handling zero-pixel extent as a special case later
    P1 += vec2((distanceSquared(P0, P1) < 0.0001) ? 0.01 : 0.0);

    vec2 delta = P1 - P0;

    // Permute so that the primary iteration is in x to reduce
    // large branches later
    bool permute = (abs(delta.x) < abs(delta.y));
	if (permute) {
		// More-vertical line. Create a permutation that swaps x and y in the output
        // by directly swizzling the inputs.
		delta = delta.yx;
		P1 = P1.yx;
		P0 = P0.yx;
	}

	// From now on, "x" is the primary iteration direction and "y" is the secondary one
    float stepDirection = sign(delta.x);
    float invdx = stepDirection / delta.x;
    vec2 dP = vec2(stepDirection, invdx * delta.y);

    // Track the derivatives of Q and k
    vec3 dQ = (Q1 - Q0) * invdx;
    float   dk = (k1 - k0) * invdx;

    // Because we test 1/2 a texel forward along the ray, on the very last iteration
    // the interpolation can go past the end of the ray. Use these bounds to clamp it.
    float zMin = min(csEndPoint.z, csOrigin.z);
    float zMax = max(csEndPoint.z, csOrigin.z);

    // Scale derivatives by the desired pixel stride
	dP *= stride; dQ *= stride; dk *= stride;

    // Offset the starting values by the jitter fraction
	P0 += dP * jitterFraction; Q0 += dQ * jitterFraction; k0 += dk * jitterFraction;

	// Slide P from P0 to P1, (now-homogeneous) Q from Q0 to Q1, and k from k0 to k1
    vec3 Q = Q0;
    float  k = k0;

	// We track the ray depth at +/- 1/2 pixel to treat pixels as clip-space solid
	// voxels. Because the depth at -1/2 for a given pixel will be the same as at
	// +1/2 for the previous iteration, we actually only have to compute one value
	// per iteration.
	float prevZMaxEstimate = csOrigin.z;
    float stepCount = 0.0;
    float rayZMax = prevZMaxEstimate, rayZMin = prevZMaxEstimate;
    float sceneZMax = rayZMax + 1e4;

    // P1.x is never modified after this point, so pre-scale it by
    // the step direction for a signed comparison
    float end = P1.x * stepDirection;

    // We only advance the z field of Q in the inner loop, since
    // Q.xy is never used until after the loop terminates.

    vec2 P;
	for (P = P0;
        ((P.x * stepDirection) <= end) &&
        (stepCount < maxSteps) &&
        ((rayZMax < sceneZMax - csZThickness) ||
            (rayZMin > sceneZMax)) &&
        (sceneZMax != 0.0);
        P += dP, Q.z += dQ.z, k += dk, stepCount += 1.0) {

        // The depth range that the ray covers within this loop
        // iteration.  Assume that the ray is moving in increasing z
        // and swap if backwards.  Because one end of the interval is
        // shared between adjacent iterations, we track the previous
        // value and then swap as needed to ensure correct ordering
        rayZMin = prevZMaxEstimate;

        // Compute the value at 1/2 step into the future
        rayZMax = (dQ.z * 0.5 + Q.z) / (dk * 0.5 + k);

        // -- this is not in the other implementation
        rayZMax = clamp(rayZMax, zMin, zMax);

		prevZMaxEstimate = rayZMax;

        // Since we don't know if the ray is stepping forward or backward in depth,
        // maybe swap. Note that we preserve our original z "max" estimate first.
        if (rayZMin > rayZMax) { swap(rayZMin, rayZMax); }

        // Camera-space z of the background
        hitPixel = permute ? P.yx : P;

         vec4 depthData = texelFetch(csZBuffer, ivec2(hitPixel), 0);
        sceneZMax = projectionToViewCoordinate(v_texCoord0, depthData.x, projectionMatrixInverse).z;

    } // pixel on ray

    // Undo the last increment, which ran after the test variables
    // were set up.
    P -= dP; Q.z -= dQ.z; k -= dk; stepCount -= 1.0;

    bool hit = (rayZMax >= sceneZMax - csZThickness) && (rayZMin <= sceneZMax);

    // If using non-unit stride and we hit a depth surface...
    if ((stride > 1) && hit) {
        // Refine the hit point within the last large-stride step

        // Retreat one whole stride step from the previous loop so that
        // we can re-run that iteration at finer scale
        P -= dP; Q.z -= dQ.z; k -= dk; stepCount -= 1.0;

        // Take the derivatives back to single-pixel stride
        float invStride = 1.0 / stride;
        dP *= invStride; dQ.z *= invStride; dk *= invStride;

        // For this test, we don't bother checking thickness or passing the end, since we KNOW there will
        // be a hit point. As soon as
        // the ray passes behind an object, call it a hit. Advance (stride + 1) steps to fully check this
        // interval (we could skip the very first iteration, but then we'd need identical code to prime the loop)
        float refinementStepCount = 0;

        // This is the current sample point's z-value, taken back to camera space
        prevZMaxEstimate = Q.z / k;
        rayZMin = prevZMaxEstimate;

        // Ensure that the FOR-loop test passes on the first iteration since we
        // won't have a valid value of sceneZMax to test.
        sceneZMax = rayZMin - 1e7;

        for (;
            (refinementStepCount <= stride*1.4) &&
            (rayZMin > sceneZMax) && (sceneZMax != 0.0);
            P += dP, Q.z += dQ.z, k += dk, refinementStepCount += 1.0) {

            rayZMin = prevZMaxEstimate;

            // Compute the ray camera-space Z value at 1/2 fine step (pixel) into the future
            rayZMax = (dQ.z * 0.5 + Q.z) / (dk * 0.5 + k);
            rayZMax = clamp(rayZMax, zMin, zMax);

            prevZMaxEstimate = rayZMax;
            rayZMin = min(rayZMax, rayZMin);

            hitPixel = permute ? P.yx : P;

             vec4 depthData = texelFetch(csZBuffer, ivec2(hitPixel), 0);
            sceneZMax = projectionToViewCoordinate(v_texCoord0, depthData.x, projectionMatrixInverse).z;


            csHitNormal = texelFetch(normals, ivec2(hitPixel), 0).xyz;

//            sceneZMax = texelFetch(csZBuffer, ivec2(hitPixel), 0).r;

        }

        // Undo the last increment, which happened after the test variables were set up
        Q.z -= dQ.z; refinementStepCount -= 1;

        // Count the refinement steps as fractions of the original stride. Save a register
        // by not retaining invStride until here
        stepCount += refinementStepCount / stride;
      //  debugColor = vec3(refinementStepCount / stride);
    } // refinement

    Q.xy += dQ.xy * stepCount;
	csHitPoint = Q * (1.0 / k);

    // Support debugging. This will compile away if debugColor is unused
    if ((P.x * stepDirection) > end) {
        // Hit the max ray distance -> blue
        debugColor = vec3(0,0,1);
    } else if (stepCount >= maxSteps) {
        // Ran out of steps -> red
        debugColor = vec3(1,0,0);
    } else if (sceneZMax == 0.0) {
        // Went off screen -> yellow
        debugColor = vec3(1,1,0);
    } else {
        // Encountered a valid hit -> green
        // ((rayZMax >= sceneZMax - csZThickness) && (rayZMin <= sceneZMax))
        debugColor = vec3(0,1,0);
    }

    // Does the last point discovered represent a valid hit?
    return hit;
}


void main() {
    vec2 hitPixel = vec2(0.0, 0.0);
    vec3 hitPoint = vec3(0.0, 0.0, 0.0);
    vec3 hitNormal = vec3(0.0, 0.0, 0.0);

    vec2 jitter = abs(hash22(v_texCoord0));


    vec2 ts = vec2(textureSize(projDepth, 0).xy);
    vec3 viewNormal = normalize(texture(normals, v_texCoord0).xyz);// + (texture(noise, v_texCoord0*0.1).xyz - 0.5) * 0.0;
    float depth = texture(projDepth, v_texCoord0).r;
    vec3 viewPos = projectionToViewCoordinate(v_texCoord0, depth, projectionMatrixInverse);


    vec3 reflected = normalize(reflect(normalize(viewPos), normalize(-viewNormal)));


    float angle = abs(dot(reflected, viewNormal));
    float frontalFade = clamp(-reflected.z,0, 1);
    if ( true ) {
        bool hit = traceScreenSpaceRay1(
            viewPos,
            reflected,
            projection,
            projDepth,
            ts,
            0.1,
            0.0, // near plane z
            1.0,// + projPos.z*2.0, // stride
            10.0, // jitterfraction
            iterationLimit*8,// + int((1.0-projPos.z)*iterationLimit),
            100.0, // max distance

            hitPixel,
            hitPoint, hitNormal);

        float distanceFade = 1.0;//max( 0.0, (distanceLimit -length(hitPoint-viewPos))/ distanceLimit);
        vec4 p = projection * vec4(hitPoint, 1.0);

        float k = 1.0 / p.w;

        vec2 pos = vec2(p.xy*k);
        vec2 ad = vec2(ts/2- abs(pos - ts/2));
        float borderFade = 1.0; //smoothstep(0, borderWidth, min(ad.x, ad.y));

        float l = 0.0;
        int l0 = int(l);
        int l1 = l0 + 1;

        float lf = l - l0;

        vec4 reflectedColor0 = texelFetch(colors, ivec2(p.xy*k)/(1<<l0), l0);
        vec4 reflectedColor1 = texelFetch(colors, ivec2(p.xy*k)/(1<<l1), l1);

        vec4 reflectedColor = reflectedColor0 * (1.0-lf) + reflectedColor1 * lf;

      // vec2 uv = vec2(p.xy*k) / textureSize(colors, 0);

        //reflectedColor = textureLod(colors, uv, l);

        float hitFade = hit? 1.0: 0.0;
        float angleFade = 1.0;/// smoothstep(0.0, 0.3, angle);;//angle < 0.5? 0.0 : 1.0;
        float faceFade = 1.0; //step(0.00001, dot(-normalize(hitNormal), reflected));
        o_color.rgb = (1.0 * reflectedColor.rgb * hitFade  * frontalFade * distanceFade * borderFade * angleFade * faceFade) + texture(colors, v_texCoord0).rgb;
        o_color.a = 1.0;
    } else {
        o_color =  texture(colors, v_texCoord0).rgba;
        o_color.a = 1.0;
    }
}
