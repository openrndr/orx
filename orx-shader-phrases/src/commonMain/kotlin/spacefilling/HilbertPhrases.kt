package org.openrndr.extra.shaderphrases.spacefilling

const val part1by1Phrase = """#ifndef SP_PART1BY1
#define SP_PART1BY1
uint part1by1 (uint x) {
    x = (x & 0x0000ffffu);
    x = ((x ^ (x << 8u)) & 0x00ff00ffu);
    x = ((x ^ (x << 4u)) & 0x0f0f0f0fu);
    x = ((x ^ (x << 2u)) & 0x33333333u);
    x = ((x ^ (x << 1u)) & 0x55555555u);
    return x;
}
#endif
"""

const val compact1by1Phrase = """#ifndef SP_COMPACT1BY1
#define SP_COMPACT1BY1    
uint compact1by1 (uint x) {
    x = (x & 0x55555555u);
    x = ((x ^ (x >> 1u)) & 0x33333333u);
    x = ((x ^ (x >> 2u)) & 0x0f0f0f0fu);
    x = ((x ^ (x >> 4u)) & 0x00ff00ffu);
    x = ((x ^ (x >> 8u)) & 0x0000ffffu);
    return x;
}
#endif
"""

const val inverseGray32Phrase = """#ifndef SP_INVERSEGRAY32
#define SP_INVERSEGRAY32
uint inverse_gray32(uint n) {
    n = n ^ (n >> 1);
    n = n ^ (n >> 2);
    n = n ^ (n >> 4);
    n = n ^ (n >> 8);
    n = n ^ (n >> 16);
    return n;
}
#endif"""

// forward Hilbert https://www.shadertoy.com/view/llGcDm
const val hilbertPhrase = """#ifndef SP_HILBERT
#define SP_HILBERT
int hilbert(ivec2 p, int level) {
    int d = 0;
    for (int k = 0; k < level; k++) {
        int n = level - k -1;
        ivec2 r = (p >> n) & 1;
        d += ((3 * r.x) ^ r.y) << (2 * n);
    	if (r.y == 0) { if (r.x == 1) { p = (1 <<n) - 1 - p; } p = p.yx; }
    }
    return d;
}
#endif
"""

// https://www.shadertoy.com/view/llGcDm
const val inverseHilbertPhrase = """#ifndef SP_INVERSEHILBERT
#define SP_INVERSEHILBERT
ivec2 inverseHilbert( int i, int level ) {
    ivec2 p = ivec2(0, 0);
    for (int k = 0; k < level; k++) {
        ivec2 r = ivec2(i >> 1, i ^(i >> 1)) & 1;
        if (r.y==0) { if(r.x == 1) { p = (1 << k) - 1 - p; } p = p.yx; }
        p += r << k;
        i >>= 2;
    }
    return p;
}
#endif    
"""

const val hilbertV3Phrase = """#ifndef SP_HILBERTV3
#define SP_HILBERTV3

// Convert 3D coordinates to a Hilbert curve index in GLSL
int hilbert(ivec3 pos, int order) {
    // Input position vector (x, y, z)
    int x = pos.x;
    int y = pos.y;
    int z = pos.z;
    
    // Initialize the index to 0
    int hilbertIndex = 0;
    
    // Temporary variables for coordinate transformation
    int rx, ry, rz;
    int bits;
    
    // Process each bit from MSB to LSB
    for (int i = order - 1; i >= 0; i--) {
        // Extract bit i from each coordinate
        bits = ((x >> i) & 1) | (((y >> i) & 1) << 1) | (((z >> i) & 1) << 2);
        
        // Calculate position in subcube
        rx = 0;
        ry = 0;
        rz = 0;
        
        // Transform coordinates based on subcube position
        if (bits == 0) {
            rx = y;
            ry = x;
            rz = z;
        } 
        else if (bits == 1) {
            rx = x;
            ry = y;
            rz = z;
        } 
        else if (bits == 2) {
            rx = x;
            ry = y + (1 << i);
            rz = z;
        } 
        else if (bits == 3) {
            rx = (1 << i) - 1 - x;
            ry = (1 << i) - 1 - y;
            rz = z;
        } 
        else if (bits == 4) {
            rx = (1 << i) - 1 - x;
            ry = y;
            rz = z + (1 << i);
        } 
        else if (bits == 5) {
            rx = y;
            ry = x;
            rz = z + (1 << i);
        } 
        else if (bits == 6) {
            rx = x;
            ry = y;
            rz = z + (1 << i);
        } 
        else if (bits == 7) {
            rx = (1 << i) - 1 - y;
            ry = (1 << i) - 1 - x;
            rz = z + (1 << i);
        }
        
        // Add the current subcube's contribution to the index
        // Each subcube contains 8^i cells
        hilbertIndex |= (bits << (3 * i));
        
        // Update coordinates
        x = rx;
        y = ry;
        z = rz;
    }
    
    return hilbertIndex;
}
#endif
    
"""