import { PageHeader } from '../components/PageHeader'
import { Watchlist } from '../components/market/Watchlist'

export function Markets() {
  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Markets"
        title="Currency pairs"
        description="Browse the major forex pairs available in the monitoring workspace."
      />
      <Watchlist />
    </div>
  )
}
