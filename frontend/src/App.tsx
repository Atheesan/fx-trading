import './App.css'

function App() {
  return (
    <main className="app-shell">
      <section className="welcome-panel" aria-labelledby="page-title">
        <p className="eyebrow">FXAI V1</p>
        <h1 id="page-title">Forex market monitoring</h1>
        <p className="summary">
          The project foundation is ready. Market data will be added in a future
          V1 feature.
        </p>
        <p className="status" role="status">
          Initial application scaffold
        </p>
      </section>
    </main>
  )
}

export default App
