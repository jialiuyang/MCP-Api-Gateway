# Cursor end-to-end demo (B2)

This walkthrough verifies the gateway end-to-end: start the server, import a
public Swagger document, connect Cursor, and ask the LLM to use it.

## 0. Prerequisites

- Java 17, Maven 3.9+, Node.js 20 (Maven auto-downloads if you skip the system install)
- Cursor 0.40+ (anything that supports `mcpServers` in `mcp.json`)
- Network access to a Swagger endpoint. We will use the Petstore reference:
  `https://petstore3.swagger.io/api/v3/openapi.json`

## 1. Build and start the gateway

```powershell
cd D:\workspace\mcp-gateway-enterprise
mvn clean package "-DskipTests"
java -jar mcpg-web\target\mcpg-web.jar
```

The console UI is at <http://localhost:8088>. Health check:

```powershell
Invoke-RestMethod http://localhost:8088/api/system/info
```

Expected: `{ "name": "mcpg", ..., "status": "UP" }`.

## 2. Import a Swagger URL

Open <http://localhost:8088/services>, click **Import Swagger**, and fill in:

| Field | Value |
| ----- | ----- |
| Name | `petstore` |
| Display name | `Swagger Petstore` |
| Swagger URL | `https://petstore3.swagger.io/api/v3/openapi.json` |
| Environment | `DEV` |

After a few seconds the row appears with 19 tools (varies). The **Tools** tab
now shows entries like `petstore__findPetsByStatus`, `petstore__placeOrder`,
etc.

Equivalently, from a shell:

```powershell
$body = @{ name='petstore'; url='https://petstore3.swagger.io/api/v3/openapi.json'; environment='DEV' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8088/api/services/import-swagger -Body $body -ContentType 'application/json'
```

## 3. Configure Cursor

Edit `%USERPROFILE%\.cursor\mcp.json` (Windows) or `~/.cursor/mcp.json` (macOS/Linux):

```json
{
  "mcpServers": {
    "mcp-gateway-enterprise": {
      "url": "http://localhost:8088/mcp"
    }
  }
}
```

> **Note**: the URL is `/mcp`, **not** `/mcp/sse`. Cursor 0.45+ speaks the
> 2025-03-26 Streamable HTTP transport, which uses a single endpoint for
> both directions. If you are stuck on an older Cursor build, use the legacy
> SSE config shown on the **Cursor Integration** page in the console.

Restart Cursor. Open the MCP / Tools panel; you should see four tools whose
names start with `list_services`, `search_api`, `get_api_schema`,
`call_api`. (Promoted tools would also appear here.)

## 4. Drive it from a chat

Try these prompts in order:

1. *"List every service registered with the MCP gateway."*  
   Expected: the LLM invokes `list_services` and replies with **petstore (DEV)**.

2. *"Search the gateway for any 'pet' related APIs."*  
   Expected: `search_api(keyword='pet')` returns a short list of operations.

3. *"Show me the schema for `petstore__findPetsByStatus`."*  
   Expected: `get_api_schema(tool_name='petstore__findPetsByStatus')` returns
   the JSON schema, including the `status` query parameter with its enum.

4. *"Use the gateway to fetch all pets that are currently available."*  
   Expected: `call_api(tool_name='petstore__findPetsByStatus',
   arguments={ queryParams: { status: 'available' } })` returns a list of
   Petstore pets.

## 5. Promote a hot path (optional)

Open <http://localhost:8088/tools>, find `petstore__findPetsByStatus`, and flip
the **Promoted** switch. The gateway broadcasts a `tools/list_changed`
notification over SSE; Cursor refreshes its tool list and the promoted
operation shows up as a first-class tool. Now the LLM can call it directly
without going through `call_api`.

## 6. Troubleshooting

| Symptom | Likely cause |
| ------- | ------------ |
| Backend log shows `Request method 'POST' is not supported` | Cursor is using Streamable HTTP but you configured the legacy `/mcp/sse` URL. Change the URL in `mcp.json` to `http://localhost:8088/mcp` (no `/sse`). |
| Cursor logs `failed to connect to MCP server` | Wrong URL. For Cursor 0.45+ use `http://localhost:8088/mcp`; for older builds add `"type": "sse"` and `http://localhost:8088/mcp/sse`. |
| Tools list is empty even after import | Cursor cached the previous (empty) list. Quit Cursor (not just close the window) and reopen. |
| Import returns "URL returned HTML rather than a spec" | The URL points at the Swagger UI page, not the JSON. The gateway tries `/v3/api-docs`, `/openapi.json`, `/swagger.json` automatically; if none work, paste the correct JSON/YAML endpoint. |
| `call_api` returns HTTP 0 / connection refused | The backend service the spec describes is not reachable from the gateway machine. Check the `baseUrl` shown on the Services page. |
