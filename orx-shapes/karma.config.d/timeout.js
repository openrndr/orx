// Raise the per-test Mocha timeout for js and wasmJs browser tests to 30 seconds
config.set({
    client: {
        mocha: {
            timeout: 30000
        }
    }
});
