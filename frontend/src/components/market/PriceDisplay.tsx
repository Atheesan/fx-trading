interface PriceDisplayProps {
  label: string
  value: number
  symbol: string
}

function formatPrice(value: number, symbol: string) {
  const fractionDigits = symbol.endsWith('JPY') ? 3 : 5
  return value.toFixed(fractionDigits)
}

export function PriceDisplay({ label, value, symbol }: PriceDisplayProps) {
  return (
    <div>
      <span className="mb-1 block text-[0.65rem] font-semibold uppercase tracking-wider text-slate-400 md:hidden">
        {label}
      </span>
      <span className="font-mono text-sm font-semibold tabular-nums text-slate-800 dark:text-slate-100">
        {formatPrice(value, symbol)}
      </span>
    </div>
  )
}
