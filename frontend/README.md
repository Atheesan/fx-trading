# FXAI Frontend

Responsive React and TypeScript market-monitoring dashboard built with Vite,
Tailwind CSS, TanStack Query, and React Router.

## Commands

```shell
npm install
npm run dev
npm run build
npm test
npm run lint
```

## Configuration

The frontend calls `http://localhost:8080` by default. Set
`VITE_API_BASE_URL` to point at a different backend origin:

```shell
VITE_API_BASE_URL=https://api.example.com npm run build
```

Only the backend base URL belongs in this variable; API paths are defined in the
dedicated client layer under `src/api`.
