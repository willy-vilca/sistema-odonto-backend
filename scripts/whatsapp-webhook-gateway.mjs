// Development gateway: expose only signed webhook endpoints, never the application.
import http from "node:http";
import { pathToFileURL } from "node:url";
const paths = new Set([
    "/api/v1/integrations/whatsapp/inbound",
    "/api/v1/integrations/whatsapp/status",
    "/api/v1/integrations/kapso/events",
]);
export function createGateway(backendPort = 8080) {
    return http.createServer(async (req, res) => {
        let url;
        try {
            url = new URL(req.url, "http://127.0.0.1");
        } catch {
            res.writeHead(400);
            res.end();
            return;
        }
        if (req.method !== "POST" || !paths.has(url.pathname)) {
            res.writeHead(404);
            res.end();
            return;
        }
        const kapso = url.pathname === "/api/v1/integrations/kapso/events";
        const contentType = kapso ? "application/json" : "application/x-www-form-urlencoded";
        if (
            !req.headers["content-type"]
                ?.toLowerCase()
                .startsWith(contentType)
        ) {
            res.writeHead(415);
            res.end();
            return;
        }
        let length = 0;
        const chunks = [];
        try {
            for await (const chunk of req) {
                length += chunk.length;
                if (length > (kapso ? 65536 : 32768)) {
                    res.writeHead(413);
                    res.end();
                    return;
                }
                chunks.push(chunk);
            }
            const headers = {
                "content-type": contentType,
                "content-length": String(length),
            };
            if (!kapso && req.headers["x-twilio-signature"])
                headers["x-twilio-signature"] =
                    req.headers["x-twilio-signature"];
            if (kapso) {
                for (const header of ["x-webhook-signature", "x-webhook-event", "x-idempotency-key", "x-webhook-payload-version", "x-webhook-batch"]) {
                    if (req.headers[header]) headers[header] = req.headers[header];
                }
            }
            const response = await fetch(
                `http://127.0.0.1:${backendPort}${url.pathname}${url.search}`,
                {
                    method: "POST",
                    headers,
                    body: Buffer.concat(chunks),
                    redirect: "error",
                    signal: AbortSignal.timeout(10000),
                },
            );
            res.writeHead(response.status, {
                "content-type":
                    response.headers.get("content-type") ?? "text/plain",
            });
            res.end(Buffer.from(await response.arrayBuffer()));
        } catch {
            if (!res.headersSent) res.writeHead(503);
            res.end();
        }
    });
}
if (
    process.argv[1] &&
    import.meta.url === pathToFileURL(process.argv[1]).href
) {
    const server = createGateway();
    server.requestTimeout = 15000;
    server.headersTimeout = 10000;
    server.listen(8082, "127.0.0.1", () =>
        process.stdout.write(
            "Receptor de WhatsApp listo en 127.0.0.1:8082. Solo admite webhooks de Twilio y Kapso.\n",
        ),
    );
}
