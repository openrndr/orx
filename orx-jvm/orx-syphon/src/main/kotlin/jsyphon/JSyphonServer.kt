package org.openrndr.extra.syphon.jsyphon
/*
JSyphonServer.java - 
Copyright 2011 -Skye Book (sbook) & Anton Marini (vade)

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
class JSyphonServer  // Public API
{
    private var ptr: Long = 0

    val native = JSyphonNative.check()

    fun initWithName(name: String) {
        ptr = initWithName(name, null)
    }

    val name: String
        get() = getName(ptr)

    fun hasClients(): Boolean = hasClients(ptr)

    fun publishFrameTexture(
        texID: Int,
        texTarget: Int,
        posX: Int,
        posY: Int,
        width: Int,
        height: Int,
        sizeX: Int,
        sizeY: Int,
        isFlipped: Boolean
    ) {
        publishFrameTexture(ptr, texID, texTarget, posX, posY, width, height, sizeX, sizeY, isFlipped)
    }

    fun publishFrameTexture(
        texID: Int,
        texTarget: Int,
        rect: NSRect,
        size: NSSize,
        isFlipped: Boolean
    ) {
        publishFrameTexture(
            ptr,
            texID,
            texTarget,
            rect.origin.x,
            rect.origin.y,
            rect.size.x,
            rect.size.y,
            size.x,
            size.y,
            isFlipped
        )
    }

    fun bindToDrawFrameOfSize(size: NSSize): Boolean =
        bindToDrawFrameOfSize(ptr, size.x, size.y)

    fun bindToDrawFrameOfSize(sizeX: Int, sizeY: Int): Boolean =
        bindToDrawFrameOfSize(ptr, sizeX, sizeY)

    fun unbindAndPublish() {
        unbindAndPublish(ptr)
    }

    fun stop() {
        stop(ptr)
    }

    // Native method declarations
    private external fun initWithName(
        name: String,
        options: HashMap<String, Any>?
    ): Long

    private external fun getName(ptr: Long): String
    private external fun hasClients(ptr: Long): Boolean
    private external fun publishFrameTexture(
        ptr: Long,
        texID: Int,
        texTarget: Int,
        posX: Int,
        posY: Int,
        width: Int,
        height: Int,
        sizeX: Int,
        sizeY: Int,
        isFlipped: Boolean
    )

    private fun publishFrameTexture(
        ptr: Long,
        texID: Int,
        texTarget: Int,
        rect: NSRect,
        size: NSSize,
        isFlipped: Boolean
    ) {
        publishFrameTexture(
            ptr,
            texID,
            texTarget,
            rect.origin.x,
            rect.origin.y,
            rect.size.x,
            rect.size.y,
            size.x,
            size.y,
            isFlipped
        )
    }

    private fun bindToDrawFrameOfSize(ptr: Long, size: NSSize): Boolean {
        return bindToDrawFrameOfSize(ptr, size.x, size.y)
    }

    private external fun bindToDrawFrameOfSize(ptr: Long, sizeX: Int, sizeY: Int): Boolean
    private external fun unbindAndPublish(ptr: Long)
    private external fun stop(ptr: Long)
}