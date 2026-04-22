package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgeFlip(e: HalfEdge) {
    val eIdx = halfEdges.indexOf(e)
    if (eIdx == -1) return
    val oeIdx = e.otherEdge
    if (oeIdx == -1) return
    val oe = halfEdges[oeIdx]

    // Faces
    val f0Idx = e.face
    val f1Idx = oe.face
    if (f0Idx == -1 || f1Idx == -1) return

    // Get edges of face 0
    val e0 = e
    val e1 = halfEdges[e0.nextEdge]
    val e2 = halfEdges[e1.nextEdge]
    if (e2.nextEdge != eIdx) return // Not a triangle

    // Get edges of face 1
    val oe0 = oe
    val oe1 = halfEdges[oe0.nextEdge]
    val oe2 = halfEdges[oe1.nextEdge]
    if (oe2.nextEdge != oeIdx) return // Not a triangle

    val v0Idx = e0.vertex
    val v1Idx = oe0.vertex
    val v2Idx = e2.vertex
    val v3Idx = oe2.vertex

    val e1Idx = e0.nextEdge
    val e2Idx = e1.nextEdge
    val oe1Idx = oe0.nextEdge
    val oe2Idx = oe1.nextEdge

    // Update vertices of the flipped edge
    e0.vertex = v3Idx
    oe0.vertex = v2Idx

    // Face 0 will be (v3, v2, v0) - NO, wait.
    // Original: 
    // F0: e0(v0->v1), e1(v1->v2), e2(v2->v0)
    // F1: oe0(v1->v0), oe1(v0->v3), oe2(v3->v1)
    // New:
    // F0: e0(v3->v2), e2(v2->v0), oe1(v0->v3)
    // F1: oe0(v2->v3), oe2(v3->v1), e1(v1->v2)

    // Update Face references
    e0.face = f0Idx
    e2.face = f0Idx
    oe1.face = f0Idx

    oe0.face = f1Idx
    oe2.face = f1Idx
    e1.face = f1Idx

    // Update next/prev pointers
    e0.nextEdge = e2Idx
    e0.prevEdge = oe1Idx
    e2.nextEdge = oe1Idx
    e2.prevEdge = eIdx
    oe1.nextEdge = eIdx
    oe1.prevEdge = e2Idx

    oe0.nextEdge = oe2Idx
    oe0.prevEdge = e1Idx
    oe2.nextEdge = e1Idx
    oe2.prevEdge = oeIdx
    e1.nextEdge = oeIdx
    e1.prevEdge = oe2Idx

    // Update Face.edge pointers to ensure they point to a valid edge of the face
    faces[f0Idx].edge = eIdx
    faces[f1Idx].edge = oeIdx

    // Update Vertex.edge pointers
    // If v0.edge was e0, it should now be oe1 (which still starts at v0)
    if (vertices[v0Idx].edge == eIdx) {
        vertices[v0Idx].edge = oe1Idx
    }
    // If v1.edge was oe0, it should now be e1 (which still starts at v1)
    if (vertices[v1Idx].edge == oeIdx) {
        vertices[v1Idx].edge = e1Idx
    }
    // v2.edge and v3.edge should be fine, but let's make sure they don't point to something moved
    // Actually, e0 now starts at v3, oe0 starts at v2.
    // So if we want, we can set them.
    vertices[v2Idx].edge = oeIdx
    vertices[v3Idx].edge = eIdx
}