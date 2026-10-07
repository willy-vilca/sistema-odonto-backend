import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { once } from "node:events";
import { createGateway } from "./whatsapp-webhook-gateway.mjs";

test("gateway exposes only signed webhook paths and strips session credentials", async () => {
    let captured;
    const backend = http.createServer(async (req, res) => {
        const chunks = [];
        for await (const chunk of req) chunks.push(chunk);
        captured = {
            url: req.url,
            headers: req.headers,
            body: Buffer.concat(chunks).toString(),
        };
        res.writeHead(200);
        res.end();
    });
    backend.listen(0, "127.0.0.1");
    await once(backend, "listening");
    const gateway = createGateway(backend.address().port);
    gateway.listen(0, "127.0.0.1");
    await once(gateway, "listening");
    const base = `http://127.0.0.1:${gateway.address().port}`;
    try {
        assert.equal(
            (await fetch(base + "/api/v1/auth/login", { method: "POST" }))
                .status,
            404,
        );
        assert.equal(
            (await fetch(base + "/api/v1/integrations/whatsapp/inbound"))
                .status,
            404,
        );
        assert.equal(
            (
                await fetch(base + "/api/v1/integrations/whatsapp/inbound", {
                    method: "POST",
                    body: "{}",
                    headers: { "content-type": "application/json" },
                })
            ).status,
            415,
        );
        const path = "/api/v1/integrations/whatsapp/status?messageId=123";
        const response = await fetch(base + path, {
            method: "POST",
            body: "Body=hola%20mundo",
            headers: {
                "content-type": "application/x-www-form-urlencoded",
                "x-twilio-signature": "signature",
                cookie: "JSESSIONID=private",
                authorization: "Bearer private",
                "x-forwarded-host": "evil.example",
            },
        });
        assert.equal(response.status, 200);
        assert.equal(captured.url, path);
        assert.equal(captured.body, "Body=hola%20mundo");
        assert.equal(captured.headers["x-twilio-signature"], "signature");
        for (const key of ["cookie", "authorization", "x-forwarded-host"])
            assert.equal(captured.headers[key], undefined);
        assert.equal(
            (
                await fetch(base + "/api/v1/integrations/whatsapp/inbound", {
                    method: "POST",
                    body: "x".repeat(32769),
                    headers: {
                        "content-type": "application/x-www-form-urlencoded",
                    },
                })
            ).status,
            413,
        );
    } finally {
        gateway.closeAllConnections();
        backend.closeAllConnections();
        await Promise.all([
            new Promise((r) => gateway.close(r)),
            new Promise((r) => backend.close(r)),
        ]);
    }
});

test("Kapso JSON is forwarded byte-for-byte with only webhook headers", async () => {
    let captured;
    const backend = http.createServer(async (req, res) => {
        const chunks = [];
        for await (const chunk of req) chunks.push(chunk);
        captured = { headers: req.headers, body: Buffer.concat(chunks) };
        res.writeHead(200);
        res.end();
    });
    backend.listen(0, "127.0.0.1");
    await once(backend, "listening");
    const gateway = createGateway(backend.address().port);
    gateway.listen(0, "127.0.0.1");
    await once(gateway, "listening");
    const endpoint = `http://127.0.0.1:${gateway.address().port}/api/v1/integrations/kapso/events`;
    try {
        const raw = '{ "message": "mañana ñ 😀", "spaces": "  " }';
        const response = await fetch(endpoint, {
            method: "POST", body: raw,
            headers: {
                "content-type": "application/json; charset=utf-8",
                "x-webhook-signature": "abc123",
                "x-webhook-event": "whatsapp.message.received",
                "x-idempotency-key": "delivery-test",
                "x-webhook-payload-version": "v2",
                authorization: "Bearer private",
                cookie: "JSESSIONID=private",
                "x-twilio-signature": "not-kapso",
            },
        });
        assert.equal(response.status, 200);
        assert.deepEqual(captured.body, Buffer.from(raw));
        assert.equal(captured.headers["x-webhook-signature"], "abc123");
        assert.equal(captured.headers["x-webhook-event"], "whatsapp.message.received");
        assert.equal(captured.headers["x-idempotency-key"], "delivery-test");
        for (const key of ["authorization", "cookie", "x-twilio-signature"]) assert.equal(captured.headers[key], undefined);
        assert.equal((await fetch(endpoint)).status, 404);
        assert.equal((await fetch(endpoint, {method:"POST",body:"x",headers:{"content-type":"text/plain"}})).status, 415);
        assert.equal((await fetch(endpoint, {method:"POST",body:"x".repeat(65537),headers:{"content-type":"application/json"}})).status, 413);
    } finally {
        gateway.closeAllConnections(); backend.closeAllConnections();
        await Promise.all([new Promise(r => gateway.close(r)),new Promise(r => backend.close(r))]);
    }
});
