# Cursor demo script

A 5-minute walkthrough you can run after a fresh `mvn clean package`. It
exercises everything B1 → B5 ships: service import, exposure modes, the
meta tools, the audit / health / policy / settings consoles, and Cursor
itself.

> All screenshots referenced below should be captured against your local
> install and committed to `docs/screenshots/` so the demo travels with the
> repo.

---

## 0. Prerequisites

```bash
mvn -DskipTests clean package
java -jar mcpg-web/target/mcpg-web.jar
```

Open <http://localhost:8088/>.

Out of the box (dev profile) the `DemoSeedRunner` has already populated:

- **order-service** (NACOS source) — 6 operations, `getOrder` and `listOrders` pre-promoted
- **user-service** (NACOS source) — 5 operations, `getUser` and `searchUsers` pre-promoted
- **petstore** (MANUAL source) — 5 operations, `findPetsByStatus` pre-promoted

So step 1 below is just to confirm the seed worked; the **real** demo starts
at step 2.

> `screenshot: docs/screenshots/01-dashboard.png` — first paint, dashboard
> shows non-zero counters and the B1-B5 roadmap with every checkpoint green.

---

## 1. Verify seeded data (≈30s)

1. **Dashboard** → numbers should read `3` services, ~16 tools, `0` calls.
2. **Services** → three rows with `ACTIVE` status. Hover the petstore row
   → display name reads "Petstore (public)". The other two are demo stubs
   so calling them will fail (which is realistic — production gateways
   often track services that are currently down).

> `screenshot: docs/screenshots/02-services.png`

---

## 2. Wire Cursor up (≈30s)

1. **MCP Governance → Client Integration**.
2. Stay on the `Streamable HTTP (recommended)` tab.
3. Click "Copy config". Paste into `~/.cursor/mcp.json` (or your workspace's
   `.cursor/mcp.json`):

   ```json
   {
     "mcpServers": {
       "mcpg-local": {
         "url": "http://localhost:8088/mcp"
       }
     }
   }
   ```

4. Restart Cursor. Open Settings → MCP. You should see `mcpg-local` with at
   least 4 + 4 = **8 tools** (4 meta + 4 promoted).

> `screenshot: docs/screenshots/03-cursor-tools.png`

---

## 3. Drive Cursor through the meta tools (≈60s)

Open the Cursor chat panel and type:

> List every service the gateway is aware of, then search for any operation
> related to "order" and show me the JSON schema of the first hit.

Expected behaviour (Cursor will call MCP tools in order):

1. `list_services` → returns the three seeded services
2. `search_api` with keyword `order` → returns `getOrder`, `listOrders`,
   `createOrder`, `cancelOrder`, `refundOrder`, `getOrderEvents`
3. `get_api_schema` for the first match → returns the stub JSON Schema

> `screenshot: docs/screenshots/04-cursor-search.png`

Then ask:

> Now try to actually invoke `getOrder` with id=`42`.

Cursor will call `call_api`. The order-service stub baseUrl is not
reachable in the demo, so the call will fail with a transport error. That is
intentional — it lets you demo the **failure path** without standing up a
real backend.

---

## 4. Show exposure mode switching (≈60s)

1. Console → **MCP Governance → Tools**.
2. Top card shows `Current exposure strategy: HYBRID`, with
   `Total tools = 16`, `Promoted = 5`, `Effective = 9`.
3. Click **Change mode** → pick `META` → save.
4. Switch back to Cursor → the tool list **shrinks to 4** (only the meta
   tools) without a restart, because the gateway broadcasts
   `notifications/tools/list_changed`.
5. Switch back to `HYBRID` to restore the original list.

> `screenshot: docs/screenshots/05-exposure-modes.png`

---

## 5. Tour the B5 consoles (≈90s)

### 5a. Audit (`/audit`)

The console shows ~64 synthetic events covering imports, refreshes, exposure
changes and MCP calls. Demonstrate:

- The **outcome filter** — flip to `Failure` to see only failed events.
- Click any row → drawer opens with full event detail including client IP
  and User-Agent.

> `screenshot: docs/screenshots/06-audit.png`

### 5b. Policies (`/policies`)

Six pre-seeded policy cards across four categories
(`governance`, `traffic`, `compliance`, `auth`). Demonstrate:

- Toggle the `PII redaction` card on → severity already `HIGH`, audit log
  picks it up.
- Click `Edit config` on `Global rate limit` → JSON editor opens,
  validation guards against malformed JSON.

> `screenshot: docs/screenshots/07-policies.png`

### 5c. Tool Health (`/health`)

The page renders deterministic synthetic metrics seeded from each tool's
persistent id (so the numbers stay the same across refreshes — great for
screenshots and demos).

- KPI strip shows total tools / active tools / 24h call volume / success
  rate / average latency.
- Two charts: 24-hour call volume (success vs. failure) and P50 latency
  histogram.
- A table of the top 15 tools by call volume.

> `screenshot: docs/screenshots/08-health.png`

### 5d. Settings (`/settings`)

Site-wide preferences. Demonstrate:

- Change `Site name` to your company name → save → header updates.
- Toggle `Keep demo data` off → next restart the seed runner stays quiet.

> `screenshot: docs/screenshots/09-settings.png`

---

## 6. Wrap-up talking points (≈30s)

- One Jar, one URL — every MCP-compliant client (Cursor, Claude Code,
  Claude Desktop, Codex, Windsurf, Cline, Continue) plugs in with no code
  changes.
- `META` keeps the LLM context budget small at scale (hundreds of
  microservices).
- `Promote` lets operators surface high-traffic operations as first-class
  tools without losing the safety net.
- Every governance card (audit, policies, health, settings) is wired
  end-to-end with persistence so future enforcement hooks have a stable
  home.

---

## Tips

- **Reset to a clean state**: delete `data/mcpg-dev.mv.db` and restart.
  The seed runner will repopulate everything.
- **Skip the seed**: launch with `MCPG_PROFILE=prod` or set
  `mcpg.demo.seed=false` in `application-dev.yml`.
- **Capture screenshots in light mode**: the console respects the Element
  Plus default theme; force light mode via your OS settings for consistent
  output across docs.
