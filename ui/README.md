
Graphite UI
===========

This folder contains a lightweight Next.js (TypeScript) + Tailwind CSS + Apollo Client scaffold for the Graphite-Forge project.

Quick start (from repository root):

```bash
cd ui
npm install
npm run dev
```

Notes:
- The UI expects a GraphQL endpoint available at `http://localhost:8080/graphql` (Edge Gateway) by default. You can override this with the environment variable `NEXT_PUBLIC_GRAPHQL_ENDPOINT` to point directly at the GraphQL service (e.g. `http://localhost:8083/graphql`).
- The scaffold uses the Next App Router (`app/`) and TailwindCSS configuration. There are placeholder locations for `components/` and `shadcn/ui` integration.
- To integrate `shadcn/ui` components, run their CLI and follow the published install steps after installing dependencies.

Suggested next steps I can do for you:
- Run `npm install` inside `/ui` and start the dev server here (requires your consent to run `npm install` in this workspace).
- Integrate `shadcn/ui` and Radix UI components and add a component library and example pages.
- Add a GitHub Actions job to build and test the Next app during CI.

Environment examples (Linux/macOS):

```bash
# Start UI against the gateway
NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://localhost:8080/graphql npm run dev

# Or point directly at the GraphQL service
NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://localhost:8083/graphql npm run dev
```

If you want, I can also add a short paragraph in the root `README.md` explaining how the gateway maps to backend services and provide a recommended `.env.local` example.
