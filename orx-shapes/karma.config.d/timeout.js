// Raise the per-test Mocha timeout for js and wasmJs browser tests to 30 seconds
config.set({
    client: {
        mocha: {
            timeout: 30000
        }
    },
    // Long-running synchronous tests (e.g. eigenmode solves) block the browser's event loop,
    // which starves Karma's socket heartbeat and causes a "ping timeout" disconnect.
    pingTimeout: 120000,
    browserDisconnectTimeout: 120000,
    browserDisconnectTolerance: 1,
    browserNoActivityTimeout: 300000,
    captureTimeout: 120000
});
