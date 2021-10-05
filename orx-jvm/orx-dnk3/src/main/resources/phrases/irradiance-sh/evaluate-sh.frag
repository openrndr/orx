vec3 evaluateSH(vec3 direction, vec3[9] _SH) {
    const float c1 = 0.42904276540489171563379376569857;    // 4 * Â2.Y22 = 1/4 * sqrt(15.PI)
    const float c2 = 0.51166335397324424423977581244463;    // 0.5 * Â1.Y10 = 1/2 * sqrt(PI/3)
    const float c3 = 0.24770795610037568833406429782001;    // Â2.Y20 = 1/16 * sqrt(5.PI)
    const float c4 = 0.88622692545275801364908374167057;    // Â0.Y00 = 1/2 * sqrt(PI)

    float x = direction.x;
    float y = direction.y;
    float z = direction.z;

    return max(vec3(0.0),
    _SH[8] * (c1 * (x * x - y * y))                       // c1.L22.(x²-y²)
    + _SH[6] * (c3 * (3.0 * z * z - 1))                   // c3.L20.(3.z² - 1)
    + _SH[0] * c4                                   // c4.L00
    + (_SH[4] * x * y + _SH[7] * x * z + _SH[5] * y * z) * 2.0 * c1 // 2.c1.(L2-2.xy + L21.xz + L2-1.yz)
    + (_SH[3] * x + _SH[1] * y + _SH[2] * z) * c2 * 2.0);    // 2.c2.(L11.x + L1-1.y + L10.z)
}