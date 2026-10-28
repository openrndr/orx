package org.openrndr.extra.testing

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.internal.gl3.ApplicationBaseGLFWGL3
import org.openrndr.internal.gl3.ApplicationGLFWGL3

/**
 * Provides a [program] with a window and GL context. The application is created once per test class,
 * opening more than one window per JVM is unreliable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractApplicationTestFixture {
    lateinit var applicationBase: ApplicationBaseGLFWGL3
    lateinit var application: ApplicationGLFWGL3
    lateinit var program: Program

    @BeforeAll
    open fun setup() {
        program = ProgramImplementation()
        applicationBase = ApplicationBaseGLFWGL3()
        application = applicationBase.build(program, Configuration()) as ApplicationGLFWGL3
        runBlocking { application.setup() }
        application.preloop()
    }

    @AfterAll
    open fun teardown() {
        application.postLoop()
    }
}
